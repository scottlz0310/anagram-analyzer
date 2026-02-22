package com.anagram.analyzer.ui.viewmodel

import com.anagram.analyzer.data.datastore.SearchSettings
import com.anagram.analyzer.data.seed.CandidateDetail

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
    val isAdditionalDictionaryDownloading: Boolean = false,
    val loadingCandidateDetailWord: String? = null,
    val candidateDetailErrorWord: String? = null,
    val candidateDetailErrorMessage: String? = null,
    val preloadLog: String? = null,
    val hasUserChangedSearchLengthRange: Boolean = false,
    val isSearchSettingsInitialized: Boolean = false,
)
