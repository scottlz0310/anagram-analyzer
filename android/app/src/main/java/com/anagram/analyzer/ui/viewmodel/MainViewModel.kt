package com.anagram.analyzer.ui.viewmodel

import android.database.sqlite.SQLiteException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anagram.analyzer.data.db.AnagramDao
import com.anagram.analyzer.data.db.AnagramEntry
import com.anagram.analyzer.domain.model.HiraganaNormalizer
import com.anagram.analyzer.domain.model.NormalizationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

data class MainUiState(
    val input: String = "",
    val normalized: String = "",
    val anagramKey: String = "",
    val candidates: List<String> = emptyList(),
    val errorMessage: String? = null,
)

@HiltViewModel
class MainViewModel @Inject constructor(
    private val anagramDao: AnagramDao,
    private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {
    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState
    private val preloadJob: Job
    private var lookupJob: Job? = null

    init {
        preloadJob = viewModelScope.launch(ioDispatcher) {
            try {
                preloadDemoDataIfNeeded()
            } catch (error: SQLiteException) {
                _uiState.update {
                    it.copy(
                        errorMessage = "データベース初期化に失敗しました: ${error.message ?: "原因不明"}",
                    )
                }
            }
        }
    }

    fun onInputChanged(value: String) {
        lookupJob?.cancel()

        if (value.isEmpty()) {
            _uiState.value = MainUiState()
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

    private suspend fun preloadDemoDataIfNeeded() {
        if (anagramDao.count() > 0) {
            return
        }

        anagramDao.insertAll(
            listOf(
                AnagramEntry(sortedKey = "ごりん", word = "りんご", length = 3),
                AnagramEntry(sortedKey = "くさら", word = "さくら", length = 3),
                AnagramEntry(sortedKey = "あいう", word = "あいう", length = 3),
            ),
        )
    }
}
