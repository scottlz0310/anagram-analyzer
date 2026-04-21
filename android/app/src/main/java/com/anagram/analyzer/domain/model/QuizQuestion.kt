package com.anagram.analyzer.domain.model

data class QuizQuestion(
    val shuffledCards: List<CharCard>,
    val sortedKey: String,
    val correctWords: List<String>,
)
