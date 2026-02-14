package com.anagram.analyzer.ui.viewmodel

import android.database.sqlite.SQLiteException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anagram.analyzer.data.db.AnagramDao
import com.anagram.analyzer.data.db.AnagramEntry
import com.anagram.analyzer.data.seed.SeedEntryLoader
import com.anagram.analyzer.domain.model.HiraganaNormalizer
import com.anagram.analyzer.domain.model.NormalizationException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
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
    val errorMessage: String? = null,
    val preloadLog: String? = null,
)

@HiltViewModel
class MainViewModel @Inject constructor(
    private val anagramDao: AnagramDao,
    private val seedEntryLoader: SeedEntryLoader,
    private val ioDispatcher: CoroutineDispatcher,
    private val preloadLogger: PreloadLogger,
) : ViewModel() {
    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState
    private val preloadJob: Job
    private var lookupJob: Job? = null

    init {
        preloadJob = viewModelScope.launch(ioDispatcher) {
            try {
                val metrics = preloadSeedDataIfNeeded()
                val preloadLog = metrics.toLogLine()
                preloadLogger.log(preloadLog)
                _uiState.update { it.copy(preloadLog = preloadLog) }
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
                MainUiState(preloadLog = state.preloadLog)
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
                val words = withContext(ioDispatcher) {
                    anagramDao.lookupWords(anagramKey)
                }
                _uiState.update { state ->
                    if (state.input != value) {
                        state
                    } else {
                        state.copy(candidates = words)
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

    private companion object {
        private val DEMO_ENTRIES = listOf(
            AnagramEntry(sortedKey = "ごりん", word = "りんご", length = 3),
            AnagramEntry(sortedKey = "くさら", word = "さくら", length = 3),
            AnagramEntry(sortedKey = "あいう", word = "あいう", length = 3),
        )
    }
}
