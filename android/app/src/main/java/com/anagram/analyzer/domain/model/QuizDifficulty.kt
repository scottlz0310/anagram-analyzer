package com.anagram.analyzer.domain.model

enum class QuizDifficulty(val minLen: Int, val maxLen: Int, val label: String) {
    EASY(3, 5, "かんたん"),
    NORMAL(5, 8, "ふつう"),
    HARD(7, 12, "むずかしい"),
}
