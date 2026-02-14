package com.anagram.analyzer.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.anagram.analyzer.data.db.AnagramDao
import com.anagram.analyzer.data.db.AnagramDatabase
import com.anagram.analyzer.data.db.AnagramEntry
import com.anagram.analyzer.domain.model.HiraganaNormalizer
import com.anagram.analyzer.domain.model.NormalizationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class MainUiState(
    val input: String = "",
    val normalized: String = "",
    val anagramKey: String = "",
    val candidates: List<String> = emptyList(),
    val errorMessage: String? = null,
)

class MainViewModel(
    private val anagramDao: AnagramDao,
) : ViewModel() {
    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState
    private val preloadJob: Job
    private var lookupJob: Job? = null

    init {
        preloadJob = viewModelScope.launch(Dispatchers.IO) {
            preloadDemoDataIfNeeded()
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
                val words = withContext(Dispatchers.IO) {
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

class MainViewModelFactory(
    private val context: Context,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(
                AnagramDatabase.getInstance(context).anagramDao(),
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
