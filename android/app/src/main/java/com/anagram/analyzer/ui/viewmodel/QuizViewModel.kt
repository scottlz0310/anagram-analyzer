package com.anagram.analyzer.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anagram.analyzer.data.datastore.QuizScoreStore
import com.anagram.analyzer.domain.model.CharCard
import com.anagram.analyzer.domain.model.QuizDifficulty
import com.anagram.analyzer.domain.usecase.GenerateQuizUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class QuizViewModel @Inject constructor(
    private val generateQuizUseCase: GenerateQuizUseCase,
    private val quizScoreStore: QuizScoreStore,
    private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {
    private val _uiState = MutableStateFlow(QuizUiState())
    val uiState: StateFlow<QuizUiState> = _uiState

    init {
        viewModelScope.launch(ioDispatcher) {
            quizScoreStore.score.collect { score ->
                _uiState.update { it.copy(score = score) }
            }
        }
        viewModelScope.launch(ioDispatcher) {
            quizScoreStore.streak.collect { streak ->
                _uiState.update { it.copy(streak = streak) }
            }
        }
        viewModelScope.launch(ioDispatcher) {
            quizScoreStore.bestStreak.collect { best ->
                _uiState.update { it.copy(bestStreak = best) }
            }
        }
    }

    fun onDifficultySelected(difficulty: QuizDifficulty) {
        _uiState.update { it.copy(difficulty = difficulty) }
    }

    fun onStartQuiz() {
        loadNextQuestion()
    }

    fun onCardTapped(cardId: Int) {
        updateAnsweringState { state ->
            val card = state.shuffledCards.firstOrNull { it.id == cardId } ?: return@updateAnsweringState state

            when {
                card.isPlaced -> {
                    state.copy(
                        shuffledCards = updateCardPlacement(state.shuffledCards, cardId, isPlaced = false),
                        answerSlots = clearCardFromSlots(state.answerSlots, cardId),
                        selectedCardId = cardId,
                        errorMessage = null,
                    )
                }
                state.selectedCardId == cardId -> {
                    state.copy(selectedCardId = null, errorMessage = null)
                }
                state.selectedCardId != null -> {
                    state.copy(selectedCardId = cardId, errorMessage = null)
                }
                else -> {
                    val firstEmptySlot = state.answerSlots.indexOfFirst { it == null }
                    if (firstEmptySlot == -1) {
                        state.copy(selectedCardId = cardId, errorMessage = null)
                    } else {
                        placeCardInSlot(state, cardId, firstEmptySlot, selectedCardId = null)
                    }
                }
            }
        }
    }

    fun onSlotTapped(slotIndex: Int) {
        updateAnsweringState { state ->
            if (slotIndex !in state.answerSlots.indices) return@updateAnsweringState state

            val cardIdInSlot = state.answerSlots[slotIndex]
            val selectedCardId = state.selectedCardId

            when {
                selectedCardId != null -> {
                    val nextSlots = clearCardFromSlots(state.answerSlots, selectedCardId).toMutableList()
                    val nextCards = updateCardPlacement(
                        state.shuffledCards,
                        selectedCardId,
                        isPlaced = true,
                    )

                    if (cardIdInSlot != null) {
                        nextSlots[slotIndex] = selectedCardId
                        state.copy(
                            shuffledCards = updateCardPlacement(nextCards, cardIdInSlot, isPlaced = false),
                            answerSlots = nextSlots,
                            selectedCardId = cardIdInSlot,
                            errorMessage = null,
                        )
                    } else {
                        nextSlots[slotIndex] = selectedCardId
                        state.copy(
                            shuffledCards = nextCards,
                            answerSlots = nextSlots,
                            selectedCardId = null,
                            errorMessage = null,
                        )
                    }
                }
                cardIdInSlot != null -> {
                    val nextSlots = state.answerSlots.toMutableList()
                    nextSlots[slotIndex] = null
                    state.copy(
                        shuffledCards = updateCardPlacement(state.shuffledCards, cardIdInSlot, isPlaced = false),
                        answerSlots = nextSlots,
                        selectedCardId = cardIdInSlot,
                        errorMessage = null,
                    )
                }
                else -> state
            }
        }
    }

    fun onSubmitAnswer() {
        val state = _uiState.value
        val question = state.question ?: return
        if (state.phase != QuizPhase.ANSWERING) return

        val answer = buildAnswer(state) ?: run {
            _uiState.update { it.copy(errorMessage = "すべての文字を配置してください") }
            return
        }

        val isCorrect = question.correctWords.any { it == answer }

        viewModelScope.launch(ioDispatcher) {
            if (isCorrect) {
                quizScoreStore.addScore(POINTS_PER_CORRECT)
                quizScoreStore.incrementStreak()
            } else {
                quizScoreStore.resetStreak()
            }
        }

        _uiState.update {
            it.copy(
                phase = if (isCorrect) QuizPhase.CORRECT else QuizPhase.INCORRECT,
            )
        }
    }

    fun onNextQuestion() {
        loadNextQuestion()
    }

    fun onReset() {
        viewModelScope.launch(ioDispatcher) {
            quizScoreStore.resetAll()
        }
        _uiState.update {
            QuizUiState(difficulty = it.difficulty)
        }
    }

    private fun loadNextQuestion() {
        if (_uiState.value.phase == QuizPhase.LOADING) return
        val difficulty = _uiState.value.difficulty
        _uiState.update {
            it.copy(
                phase = QuizPhase.LOADING,
                question = null,
                shuffledCards = emptyList(),
                answerSlots = emptyList(),
                selectedCardId = null,
                errorMessage = null,
            )
        }
        viewModelScope.launch {
            try {
                val question = withContext(ioDispatcher) {
                    generateQuizUseCase.execute(difficulty.minLen, difficulty.maxLen)
                }
                if (question != null) {
                    _uiState.update {
                        it.copy(
                            phase = QuizPhase.ANSWERING,
                            question = question,
                            shuffledCards = question.shuffledCards,
                            answerSlots = List(question.shuffledCards.size) { null },
                            selectedCardId = null,
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            phase = QuizPhase.IDLE,
                            question = null,
                            shuffledCards = emptyList(),
                            answerSlots = emptyList(),
                            selectedCardId = null,
                            errorMessage = "出題できる単語が見つかりませんでした。難易度を変えてみてください。",
                        )
                    }
                }
            } catch (error: Throwable) {
                if (error is CancellationException) throw error
                _uiState.update {
                    it.copy(
                        phase = QuizPhase.IDLE,
                        question = null,
                        shuffledCards = emptyList(),
                        answerSlots = emptyList(),
                        selectedCardId = null,
                        errorMessage = "問題の取得に失敗しました: ${error.message ?: "原因不明"}",
                    )
                }
            }
        }
    }

    private fun updateAnsweringState(transform: (QuizUiState) -> QuizUiState) {
        _uiState.update { state ->
            if (state.phase != QuizPhase.ANSWERING) {
                state
            } else {
                transform(state)
            }
        }
    }

    private fun placeCardInSlot(
        state: QuizUiState,
        cardId: Int,
        slotIndex: Int,
        selectedCardId: Int?,
    ): QuizUiState {
        val nextSlots = clearCardFromSlots(state.answerSlots, cardId).toMutableList()
        nextSlots[slotIndex] = cardId
        return state.copy(
            shuffledCards = updateCardPlacement(state.shuffledCards, cardId, isPlaced = true),
            answerSlots = nextSlots,
            selectedCardId = selectedCardId,
            errorMessage = null,
        )
    }

    private fun buildAnswer(state: QuizUiState): String? {
        if (state.answerSlots.any { it == null }) return null

        val cardsById = state.shuffledCards.associateBy(CharCard::id)
        val answer = StringBuilder()
        state.answerSlots.forEach { cardId ->
            val resolvedId = cardId ?: return null
            val card = cardsById[resolvedId] ?: return null
            answer.append(card.char)
        }
        return answer.toString()
    }

    private fun updateCardPlacement(
        cards: List<CharCard>,
        cardId: Int,
        isPlaced: Boolean,
    ): List<CharCard> = cards.map { card ->
        if (card.id == cardId) {
            card.copy(isPlaced = isPlaced)
        } else {
            card
        }
    }

    private fun clearCardFromSlots(answerSlots: List<Int?>, cardId: Int): List<Int?> =
        answerSlots.map { slotCardId ->
            if (slotCardId == cardId) {
                null
            } else {
                slotCardId
            }
        }

    private companion object {
        private const val POINTS_PER_CORRECT = 10
    }
}
