package io.github.scottlz0310.anagramanalyzer.ui.viewmodel

import io.github.scottlz0310.anagramanalyzer.domain.model.CharCard
import io.github.scottlz0310.anagramanalyzer.domain.model.QuizDifficulty
import io.github.scottlz0310.anagramanalyzer.domain.model.QuizQuestion

enum class QuizPhase { IDLE, LOADING, ANSWERING, CORRECT, INCORRECT }

data class QuizUiState(
    val phase: QuizPhase = QuizPhase.IDLE,
    val question: QuizQuestion? = null,
    val shuffledCards: List<CharCard> = emptyList(),
    val answerSlots: List<Int?> = emptyList(),
    val selectedCardId: Int? = null,
    val score: Int = 0,
    val streak: Int = 0,
    val bestStreak: Int = 0,
    val difficulty: QuizDifficulty = QuizDifficulty.EASY,
    val errorMessage: String? = null,
)
