package com.anagram.analyzer.data.datastore

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private val Context.quizScoreDataStore by preferencesDataStore(name = "quiz_score")

interface QuizScoreStore {
    val score: Flow<Int>
    val streak: Flow<Int>
    val bestStreak: Flow<Int>

    suspend fun addScore(points: Int)
    suspend fun incrementStreak()
    suspend fun resetStreak()
    suspend fun resetAll()
}

class DataStoreQuizScoreStore(context: Context) : QuizScoreStore {
    private val appContext = context.applicationContext
    override val score: Flow<Int> = appContext.quizScoreDataStore.data
        .catch { error -> if (error is IOException) emit(emptyPreferences()) else throw error }
        .map { it[KEY_SCORE] ?: 0 }

    override val streak: Flow<Int> = appContext.quizScoreDataStore.data
        .catch { error -> if (error is IOException) emit(emptyPreferences()) else throw error }
        .map { it[KEY_STREAK] ?: 0 }

    override val bestStreak: Flow<Int> = appContext.quizScoreDataStore.data
        .catch { error -> if (error is IOException) emit(emptyPreferences()) else throw error }
        .map { it[KEY_BEST_STREAK] ?: 0 }

    override suspend fun addScore(points: Int) {
        appContext.quizScoreDataStore.edit { prefs ->
            prefs[KEY_SCORE] = (prefs[KEY_SCORE] ?: 0) + points
        }
    }

    override suspend fun incrementStreak() {
        appContext.quizScoreDataStore.edit { prefs ->
            val newStreak = (prefs[KEY_STREAK] ?: 0) + 1
            prefs[KEY_STREAK] = newStreak
            val best = prefs[KEY_BEST_STREAK] ?: 0
            if (newStreak > best) prefs[KEY_BEST_STREAK] = newStreak
        }
    }

    override suspend fun resetStreak() {
        appContext.quizScoreDataStore.edit { prefs ->
            prefs[KEY_STREAK] = 0
        }
    }

    override suspend fun resetAll() {
        appContext.quizScoreDataStore.edit { prefs ->
            prefs[KEY_SCORE] = 0
            prefs[KEY_STREAK] = 0
            prefs[KEY_BEST_STREAK] = 0
        }
    }

    private companion object {
        private val KEY_SCORE = intPreferencesKey("quiz_score")
        private val KEY_STREAK = intPreferencesKey("quiz_streak")
        private val KEY_BEST_STREAK = intPreferencesKey("quiz_best_streak")
    }
}
