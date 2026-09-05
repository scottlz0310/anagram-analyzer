package io.github.scottlz0310.anagramanalyzer.ui.viewmodel

import io.github.scottlz0310.anagramanalyzer.data.datastore.QuizScoreStore
import io.github.scottlz0310.anagramanalyzer.data.db.AnagramDao
import io.github.scottlz0310.anagramanalyzer.data.db.AnagramEntry
import io.github.scottlz0310.anagramanalyzer.domain.model.CharCard
import io.github.scottlz0310.anagramanalyzer.domain.model.QuizDifficulty
import io.github.scottlz0310.anagramanalyzer.domain.usecase.GenerateQuizUseCase
import io.github.scottlz0310.anagramanalyzer.domain.usecase.SearchAnagramUseCase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class QuizViewModelTest {
    @Test
    fun スタート時にANSWERING状態になりカードとスロットが初期化される() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val viewModel = buildViewModel(
                dao = FakeQuizAnagramDao(
                    randomEntry = AnagramEntry(sortedKey = "ごりん", word = "りんご", length = 3),
                    words = listOf("りんご"),
                ),
            )

            viewModel.onStartQuiz()
            advanceUntilIdle()

            assertEquals(QuizPhase.ANSWERING, viewModel.uiState.value.phase)
            assertTrue(viewModel.uiState.value.shuffledCards.isNotEmpty())
            assertEquals(
                viewModel.uiState.value.shuffledCards.size,
                viewModel.uiState.value.answerSlots.size,
            )
            assertTrue(viewModel.uiState.value.answerSlots.all { it == null })
            assertNull(viewModel.uiState.value.selectedCardId)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun カードタップで先頭の空きスロットに配置される() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val viewModel = buildViewModel(
                dao = FakeQuizAnagramDao(
                    randomEntry = AnagramEntry(sortedKey = "ごりん", word = "りんご", length = 3),
                    words = listOf("りんご"),
                ),
            )

            viewModel.onStartQuiz()
            advanceUntilIdle()

            val cardId = viewModel.uiState.value.shuffledCards.first().id
            viewModel.onCardTapped(cardId)

            val state = viewModel.uiState.value
            assertEquals(cardId, state.answerSlots.first())
            assertTrue(state.shuffledCards.first { it.id == cardId }.isPlaced)
            assertNull(state.selectedCardId)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun スロットタップでカードを取り外して任意の位置に移動できる() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val viewModel = buildViewModel(
                dao = FakeQuizAnagramDao(
                    randomEntry = AnagramEntry(sortedKey = "ごりん", word = "りんご", length = 3),
                    words = listOf("りんご"),
                ),
            )

            viewModel.onStartQuiz()
            advanceUntilIdle()

            val firstCardId = viewModel.uiState.value.shuffledCards.first().id
            viewModel.onCardTapped(firstCardId)
            viewModel.onSlotTapped(0)

            var state = viewModel.uiState.value
            assertEquals(firstCardId, state.selectedCardId)
            assertEquals(null, state.answerSlots[0])
            assertTrue(state.shuffledCards.first { it.id == firstCardId }.isPlaced.not())

            viewModel.onSlotTapped(2)

            state = viewModel.uiState.value
            assertEquals(firstCardId, state.answerSlots[2])
            assertNull(state.selectedCardId)
            assertTrue(state.shuffledCards.first { it.id == firstCardId }.isPlaced)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun 埋まったスロットに選択中カードを置くと元のカードが選択状態になる() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val viewModel = buildViewModel(
                dao = FakeQuizAnagramDao(
                    randomEntry = AnagramEntry(sortedKey = "ごりん", word = "りんご", length = 3),
                    words = listOf("りんご"),
                ),
            )

            viewModel.onStartQuiz()
            advanceUntilIdle()

            val firstCardId = viewModel.uiState.value.shuffledCards[0].id
            val secondCardId = viewModel.uiState.value.shuffledCards[1].id

            viewModel.onCardTapped(firstCardId)
            viewModel.onCardTapped(secondCardId)
            viewModel.onSlotTapped(0)
            viewModel.onSlotTapped(1)

            val state = viewModel.uiState.value
            assertNull(state.answerSlots[0])
            assertEquals(firstCardId, state.answerSlots[1])
            assertEquals(secondCardId, state.selectedCardId)
            assertTrue(state.shuffledCards.first { it.id == firstCardId }.isPlaced)
            assertTrue(state.shuffledCards.first { it.id == secondCardId }.isPlaced.not())
            assertTrue(secondCardId !in state.answerSlots.filterNotNull())
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun 正解時にスコアが加算されCORRECT状態になる() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val quizScoreStore = FakeQuizScoreStore()
            val viewModel = buildViewModel(
                dao = FakeQuizAnagramDao(
                    randomEntry = AnagramEntry(sortedKey = "ごりん", word = "りんご", length = 3),
                    words = listOf("りんご"),
                ),
                quizScoreStore = quizScoreStore,
            )

            viewModel.onStartQuiz()
            advanceUntilIdle()

            placeAnswer(viewModel, "りんご")
            viewModel.onSubmitAnswer()
            advanceUntilIdle()

            assertEquals(QuizPhase.CORRECT, viewModel.uiState.value.phase)
            assertEquals(10, quizScoreStore.scoreValue)
            assertEquals(1, quizScoreStore.streakValue)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun 不正解時にストリークがリセットされINCORRECT状態になる() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val quizScoreStore = FakeQuizScoreStore(initialStreak = 3)
            val viewModel = buildViewModel(
                dao = FakeQuizAnagramDao(
                    randomEntry = AnagramEntry(sortedKey = "ごりん", word = "りんご", length = 3),
                    words = listOf("りんご"),
                ),
                quizScoreStore = quizScoreStore,
            )

            viewModel.onStartQuiz()
            advanceUntilIdle()

            placeAnswer(viewModel, "ごんり")
            viewModel.onSubmitAnswer()
            advanceUntilIdle()

            assertEquals(QuizPhase.INCORRECT, viewModel.uiState.value.phase)
            assertEquals(0, quizScoreStore.streakValue)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun エントリが見つからない場合はIDLE状態でエラーを表示する() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val viewModel = buildViewModel(
                dao = FakeQuizAnagramDao(randomEntry = null),
            )

            viewModel.onStartQuiz()
            advanceUntilIdle()

            assertEquals(QuizPhase.IDLE, viewModel.uiState.value.phase)
            assertTrue(
                viewModel.uiState.value.errorMessage?.contains("見つかりませんでした") == true,
            )
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun 難易度選択が反映される() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val viewModel = buildViewModel(dao = FakeQuizAnagramDao())

            viewModel.onDifficultySelected(QuizDifficulty.HARD)

            assertEquals(QuizDifficulty.HARD, viewModel.uiState.value.difficulty)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun リセット時にスコアとストリークがゼロになる() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val quizScoreStore = FakeQuizScoreStore(initialScore = 50, initialStreak = 5)
            val viewModel = buildViewModel(
                dao = FakeQuizAnagramDao(
                    randomEntry = AnagramEntry(sortedKey = "ごりん", word = "りんご", length = 3),
                    words = listOf("りんご"),
                ),
                quizScoreStore = quizScoreStore,
            )

            viewModel.onReset()
            advanceUntilIdle()

            assertEquals(0, quizScoreStore.scoreValue)
            assertEquals(0, quizScoreStore.streakValue)
        } finally {
            Dispatchers.resetMain()
        }
    }

    private fun buildViewModel(
        dao: AnagramDao = FakeQuizAnagramDao(),
        quizScoreStore: QuizScoreStore = FakeQuizScoreStore(),
        dispatcher: CoroutineDispatcher = Dispatchers.Main,
    ): QuizViewModel = QuizViewModel(
        generateQuizUseCase = GenerateQuizUseCase(
            anagramDao = dao,
            searchAnagramUseCase = SearchAnagramUseCase(dao),
        ),
        quizScoreStore = quizScoreStore,
        ioDispatcher = dispatcher,
    )

    private fun placeAnswer(viewModel: QuizViewModel, answer: String) {
        val availableCardIds = viewModel.uiState.value.shuffledCards
            .groupBy(CharCard::char)
            .mapValues { (_, cards) -> cards.map(CharCard::id).toMutableList() }

        answer.forEach { char ->
            val ids = availableCardIds[char] ?: error("テスト用カードが不足しています: $char")
            val cardId = ids.removeAt(0)
            viewModel.onCardTapped(cardId)
        }
    }

    private class FakeQuizAnagramDao(
        private val randomEntry: AnagramEntry? = null,
        private val words: List<String> = emptyList(),
    ) : AnagramDao {
        override suspend fun insertAll(entries: List<AnagramEntry>) = Unit
        override suspend fun lookupWords(sortedKey: String): List<String> = words
        override suspend fun count(): Long = if (randomEntry != null) 1L else 0L

        override suspend fun countByLength(minLen: Int, maxLen: Int): Int = if (randomEntry != null && randomEntry.length in minLen..maxLen) 1 else 0

        override suspend fun getEntryAtOffset(minLen: Int, maxLen: Int, offset: Int): AnagramEntry? = randomEntry?.takeIf { it.length in minLen..maxLen && offset == 0 }

        override suspend fun countCommonByLength(minLen: Int, maxLen: Int): Int = if (randomEntry != null && randomEntry.length in minLen..maxLen && randomEntry.isCommon) 1 else 0

        override suspend fun getCommonEntryAtOffset(
            minLen: Int,
            maxLen: Int,
            offset: Int,
        ): AnagramEntry? = randomEntry?.takeIf {
            it.length in minLen..maxLen && it.isCommon && offset == 0
        }
    }

    private class FakeQuizScoreStore(
        initialScore: Int = 0,
        initialStreak: Int = 0,
        initialBestStreak: Int = 0,
    ) : QuizScoreStore {
        var scoreValue = initialScore
        var streakValue = initialStreak
        var bestStreakValue = initialBestStreak

        private val scoreFlow = MutableStateFlow(initialScore)
        private val streakFlow = MutableStateFlow(initialStreak)
        private val bestStreakFlow = MutableStateFlow(initialBestStreak)

        override val score: Flow<Int> = scoreFlow
        override val streak: Flow<Int> = streakFlow
        override val bestStreak: Flow<Int> = bestStreakFlow

        override suspend fun addScore(points: Int) {
            scoreValue += points
            scoreFlow.value = scoreValue
        }

        override suspend fun incrementStreak() {
            streakValue += 1
            streakFlow.value = streakValue
            if (streakValue > bestStreakValue) {
                bestStreakValue = streakValue
                bestStreakFlow.value = bestStreakValue
            }
        }

        override suspend fun resetStreak() {
            streakValue = 0
            streakFlow.value = 0
        }

        override suspend fun resetAll() {
            scoreValue = 0
            streakValue = 0
            bestStreakValue = 0
            scoreFlow.value = 0
            streakFlow.value = 0
            bestStreakFlow.value = 0
        }
    }
}
