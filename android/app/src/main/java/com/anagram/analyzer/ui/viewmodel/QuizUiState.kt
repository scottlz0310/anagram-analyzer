package com.anagram.analyzer.ui.viewmodel

import com.anagram.analyzer.domain.model.QuizDifficulty
import com.anagram.analyzer.domain.model.QuizQuestion

enum class QuizPhase { IDLE, LOADING, ANSWERING, CORRECT, INCORRECT }

data class QuizUiState(
    val phase: QuizPhase = QuizPhase.IDLE,
    val question: QuizQuestion? = null,
    val inputAnswer: String = "",
    val score: Int = 0,
    val streak: Int = 0,
    val bestStreak: Int = 0,
    val difficulty: QuizDifficulty = QuizDifficulty.EASY,
    val errorMessage: String? = null,
)
