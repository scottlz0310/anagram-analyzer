package com.anagram.analyzer.domain.usecase

import com.anagram.analyzer.data.db.AnagramDao
import com.anagram.analyzer.data.db.AnagramEntry
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GenerateQuizUseCaseTest {
    @Test
    fun 正解そのものの並びでは出題しない() = runTest {
        val dao = FakeGenerateQuizAnagramDao(
            entries = listOf(
                AnagramEntry(sortedKey = "ごりん", word = "りんご", length = 3),
            ),
            wordsBySortedKey = mapOf(
                "ごりん" to listOf("りんご", "ごりん"),
            ),
        )
        val useCase = GenerateQuizUseCase(
            anagramDao = dao,
            searchAnagramUseCase = SearchAnagramUseCase(dao),
        )

        val question = useCase.execute(minLen = 3, maxLen = 3)

        assertNotNull(question)
        val prompt = question!!.shuffledCards.joinToString(separator = "") { it.char.toString() }
        assertTrue(prompt !in question.correctWords)
        assertTrue(prompt != question.sortedKey)
    }

    @Test
    fun 回避不能な問題は別のエントリへ切り替える() = runTest {
        val dao = FakeGenerateQuizAnagramDao(
            entries = listOf(
                AnagramEntry(sortedKey = "あい", word = "あい", length = 2),
                AnagramEntry(sortedKey = "ごりん", word = "りんご", length = 3),
            ),
            wordsBySortedKey = mapOf(
                "あい" to listOf("あい", "いあ"),
                "ごりん" to listOf("りんご"),
            ),
        )
        val useCase = GenerateQuizUseCase(
            anagramDao = dao,
            searchAnagramUseCase = SearchAnagramUseCase(dao),
        )

        val question = useCase.execute(minLen = 2, maxLen = 3)

        assertNotNull(question)
        assertEquals("ごりん", question!!.sortedKey)
    }

    @Test
    fun 全て回避不能ならnullを返す() = runTest {
        val dao = FakeGenerateQuizAnagramDao(
            entries = listOf(
                AnagramEntry(sortedKey = "あい", word = "あい", length = 2),
            ),
            wordsBySortedKey = mapOf(
                "あい" to listOf("あい", "いあ"),
            ),
        )
        val useCase = GenerateQuizUseCase(
            anagramDao = dao,
            searchAnagramUseCase = SearchAnagramUseCase(dao),
        )

        val question = useCase.execute(minLen = 2, maxLen = 2)

        assertNull(question)
    }

    private class FakeGenerateQuizAnagramDao(
        private val entries: List<AnagramEntry>,
        private val wordsBySortedKey: Map<String, List<String>>,
    ) : AnagramDao {
        override suspend fun insertAll(entries: List<AnagramEntry>) = Unit

        override suspend fun lookupWords(sortedKey: String): List<String> =
            wordsBySortedKey[sortedKey].orEmpty()

        override suspend fun count(): Long = entries.size.toLong()

        override suspend fun countByLength(minLen: Int, maxLen: Int): Int =
            filterEntries(minLen, maxLen, commonOnly = false).size

        override suspend fun getEntryAtOffset(minLen: Int, maxLen: Int, offset: Int): AnagramEntry? =
            filterEntries(minLen, maxLen, commonOnly = false).getOrNull(offset)

        override suspend fun countCommonByLength(minLen: Int, maxLen: Int): Int =
            filterEntries(minLen, maxLen, commonOnly = true).size

        override suspend fun getCommonEntryAtOffset(
            minLen: Int,
            maxLen: Int,
            offset: Int,
        ): AnagramEntry? = filterEntries(minLen, maxLen, commonOnly = true).getOrNull(offset)

        private fun filterEntries(
            minLen: Int,
            maxLen: Int,
            commonOnly: Boolean,
        ): List<AnagramEntry> = entries.filter { entry ->
            entry.length in minLen..maxLen && (!commonOnly || entry.isCommon)
        }
    }
}
