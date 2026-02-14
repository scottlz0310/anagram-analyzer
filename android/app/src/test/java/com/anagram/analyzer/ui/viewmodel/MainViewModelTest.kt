package com.anagram.analyzer.ui.viewmodel

import android.database.sqlite.SQLiteException
import com.anagram.analyzer.data.db.AnagramDao
import com.anagram.analyzer.data.db.AnagramEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.resetMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {
    @Test
    fun preload完了前でも候補を取得できる() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val viewModel = MainViewModel(
                anagramDao = FakeAnagramDao(insertDelayMs = 100),
                ioDispatcher = dispatcher,
            )

            viewModel.onInputChanged("りんご")
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals("りんご", state.input)
            assertEquals(listOf("りんご"), state.candidates)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun 連続入力時は最新入力の結果のみ反映する() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val viewModel = MainViewModel(
                anagramDao = FakeAnagramDao(
                    initialEntries = listOf(
                        AnagramEntry(sortedKey = "ごりん", word = "りんご", length = 3),
                        AnagramEntry(sortedKey = "くさら", word = "さくら", length = 3),
                    ),
                    lookupDelayByKey = mapOf(
                        "ごりん" to 200,
                        "くさら" to 10,
                    ),
                ),
                ioDispatcher = dispatcher,
            )

            viewModel.onInputChanged("りんご")
            viewModel.onInputChanged("さくら")
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals("さくら", state.input)
            assertEquals(listOf("さくら"), state.candidates)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun preload失敗時はエラーメッセージを反映する() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val viewModel = MainViewModel(
                anagramDao = FakeAnagramDao(
                    countFailure = SQLiteException("DB error"),
                ),
                ioDispatcher = dispatcher,
            )

            advanceUntilIdle()

            assertTrue(
                viewModel.uiState.value.errorMessage?.contains("データベース初期化に失敗しました") == true,
            )
        } finally {
            Dispatchers.resetMain()
        }
    }

    private class FakeAnagramDao(
        initialEntries: List<AnagramEntry> = emptyList(),
        private val insertDelayMs: Long = 0,
        private val lookupDelayByKey: Map<String, Long> = emptyMap(),
        private val countFailure: SQLiteException? = null,
    ) : AnagramDao {
        private val entries = initialEntries.toMutableList()

        override suspend fun insertAll(entries: List<AnagramEntry>) {
            delay(insertDelayMs)
            entries.forEach { candidate ->
                val duplicated = this.entries.any {
                    it.sortedKey == candidate.sortedKey && it.word == candidate.word
                }
                if (!duplicated) {
                    this.entries.add(candidate)
                }
            }
        }

        override suspend fun lookupWords(sortedKey: String): List<String> {
            delay(lookupDelayByKey[sortedKey] ?: 0)
            return entries
                .filter { it.sortedKey == sortedKey }
                .map { it.word }
                .sorted()
        }

        override suspend fun count(): Long {
            if (countFailure != null) {
                throw countFailure
            }
            return entries.size.toLong()
        }
    }
}
