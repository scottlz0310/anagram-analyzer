package com.anagram.analyzer.domain.usecase

import com.anagram.analyzer.data.db.AnagramDao
import com.anagram.analyzer.domain.model.QuizQuestion
import javax.inject.Inject

class GenerateQuizUseCase @Inject constructor(
    private val anagramDao: AnagramDao,
    private val searchAnagramUseCase: SearchAnagramUseCase,
) {
    suspend fun execute(minLen: Int, maxLen: Int): QuizQuestion? {
        val entry = anagramDao.getRandomEntry(minLen, maxLen) ?: return null
        val correctWords = searchAnagramUseCase.execute(entry.sortedKey)
        if (correctWords.isEmpty()) return null
        return QuizQuestion(
            shuffledChars = shuffle(entry.sortedKey),
            sortedKey = entry.sortedKey,
            correctWords = correctWords,
        )
    }

    private fun shuffle(original: String, maxAttempts: Int = 10): String {
        if (original.length <= 1) return original
        repeat(maxAttempts) {
            val shuffled = original.toList().shuffled().joinToString("")
            if (shuffled != original) return shuffled
        }
        return original.toList().shuffled().joinToString("")
    }
}
