package com.anagram.analyzer.ui.viewmodel

import android.database.sqlite.SQLiteException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anagram.analyzer.data.datastore.InputHistoryStore
import com.anagram.analyzer.data.datastore.SearchSettings
import com.anagram.analyzer.data.datastore.SearchSettingsStore
import com.anagram.analyzer.domain.model.HiraganaNormalizer
import com.anagram.analyzer.domain.model.NormalizationException
import com.anagram.analyzer.domain.usecase.ApplyAdditionalDictionaryUseCase
import com.anagram.analyzer.domain.usecase.LoadCandidateDetailUseCase
import com.anagram.analyzer.domain.usecase.PreloadSeedUseCase
import com.anagram.analyzer.domain.usecase.SearchAnagramUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val preloadSeedUseCase: PreloadSeedUseCase,
    private val searchAnagramUseCase: SearchAnagramUseCase,
    private val loadCandidateDetailUseCase: LoadCandidateDetailUseCase,
    private val applyAdditionalDictionaryUseCase: ApplyAdditionalDictionaryUseCase,
    private val inputHistoryStore: InputHistoryStore,
    private val searchSettingsStore: SearchSettingsStore,
    private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {
    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState
    private val preloadJob: Job
    private var lookupJob: Job? = null
    private var searchSettingsPersistJob: Job? = null
    private var additionalDictionaryJob: Job? = null
    private var candidateDetailFetchJob: Job? = null

    init {
        preloadJob = viewModelScope.launch(ioDispatcher) {
            val persistedInputHistory = inputHistoryStore.inputHistory.first().take(MAX_INPUT_HISTORY)
            val persistedSearchSettings = searchSettingsStore.searchSettings.first()
            _uiState.update { state ->
                val shouldApplyPersistedSearchSettings =
                    !state.hasUserChangedSearchLengthRange &&
                        !state.isSearchSettingsInitialized
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
                    isSearchSettingsInitialized = true,
                )
            }
            try {
                val result = preloadSeedUseCase.execute()
                _uiState.update {
                    it.copy(
                        preloadLog = result.preloadLog,
                        candidateDetails = result.candidateDetails,
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
                    isAdditionalDictionaryDownloading = state.isAdditionalDictionaryDownloading,
                    loadingCandidateDetailWord = state.loadingCandidateDetailWord,
                    candidateDetailErrorWord = state.candidateDetailErrorWord,
                    candidateDetailErrorMessage = state.candidateDetailErrorMessage,
                    hasUserChangedSearchLengthRange = state.hasUserChangedSearchLengthRange,
                    isSearchSettingsInitialized = state.isSearchSettingsInitialized,
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
                    searchAnagramUseCase.execute(anagramKey)
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
        val currentState = _uiState.value
        if (
            currentState.minSearchLength == sanitizedMinLength &&
            currentState.maxSearchLength == sanitizedMaxLength
        ) {
            return
        }
        _uiState.update {
            it.copy(
                minSearchLength = sanitizedMinLength,
                maxSearchLength = sanitizedMaxLength,
                hasUserChangedSearchLengthRange = true,
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
        if (additionalDictionaryJob?.isActive == true) {
            return
        }
        _uiState.update {
            it.copy(
                isAdditionalDictionaryDownloading = true,
                settingsMessage = "追加辞書を適用中です...",
            )
        }
        additionalDictionaryJob = viewModelScope.launch {
            preloadJob.join()
            try {
                val (insertedEntries, totalEntries) = withContext(ioDispatcher) {
                    applyAdditionalDictionaryUseCase.execute()
                }
                _uiState.update {
                    it.copy(
                        settingsMessage = if (insertedEntries > 0) {
                            "追加辞書を適用しました（追加${insertedEntries}件 / 読込${totalEntries}件）"
                        } else {
                            "追加辞書は最新です（追加0件 / 読込${totalEntries}件）"
                        },
                    )
                }
            } catch (error: Throwable) {
                if (error is CancellationException) {
                    throw error
                }
                if (
                    error is IllegalArgumentException ||
                    error is IllegalStateException ||
                    error is SQLiteException
                ) {
                    _uiState.update {
                        it.copy(
                            settingsMessage = "追加辞書の適用に失敗しました: ${error.message ?: "原因不明"}",
                        )
                    }
                } else {
                    throw error
                }
            } finally {
                _uiState.update {
                    it.copy(
                        isAdditionalDictionaryDownloading = false,
                    )
                }
            }
        }
    }

    fun onCandidateDetailFetchRequested(word: String) {
        val currentState = _uiState.value
        if (currentState.candidateDetails.containsKey(word)) {
            return
        }
        if (currentState.loadingCandidateDetailWord == word) {
            return
        }
        candidateDetailFetchJob?.cancel()
        _uiState.update {
            it.copy(
                loadingCandidateDetailWord = word,
                candidateDetailErrorWord = null,
                candidateDetailErrorMessage = null,
            )
        }
        candidateDetailFetchJob = viewModelScope.launch {
            preloadJob.join()
            try {
                val fetchedDetail = withContext(ioDispatcher) {
                    loadCandidateDetailUseCase.execute(word)
                }
                _uiState.update { state ->
                    if (fetchedDetail != null) {
                        state.copy(
                            candidateDetails = state.candidateDetails + (word to fetchedDetail),
                            candidateDetailErrorWord = null,
                            candidateDetailErrorMessage = null,
                        )
                    } else {
                        state.copy(
                            candidateDetailErrorWord = word,
                            candidateDetailErrorMessage = "候補詳細を取得できませんでした",
                        )
                    }
                }
            } catch (error: Throwable) {
                if (error is CancellationException) {
                    throw error
                }
                _uiState.update {
                    it.copy(
                        candidateDetailErrorWord = word,
                        candidateDetailErrorMessage = "候補詳細の取得に失敗しました: ${error.message ?: "原因不明"}",
                    )
                }
            } finally {
                _uiState.update { state ->
                    if (state.loadingCandidateDetailWord == word) {
                        state.copy(loadingCandidateDetailWord = null)
                    } else {
                        state
                    }
                }
            }
        }
    }

    private fun appendInputHistory(history: List<String>, value: String): List<String> {
        return buildList {
            add(value)
            addAll(history.filterNot { it == value })
        }.take(MAX_INPUT_HISTORY)
    }

    private companion object {
        private const val MAX_INPUT_HISTORY = 10
    }
}
