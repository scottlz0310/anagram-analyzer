package com.anagram.analyzer.data.seed

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AssetCandidateDetailLoaderTest {
    @Test
    fun tsvを正しくparseできる() {
        val details = parseCandidateDetails(
            sequenceOf(
                "# comment",
                "",
                "りんご\t林檎\tapple",
                "さくら\t桜\tcherry blossom",
            ),
        )

        assertEquals(2, details.size)
        assertEquals("林檎", details["りんご"]?.kanji)
        assertEquals("apple", details["りんご"]?.meaning)
    }

    @Test(expected = IllegalArgumentException::class)
    fun 列数不正は例外を送出する() {
        parseCandidateDetails(sequenceOf("りんご\t林檎"))
    }

    @Test
    fun 空列は例外を送出する() {
        val exception = try {
            parseCandidateDetails(sequenceOf("りんご\t\tapple"))
            null
        } catch (e: IllegalArgumentException) {
            e
        }
        requireNotNull(exception)
        assertTrue(exception.message?.contains("空列") == true)
    }

    @Test
    fun seedとcacheのマージはseedを優先する() {
        val seedDetails = mapOf(
            "りんご" to CandidateDetail(kanji = "林檎", meaning = "apple"),
        )
        val cachedDetails = mapOf(
            "りんご" to CandidateDetail(kanji = "リンゴ", meaning = "old apple"),
            "おなじ" to CandidateDetail(kanji = "同じ", meaning = "same"),
        )

        val merged = mergeCandidateDetails(
            seedDetails = seedDetails,
            cachedDetails = cachedDetails,
        )

        assertEquals("林檎", merged["りんご"]?.kanji)
        assertEquals("same", merged["おなじ"]?.meaning)
    }

    @Test
    fun ローカル詳細解決はseedをcacheより優先する() {
        val seedDetails = mapOf(
            "りんご" to CandidateDetail(kanji = "林檎", meaning = "apple"),
        )
        val cachedDetail = CandidateDetail(kanji = "リンゴ", meaning = "old apple")

        val resolved = resolveLocalCandidateDetail(
            word = "りんご",
            seedDetails = seedDetails,
            cachedDetail = cachedDetail,
        )

        assertEquals("林檎", resolved?.kanji)
        assertEquals("apple", resolved?.meaning)
    }
}
