package com.anagram.analyzer.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anagram.analyzer.data.datastore.QuizScoreStore
import com.anagram.analyzer.domain.model.HiraganaNormalizer
import com.anagram.analyzer.domain.model.NormalizationException
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

    fun onInputAnswerChanged(value: String) {
        _uiState.update { it.copy(inputAnswer = value, errorMessage = null) }
    }

    fun onSubmitAnswer() {
        val state = _uiState.value
        val question = state.question ?: return
        if (state.phase != QuizPhase.ANSWERING) return

        val inputRaw = state.inputAnswer.trim()
        if (inputRaw.isEmpty()) return

        val normalizedInput = try {
            HiraganaNormalizer.normalizeHiragana(inputRaw)
        } catch (_: NormalizationException) {
            _uiState.update { it.copy(errorMessage = "ひらがなで入力してください") }
            return
        }

        val isCorrect = question.correctWords.any { it == normalizedInput }

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
        _uiState.update { it.copy(phase = QuizPhase.LOADING, inputAnswer = "", errorMessage = null) }
        viewModelScope.launch {
            try {
                val question = withContext(ioDispatcher) {
                    generateQuizUseCase.execute(difficulty.minLen, difficulty.maxLen)
                }
                if (question != null) {
                    _uiState.update { it.copy(phase = QuizPhase.ANSWERING, question = question) }
                } else {
                    _uiState.update {
                        it.copy(
                            phase = QuizPhase.IDLE,
                            errorMessage = "出題できる単語が見つかりませんでした。難易度を変えてみてください。",
                        )
                    }
                }
            } catch (error: Throwable) {
                if (error is CancellationException) throw error
                _uiState.update {
                    it.copy(
                        phase = QuizPhase.IDLE,
                        errorMessage = "問題の取得に失敗しました: ${error.message ?: "原因不明"}",
                    )
                }
            }
        }
    }

    private companion object {
        private const val POINTS_PER_CORRECT = 10
    }
}
