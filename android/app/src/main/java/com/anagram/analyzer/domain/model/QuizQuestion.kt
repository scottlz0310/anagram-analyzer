package com.anagram.analyzer.domain.model

data class QuizQuestion(
    val shuffledChars: String,
    val sortedKey: String,
    val correctWords: List<String>,
)
