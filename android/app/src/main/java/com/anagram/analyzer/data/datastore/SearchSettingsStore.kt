package com.anagram.analyzer.data.datastore

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

data class SearchSettings(
    val minLength: Int = DEFAULT_MIN_LENGTH,
    val maxLength: Int = DEFAULT_MAX_LENGTH,
) {
    companion object {
        const val DEFAULT_MIN_LENGTH = 2
        const val DEFAULT_MAX_LENGTH = 20
        const val ABSOLUTE_MIN_LENGTH = 1
        const val ABSOLUTE_MAX_LENGTH = 20
    }
}

interface SearchSettingsStore {
    val searchSettings: Flow<SearchSettings>

    suspend fun setSearchLengthRange(minLength: Int, maxLength: Int)
}

@Singleton
class DataStoreSearchSettingsStore @Inject constructor(
    @ApplicationContext private val context: Context,
) : SearchSettingsStore {
    override val searchSettings: Flow<SearchSettings> = context.settingsDataStore.data
        .catch { error ->
            if (error is IOException) {
                emit(emptyPreferences())
            } else {
                throw error
            }
        }
        .map { preferences ->
            val minLength = (preferences[KEY_MIN_LENGTH] ?: SearchSettings.DEFAULT_MIN_LENGTH)
                .coerceIn(SearchSettings.ABSOLUTE_MIN_LENGTH, SearchSettings.ABSOLUTE_MAX_LENGTH)
            val maxLength = (preferences[KEY_MAX_LENGTH] ?: SearchSettings.DEFAULT_MAX_LENGTH)
                .coerceIn(minLength, SearchSettings.ABSOLUTE_MAX_LENGTH)
            SearchSettings(minLength = minLength, maxLength = maxLength)
        }

    override suspend fun setSearchLengthRange(minLength: Int, maxLength: Int) {
        val sanitizedMinLength = minLength.coerceIn(
            SearchSettings.ABSOLUTE_MIN_LENGTH,
            SearchSettings.ABSOLUTE_MAX_LENGTH,
        )
        val sanitizedMaxLength = maxLength.coerceIn(
            sanitizedMinLength,
            SearchSettings.ABSOLUTE_MAX_LENGTH,
        )
        context.settingsDataStore.edit { preferences ->
            preferences[KEY_MIN_LENGTH] = sanitizedMinLength
            preferences[KEY_MAX_LENGTH] = sanitizedMaxLength
        }
    }

    private companion object {
        private val KEY_MIN_LENGTH = intPreferencesKey("search_min_length")
        private val KEY_MAX_LENGTH = intPreferencesKey("search_max_length")
    }
}
