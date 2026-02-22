package com.anagram.analyzer.ui.viewmodel

import com.anagram.analyzer.data.db.AnagramDao
import com.anagram.analyzer.data.db.AnagramEntry
import com.anagram.analyzer.domain.model.QuizDifficulty
import com.anagram.analyzer.domain.usecase.GenerateQuizUseCase
import com.anagram.analyzer.domain.usecase.SearchAnagramUseCase
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
import com.anagram.analyzer.data.datastore.QuizScoreStore

@OptIn(ExperimentalCoroutinesApi::class)
class QuizViewModelTest {
    @Test
    fun スタート時にANSWERING状態になる() = runTest {
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
            assertNull(viewModel.uiState.value.question?.shuffledChars?.let {
                if (it.isEmpty()) "empty" else null
            })
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

            viewModel.onInputAnswerChanged("りんご")
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

            viewModel.onInputAnswerChanged("まちがい")
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
        dispatcher: kotlinx.coroutines.CoroutineDispatcher = Dispatchers.Main,
    ): QuizViewModel = QuizViewModel(
        generateQuizUseCase = GenerateQuizUseCase(
            anagramDao = dao,
            searchAnagramUseCase = SearchAnagramUseCase(dao),
        ),
        quizScoreStore = quizScoreStore,
        ioDispatcher = dispatcher,
    )

    // ---- fakes ----

    private class FakeQuizAnagramDao(
        private val randomEntry: AnagramEntry? = null,
        private val words: List<String> = emptyList(),
    ) : AnagramDao {
        override suspend fun insertAll(entries: List<AnagramEntry>) = Unit
        override suspend fun lookupWords(sortedKey: String): List<String> = words
        override suspend fun count(): Long = if (randomEntry != null) 1L else 0L
        override suspend fun countByLength(minLen: Int, maxLen: Int): Int =
            if (randomEntry != null && randomEntry.length in minLen..maxLen) 1 else 0
        override suspend fun getEntryAtOffset(minLen: Int, maxLen: Int, offset: Int): AnagramEntry? =
            randomEntry?.takeIf { it.length in minLen..maxLen && offset == 0 }
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
