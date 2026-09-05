package io.github.scottlz0310.anagramanalyzer.domain.model

data class QuizQuestion(
    val shuffledCards: List<CharCard>,
    val sortedKey: String,
    val correctWords: List<String>,
)
