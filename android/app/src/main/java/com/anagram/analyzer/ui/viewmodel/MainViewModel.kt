package com.anagram.analyzer.ui.viewmodel

import android.database.sqlite.SQLiteException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anagram.analyzer.data.datastore.InputHistoryStore
import com.anagram.analyzer.data.datastore.SearchSettings
import com.anagram.analyzer.data.datastore.SearchSettingsStore
import com.anagram.analyzer.data.db.AnagramDao
import com.anagram.analyzer.data.db.AnagramEntry
import com.anagram.analyzer.data.seed.CandidateDetail
import com.anagram.analyzer.data.seed.CandidateDetailLoader
import com.anagram.analyzer.data.seed.SeedEntryLoader
import com.anagram.analyzer.domain.model.HiraganaNormalizer
import com.anagram.analyzer.domain.model.NormalizationException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.TimeSource
import javax.inject.Inject

fun interface PreloadLogger {
    fun log(message: String)
}

private data class PreloadMetrics(
    val source: String,
    val totalEntries: Long,
    val insertedEntries: Long,
    val elapsedMillis: Long,
)

data class MainUiState(
    val input: String = "",
    val normalized: String = "",
    val anagramKey: String = "",
    val candidates: List<String> = emptyList(),
    val minSearchLength: Int = SearchSettings.DEFAULT_MIN_LENGTH,
    val maxSearchLength: Int = SearchSettings.DEFAULT_MAX_LENGTH,
    val inputHistory: List<String> = emptyList(),
    val candidateDetails: Map<String, CandidateDetail> = emptyMap(),
    val errorMessage: String? = null,
    val settingsMessage: String? = null,
    val preloadLog: String? = null,
)

