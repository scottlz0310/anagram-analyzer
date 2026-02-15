package com.anagram.analyzer.data.datastore

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

interface InputHistoryStore {
    val inputHistory: Flow<List<String>>

    suspend fun setInputHistory(history: List<String>)
}

@Singleton
class DataStoreInputHistoryStore @Inject constructor(
    @ApplicationContext private val context: Context,
) : InputHistoryStore {
    override val inputHistory: Flow<List<String>> = context.settingsDataStore.data
        .catch { error ->
            if (error is IOException) {
                emit(emptyPreferences())
            } else {
                throw error
            }
        }
        .map { preferences ->
            preferences[KEY_INPUT_HISTORY]
                ?.split(HISTORY_SEPARATOR)
                ?.filter { it.isNotBlank() }
                ?.take(MAX_INPUT_HISTORY)
                ?: emptyList()
        }

    override suspend fun setInputHistory(history: List<String>) {
        val sanitizedHistory = history.filter { it.isNotBlank() }.take(MAX_INPUT_HISTORY)
        context.settingsDataStore.edit { preferences ->
            if (sanitizedHistory.isEmpty()) {
                preferences.remove(KEY_INPUT_HISTORY)
            } else {
                preferences[KEY_INPUT_HISTORY] = sanitizedHistory.joinToString(separator = HISTORY_SEPARATOR)
            }
        }
    }

    private companion object {
        private val KEY_INPUT_HISTORY = stringPreferencesKey("input_history")
        private const val HISTORY_SEPARATOR = "\n"
        private const val MAX_INPUT_HISTORY = 10
    }
}
