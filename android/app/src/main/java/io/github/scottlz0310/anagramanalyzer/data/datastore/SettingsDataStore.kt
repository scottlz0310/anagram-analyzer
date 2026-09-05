package io.github.scottlz0310.anagramanalyzer.data.datastore

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore

internal val Context.settingsDataStore by preferencesDataStore(name = "settings")
