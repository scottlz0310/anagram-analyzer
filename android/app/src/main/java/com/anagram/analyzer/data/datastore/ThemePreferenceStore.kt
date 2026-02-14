package com.anagram.analyzer.data.datastore

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

@Singleton
class ThemePreferenceStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    val isDarkTheme: Flow<Boolean> = context.settingsDataStore.data
        .catch { error ->
            if (error is IOException) {
                emit(emptyPreferences())
            } else {
                throw error
            }
        }
        .map { preferences ->
            preferences[KEY_DARK_THEME] ?: false
        }

    suspend fun setDarkTheme(enabled: Boolean) {
        context.settingsDataStore.edit { preferences ->
            preferences[KEY_DARK_THEME] = enabled
        }
    }

    private companion object {
        private val KEY_DARK_THEME = booleanPreferencesKey("dark_theme")
    }
}