@HiltViewModel
class MainViewModel @Inject constructor(
    private val anagramDao: AnagramDao,
    private val seedEntryLoader: SeedEntryLoader,
    private val candidateDetailLoader: CandidateDetailLoader,
    private val inputHistoryStore: InputHistoryStore,
    private val searchSettingsStore: SearchSettingsStore,
    private val ioDispatcher: CoroutineDispatcher,
    private val preloadLogger: PreloadLogger,
) : ViewModel() {
    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState
    private val preloadJob: Job
    private var lookupJob: Job? = null
    private var searchSettingsPersistJob: Job? = null

    init {
        preloadJob = viewModelScope.launch(ioDispatcher) {
            val persistedInputHistory = inputHistoryStore.inputHistory.first().take(MAX_INPUT_HISTORY)
            val persistedSearchSettings = searchSettingsStore.searchSettings.first()
            _uiState.update { state ->
                val shouldApplyPersistedSearchSettings =
                    state.minSearchLength == SearchSettings.DEFAULT_MIN_LENGTH &&
                        state.maxSearchLength == SearchSettings.DEFAULT_MAX_LENGTH
                state.copy(
                    inputHistory = if (state.inputHistory.isEmpty()) {
                        persistedInputHistory
                    } else {
                        state.inputHistory
                    },
                    minSearchLength = if (shouldApplyPersistedSearchSettings) {
                        persistedSearchSettings.minLength
                    } else {
                        state.minSearchLength
                    },
                    maxSearchLength = if (shouldApplyPersistedSearchSettings) {
                        persistedSearchSettings.maxLength
                    } else {
                        state.maxSearchLength
                    },
                )
            }
            try {
                val metrics = preloadSeedDataIfNeeded()
                val candidateDetails = candidateDetailLoader.loadDetails()
                val preloadLog = metrics.toLogLine()
                preloadLogger.log(preloadLog)
                _uiState.update {
                    it.copy(
                        preloadLog = preloadLog,
                        candidateDetails = candidateDetails,
                    )
                }
            } catch (error: SQLiteException) {
                _uiState.update {
                    it.copy(
                        errorMessage = "データベース初期化に失敗しました: ${error.message ?: "原因不明"}",
                    )
                }
            } catch (error: IllegalArgumentException) {
                _uiState.update {
                    it.copy(
                        errorMessage = "辞書データの読み込みに失敗しました: ${error.message ?: "原因不明"}",
                    )
                }
            }
        }
    }

    fun onInputChanged(value: String) {
        lookupJob?.cancel()

        if (value.isEmpty()) {
            _uiState.update { state ->
                MainUiState(
                    preloadLog = state.preloadLog,
                    inputHistory = state.inputHistory,
                    candidateDetails = state.candidateDetails,
                    minSearchLength = state.minSearchLength,
                    maxSearchLength = state.maxSearchLength,
                    settingsMessage = state.settingsMessage,
                )
            }
            return
        }

        try {
            val normalized = HiraganaNormalizer.normalizeHiragana(value)
            val anagramKey = HiraganaNormalizer.anagramKey(normalized)
            _uiState.update {
                it.copy(
                    input = value,
                    normalized = normalized,
                    anagramKey = anagramKey,
                    candidates = emptyList(),
                    errorMessage = null,
                )
            }

            lookupJob = viewModelScope.launch {
                preloadJob.join()
                val currentState = _uiState.value
                if (normalized.length !in currentState.minSearchLength..currentState.maxSearchLength) {
                    _uiState.update { state ->
                        if (state.input != value) {
                            state
                        } else {
                            state.copy(
                                anagramKey = "",
                                candidates = emptyList(),
                                errorMessage = "文字数は${state.minSearchLength}〜${state.maxSearchLength}文字で入力してください",
                            )
                        }
                    }
                    return@launch
                }
                val words = withContext(ioDispatcher) {
                    anagramDao.lookupWords(anagramKey)
                }
                val updatedState = _uiState.updateAndGet { state ->
                    if (state.input != value) {
                        state
                    } else {
                        val updatedHistory = if (words.isNotEmpty()) {
                            appendInputHistory(state.inputHistory, normalized)
                        } else {
                            state.inputHistory
                        }
                        state.copy(
                            candidates = words,
                            inputHistory = updatedHistory,
                        )
                    }
                }
                if (updatedState.input == value && words.isNotEmpty()) {
                    withContext(ioDispatcher) {
                        inputHistoryStore.setInputHistory(updatedState.inputHistory)
                    }
                }
            }
        } catch (error: NormalizationException) {
            _uiState.update {
                it.copy(
                    input = value,
                    normalized = "",
                    anagramKey = "",
                    candidates = emptyList(),
                    errorMessage = error.message,
                )
            }
        }
    }

    fun onSearchLengthRangeChanged(minLength: Int, maxLength: Int) {
        val sanitizedMinLength = minLength.coerceIn(
            SearchSettings.ABSOLUTE_MIN_LENGTH,
            SearchSettings.ABSOLUTE_MAX_LENGTH,
        )
        val sanitizedMaxLength = maxLength.coerceIn(
            sanitizedMinLength,
            SearchSettings.ABSOLUTE_MAX_LENGTH,
        )
        _uiState.update {
            it.copy(
                minSearchLength = sanitizedMinLength,
                maxSearchLength = sanitizedMaxLength,
                settingsMessage = null,
            )
        }
        searchSettingsPersistJob?.cancel()
        searchSettingsPersistJob = viewModelScope.launch(ioDispatcher) {
            searchSettingsStore.setSearchLengthRange(
                minLength = sanitizedMinLength,
                maxLength = sanitizedMaxLength,
            )
        }
        val currentInput = _uiState.value.input
        if (currentInput.isNotEmpty()) {
            onInputChanged(currentInput)
        }
    }

    fun onAdditionalDictionaryDownloadRequested() {
        _uiState.update { it.copy(settingsMessage = "現在、追加辞書ダウンロード機能は準備中です") }
    }

    private suspend fun preloadSeedDataIfNeeded(): PreloadMetrics {
        val started = TimeSource.Monotonic.markNow()
        val beforeCount = anagramDao.count()
        if (beforeCount > 0) {
            return PreloadMetrics(
                source = "existing_db",
                totalEntries = beforeCount,
                insertedEntries = 0,
                elapsedMillis = started.elapsedNow().inWholeMilliseconds,
            )
        }

        val seedEntries = seedEntryLoader.loadEntries()
        if (seedEntries.isNotEmpty()) {
            anagramDao.insertAll(seedEntries)
            val afterCount = anagramDao.count()
            return PreloadMetrics(
                source = "seed_asset",
                totalEntries = afterCount,
                insertedEntries = (afterCount - beforeCount).coerceAtLeast(0),
                elapsedMillis = started.elapsedNow().inWholeMilliseconds,
            )
        }

        anagramDao.insertAll(DEMO_ENTRIES)
        val afterCount = anagramDao.count()
        return PreloadMetrics(
            source = "demo_fallback",
            totalEntries = afterCount,
            insertedEntries = (afterCount - beforeCount).coerceAtLeast(0),
            elapsedMillis = started.elapsedNow().inWholeMilliseconds,
        )
    }

    private fun PreloadMetrics.toLogLine(): String {
        return "preload source=$source total=$totalEntries inserted=$insertedEntries elapsedMs=$elapsedMillis"
    }

    private fun appendInputHistory(history: List<String>, value: String): List<String> {
        return buildList {
            add(value)
            addAll(history.filterNot { it == value })
        }.take(MAX_INPUT_HISTORY)
    }

    private companion object {
        private const val MAX_INPUT_HISTORY = 10
        private val DEMO_ENTRIES = listOf(
            AnagramEntry(sortedKey = "ごりん", word = "りんご", length = 3),
            AnagramEntry(sortedKey = "くさら", word = "さくら", length = 3),
            AnagramEntry(sortedKey = "あいう", word = "あいう", length = 3),
        )
    }
}
