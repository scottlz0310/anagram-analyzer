package com.anagram.analyzer.ui.viewmodel

import android.database.sqlite.SQLiteException
import com.anagram.analyzer.data.datastore.InputHistoryStore
import com.anagram.analyzer.data.datastore.SearchSettings
import com.anagram.analyzer.data.datastore.SearchSettingsStore
import com.anagram.analyzer.data.db.AnagramDao
import com.anagram.analyzer.data.db.AnagramEntry
import com.anagram.analyzer.data.seed.CandidateDetail
import com.anagram.analyzer.data.seed.CandidateDetailLoader
import com.anagram.analyzer.data.seed.SeedEntryLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
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
                seedEntryLoader = FakeSeedEntryLoader(),
                candidateDetailLoader = FakeCandidateDetailLoader(),
                inputHistoryStore = FakeInputHistoryStore(),
                searchSettingsStore = FakeSearchSettingsStore(),
                ioDispatcher = dispatcher,
                preloadLogger = PreloadLogger { _ -> },
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
    fun 候補表示時に入力履歴へ追加する() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val viewModel = MainViewModel(
                anagramDao = FakeAnagramDao(),
                seedEntryLoader = FakeSeedEntryLoader(),
                candidateDetailLoader = FakeCandidateDetailLoader(),
                inputHistoryStore = FakeInputHistoryStore(),
                searchSettingsStore = FakeSearchSettingsStore(),
                ioDispatcher = dispatcher,
                preloadLogger = PreloadLogger { _ -> },
            )

            viewModel.onInputChanged("りんご")
            advanceUntilIdle()

            assertEquals(listOf("りんご"), viewModel.uiState.value.inputHistory)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun 保存済み文字数範囲を起動時に復元する() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val viewModel = MainViewModel(
                anagramDao = FakeAnagramDao(),
                seedEntryLoader = FakeSeedEntryLoader(),
                candidateDetailLoader = FakeCandidateDetailLoader(),
                inputHistoryStore = FakeInputHistoryStore(),
                searchSettingsStore = FakeSearchSettingsStore(
                    initialSettings = SearchSettings(minLength = 3, maxLength = 8),
                ),
                ioDispatcher = dispatcher,
                preloadLogger = PreloadLogger { _ -> },
            )

            advanceUntilIdle()

            assertEquals(3, viewModel.uiState.value.minSearchLength)
            assertEquals(8, viewModel.uiState.value.maxSearchLength)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun 文字数範囲外の入力はエラーを表示する() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val viewModel = MainViewModel(
                anagramDao = FakeAnagramDao(),
                seedEntryLoader = FakeSeedEntryLoader(),
                candidateDetailLoader = FakeCandidateDetailLoader(),
                inputHistoryStore = FakeInputHistoryStore(),
                searchSettingsStore = FakeSearchSettingsStore(
                    initialSettings = SearchSettings(minLength = 4, maxLength = 8),
                ),
                ioDispatcher = dispatcher,
                preloadLogger = PreloadLogger { _ -> },
            )

            advanceUntilIdle()
            viewModel.onInputChanged("りんご")
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertTrue(state.errorMessage?.contains("文字数は4〜8文字で入力してください") == true)
            assertTrue(state.candidates.isEmpty())
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun 入力履歴は重複を先頭へ寄せる() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val viewModel = MainViewModel(
                anagramDao = FakeAnagramDao(),
                seedEntryLoader = FakeSeedEntryLoader(),
                candidateDetailLoader = FakeCandidateDetailLoader(),
                inputHistoryStore = FakeInputHistoryStore(),
                searchSettingsStore = FakeSearchSettingsStore(),
                ioDispatcher = dispatcher,
                preloadLogger = PreloadLogger { _ -> },
            )

            viewModel.onInputChanged("りんご")
            advanceUntilIdle()
            viewModel.onInputChanged("さくら")
            advanceUntilIdle()
            viewModel.onInputChanged("りんご")
            advanceUntilIdle()

            assertEquals(listOf("りんご", "さくら"), viewModel.uiState.value.inputHistory)
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
                seedEntryLoader = FakeSeedEntryLoader(),
                candidateDetailLoader = FakeCandidateDetailLoader(),
                inputHistoryStore = FakeInputHistoryStore(),
                searchSettingsStore = FakeSearchSettingsStore(),
                ioDispatcher = dispatcher,
                preloadLogger = PreloadLogger { _ -> },
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
                seedEntryLoader = FakeSeedEntryLoader(),
                candidateDetailLoader = FakeCandidateDetailLoader(),
                inputHistoryStore = FakeInputHistoryStore(),
                searchSettingsStore = FakeSearchSettingsStore(),
                ioDispatcher = dispatcher,
                preloadLogger = PreloadLogger { _ -> },
            )

            advanceUntilIdle()

            assertTrue(
                viewModel.uiState.value.errorMessage?.contains("データベース初期化に失敗しました") == true,
            )
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun seed読み込み失敗時はエラーメッセージを反映する() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val viewModel = MainViewModel(
                anagramDao = FakeAnagramDao(),
                seedEntryLoader = FakeSeedEntryLoader(loadFailure = IllegalArgumentException("bad seed")),
                candidateDetailLoader = FakeCandidateDetailLoader(),
                inputHistoryStore = FakeInputHistoryStore(),
                searchSettingsStore = FakeSearchSettingsStore(),
                ioDispatcher = dispatcher,
                preloadLogger = PreloadLogger { _ -> },
            )

            advanceUntilIdle()

            assertTrue(
                viewModel.uiState.value.errorMessage?.contains("辞書データの読み込みに失敗しました") == true,
            )
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun preload完了時に計測ログを保持する() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val viewModel = MainViewModel(
                anagramDao = FakeAnagramDao(),
                seedEntryLoader = FakeSeedEntryLoader(
                    entries = listOf(
                        AnagramEntry(sortedKey = "ごりん", word = "りんご", length = 3),
                        AnagramEntry(sortedKey = "くさら", word = "さくら", length = 3),
                    ),
                ),
                candidateDetailLoader = FakeCandidateDetailLoader(),
                inputHistoryStore = FakeInputHistoryStore(),
                searchSettingsStore = FakeSearchSettingsStore(),
                ioDispatcher = dispatcher,
                preloadLogger = PreloadLogger { _ -> },
            )

            advanceUntilIdle()

            val preloadLog = viewModel.uiState.value.preloadLog.orEmpty()
            assertTrue(preloadLog.contains("source=seed_asset"))
            assertTrue(preloadLog.contains("total=2"))
            assertTrue(preloadLog.contains("inserted=2"))
            assertTrue(preloadLog.contains("elapsedMs="))
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun 候補詳細データをstateに保持する() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val viewModel = MainViewModel(
                anagramDao = FakeAnagramDao(),
                seedEntryLoader = FakeSeedEntryLoader(),
                candidateDetailLoader = FakeCandidateDetailLoader(
                    details = mapOf(
                        "りんご" to CandidateDetail(kanji = "林檎", meaning = "apple"),
                    ),
                ),
                inputHistoryStore = FakeInputHistoryStore(),
                searchSettingsStore = FakeSearchSettingsStore(),
                ioDispatcher = dispatcher,
                preloadLogger = PreloadLogger { _ -> },
            )

            advanceUntilIdle()

            assertEquals("林檎", viewModel.uiState.value.candidateDetails["りんご"]?.kanji)
            assertEquals("apple", viewModel.uiState.value.candidateDetails["りんご"]?.meaning)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun 保存済み入力履歴を起動時に復元する() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val inputHistoryStore = FakeInputHistoryStore(
                initialHistory = listOf("りんご", "さくら"),
            )
            val viewModel = MainViewModel(
                anagramDao = FakeAnagramDao(),
                seedEntryLoader = FakeSeedEntryLoader(),
                candidateDetailLoader = FakeCandidateDetailLoader(),
                inputHistoryStore = inputHistoryStore,
                searchSettingsStore = FakeSearchSettingsStore(),
                ioDispatcher = dispatcher,
                preloadLogger = PreloadLogger { _ -> },
            )

            advanceUntilIdle()

            assertEquals(listOf("りんご", "さくら"), viewModel.uiState.value.inputHistory)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun 保存済み入力履歴は10件まで復元する() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val persistedHistory = (1..12).map { "りんご$it" }
            val inputHistoryStore = FakeInputHistoryStore(initialHistory = persistedHistory)
            val viewModel = MainViewModel(
                anagramDao = FakeAnagramDao(),
                seedEntryLoader = FakeSeedEntryLoader(),
                candidateDetailLoader = FakeCandidateDetailLoader(),
                inputHistoryStore = inputHistoryStore,
                searchSettingsStore = FakeSearchSettingsStore(),
                ioDispatcher = dispatcher,
                preloadLogger = PreloadLogger { _ -> },
            )

            advanceUntilIdle()

            assertEquals(10, viewModel.uiState.value.inputHistory.size)
            assertEquals(persistedHistory.take(10), viewModel.uiState.value.inputHistory)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun 入力履歴更新時に永続化する() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val inputHistoryStore = FakeInputHistoryStore()
            val viewModel = MainViewModel(
                anagramDao = FakeAnagramDao(),
                seedEntryLoader = FakeSeedEntryLoader(),
                candidateDetailLoader = FakeCandidateDetailLoader(),
                inputHistoryStore = inputHistoryStore,
                searchSettingsStore = FakeSearchSettingsStore(),
                ioDispatcher = dispatcher,
                preloadLogger = PreloadLogger { _ -> },
            )

            viewModel.onInputChanged("りんご")
            advanceUntilIdle()

            assertEquals(listOf("りんご"), inputHistoryStore.persistedHistory)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun 文字数範囲変更時に永続化する() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val searchSettingsStore = FakeSearchSettingsStore()
            val viewModel = MainViewModel(
                anagramDao = FakeAnagramDao(),
                seedEntryLoader = FakeSeedEntryLoader(),
                candidateDetailLoader = FakeCandidateDetailLoader(),
                inputHistoryStore = FakeInputHistoryStore(),
                searchSettingsStore = searchSettingsStore,
                ioDispatcher = dispatcher,
                preloadLogger = PreloadLogger { _ -> },
            )

            advanceUntilIdle()
            viewModel.onSearchLengthRangeChanged(minLength = 3, maxLength = 10)
            advanceUntilIdle()

            assertEquals(3, viewModel.uiState.value.minSearchLength)
            assertEquals(10, viewModel.uiState.value.maxSearchLength)
            assertEquals(SearchSettings(minLength = 3, maxLength = 10), searchSettingsStore.persistedSettings)
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

    private class FakeSeedEntryLoader(
        private val entries: List<AnagramEntry> = emptyList(),
        private val loadFailure: IllegalArgumentException? = null,
    ) : SeedEntryLoader {
        override suspend fun loadEntries(): List<AnagramEntry> {
            if (loadFailure != null) {
                throw loadFailure
            }
            return entries
        }
    }

    private class FakeCandidateDetailLoader(
        private val details: Map<String, CandidateDetail> = emptyMap(),
    ) : CandidateDetailLoader {
        override suspend fun loadDetails(): Map<String, CandidateDetail> {
            return details
        }
    }

    private class FakeInputHistoryStore(
        initialHistory: List<String> = emptyList(),
    ) : InputHistoryStore {
        private val historyFlow = MutableStateFlow(initialHistory)
        var persistedHistory: List<String> = initialHistory

        override val inputHistory = historyFlow

        override suspend fun setInputHistory(history: List<String>) {
            persistedHistory = history
            historyFlow.value = history
        }
    }

    private class FakeSearchSettingsStore(
        initialSettings: SearchSettings = SearchSettings(),
    ) : SearchSettingsStore {
        private val settingsFlow = MutableStateFlow(initialSettings)
        var persistedSettings: SearchSettings = initialSettings

        override val searchSettings = settingsFlow

        override suspend fun setSearchLengthRange(minLength: Int, maxLength: Int) {
            persistedSettings = SearchSettings(minLength = minLength, maxLength = maxLength)
            settingsFlow.value = persistedSettings
        }
    }
}
