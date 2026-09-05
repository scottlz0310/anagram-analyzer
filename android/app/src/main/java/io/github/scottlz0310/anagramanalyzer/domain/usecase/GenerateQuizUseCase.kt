package io.github.scottlz0310.anagramanalyzer.domain.usecase

import io.github.scottlz0310.anagramanalyzer.data.db.AnagramDao
import io.github.scottlz0310.anagramanalyzer.data.db.AnagramEntry
import io.github.scottlz0310.anagramanalyzer.domain.model.CharCard
import io.github.scottlz0310.anagramanalyzer.domain.model.QuizQuestion
import javax.inject.Inject
import kotlin.random.Random

class GenerateQuizUseCase @Inject constructor(
    private val anagramDao: AnagramDao,
    private val searchAnagramUseCase: SearchAnagramUseCase,
) {
    suspend fun execute(minLen: Int, maxLen: Int): QuizQuestion? {
        val commonCount = anagramDao.countCommonByLength(minLen, maxLen)
        val useCommon = commonCount > 0
        val count = if (useCommon) commonCount else anagramDao.countByLength(minLen, maxLen)
        if (count == 0) return null

        for (offset in buildCandidateOffsets(count)) {
            val entry = if (useCommon) {
                anagramDao.getCommonEntryAtOffset(minLen, maxLen, offset)
            } else {
                anagramDao.getEntryAtOffset(minLen, maxLen, offset)
            } ?: continue

            val question = buildQuestion(entry)
            if (question != null) return question
        }

        return null
    }

    private suspend fun buildQuestion(entry: AnagramEntry): QuizQuestion? {
        val correctWords = searchAnagramUseCase.execute(entry.sortedKey)
        if (correctWords.isEmpty()) return null

        val prompt = buildPrompt(entry.sortedKey, correctWords) ?: return null
        return QuizQuestion(
            shuffledCards = prompt.mapIndexed { index, char ->
                CharCard(id = index, char = char, isPlaced = false)
            },
            sortedKey = entry.sortedKey,
            correctWords = correctWords,
        )
    }

    private fun buildCandidateOffsets(count: Int): List<Int> {
        if (count <= EXHAUSTIVE_ENTRY_THRESHOLD) {
            return (0 until count).shuffled()
        }

        val offsets = linkedSetOf<Int>()
        while (offsets.size < MAX_ENTRY_ATTEMPTS) {
            offsets += Random.nextInt(count)
        }
        return offsets.toList()
    }

    private fun buildPrompt(sortedKey: String, correctWords: List<String>): String? {
        if (sortedKey.length <= 1) return null

        val forbiddenPrompts = correctWords.toHashSet().apply {
            add(sortedKey)
        }

        repeat(MAX_SHUFFLE_ATTEMPTS) {
            val shuffled = sortedKey.toList().shuffled().joinToString("")
            if (shuffled !in forbiddenPrompts) return shuffled
        }

        return findPromptByPrefixSearch(sortedKey, forbiddenPrompts)
    }

    private fun findPromptByPrefixSearch(
        sortedKey: String,
        forbiddenPrompts: Set<String>,
    ): String? {
        val remainingCounts = sortedKey.groupingBy { it }.eachCount().toMutableMap()
        val charOrder = remainingCounts.keys.sorted()
        val forbiddenPrefixes = buildForbiddenPrefixes(forbiddenPrompts)
        return searchPrompt(
            prefix = StringBuilder(),
            targetLength = sortedKey.length,
            remainingCounts = remainingCounts,
            charOrder = charOrder,
            forbiddenPrefixes = forbiddenPrefixes,
            forbiddenPrompts = forbiddenPrompts,
        )
    }

    private fun searchPrompt(
        prefix: StringBuilder,
        targetLength: Int,
        remainingCounts: MutableMap<Char, Int>,
        charOrder: List<Char>,
        forbiddenPrefixes: Set<String>,
        forbiddenPrompts: Set<String>,
    ): String? {
        if (prefix.length == targetLength) {
            val candidate = prefix.toString()
            return candidate.takeIf { it !in forbiddenPrompts }
        }

        for (char in charOrder) {
            val remaining = remainingCounts[char] ?: 0
            if (remaining == 0) continue

            remainingCounts[char] = remaining - 1
            prefix.append(char)

            val nextPrefix = prefix.toString()
            val candidate = if (prefix.length < targetLength && nextPrefix !in forbiddenPrefixes) {
                nextPrefix + buildRemainingChars(remainingCounts, charOrder)
            } else {
                searchPrompt(
                    prefix = prefix,
                    targetLength = targetLength,
                    remainingCounts = remainingCounts,
                    charOrder = charOrder,
                    forbiddenPrefixes = forbiddenPrefixes,
                    forbiddenPrompts = forbiddenPrompts,
                )
            }

            if (candidate != null && candidate !in forbiddenPrompts) {
                return candidate
            }

            prefix.deleteCharAt(prefix.lastIndex)
            remainingCounts[char] = remaining
        }

        return null
    }

    private fun buildForbiddenPrefixes(forbiddenPrompts: Set<String>): Set<String> = buildSet {
        forbiddenPrompts.forEach { word ->
            for (index in 1 until word.length) {
                add(word.substring(0, index))
            }
        }
    }

    private fun buildRemainingChars(
        remainingCounts: Map<Char, Int>,
        charOrder: List<Char>,
    ): String = buildString {
        charOrder.forEach { char ->
            repeat(remainingCounts[char] ?: 0) {
                append(char)
            }
        }
    }

    private companion object {
        private const val EXHAUSTIVE_ENTRY_THRESHOLD = 128
        private const val MAX_ENTRY_ATTEMPTS = 32
        private const val MAX_SHUFFLE_ATTEMPTS = 24
    }
}
