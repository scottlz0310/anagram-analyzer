package com.anagram.analyzer.data.seed

import com.anagram.analyzer.data.db.AnagramEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AssetSeedEntryLoaderTest {
    @Test
    fun tsvを正しくparseできる() {
        val entries = parseSeedEntries(
            sequenceOf(
                "# comment",
                "",
                "ごりん\tりんご\t3",
                "くさら\tさくら\t3",
            ),
        )

        assertEquals(2, entries.size)
        assertEquals("ごりん", entries[0].sortedKey)
        assertEquals("りんご", entries[0].word)
        assertEquals(3, entries[0].length)
    }

    @Test(expected = IllegalArgumentException::class)
    fun 列数不正は例外を送出する() {
        parseSeedEntries(sequenceOf("ごりん\tりんご"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun length不正は例外を送出する() {
        parseSeedEntries(sequenceOf("ごりん\tりんご\tnan"))
    }

    @Test
    fun 末尾空列は空列エラーを送出する() {
        val exception = try {
            parseSeedEntries(sequenceOf("ごりん\tりんご\t"))
            null
        } catch (e: IllegalArgumentException) {
            e
        }
        requireNotNull(exception)
        assertTrue(exception.message?.contains("空列") == true)
    }

    @Test
    fun ファイル名指定時は例外メッセージへ反映する() {
        val exception = try {
            parseSeedEntries(
                lines = sequenceOf("ごりん\tりんご"),
                fileName = "anagram_additional_seed.tsv",
            )
            null
        } catch (e: IllegalArgumentException) {
            e
        }
        requireNotNull(exception)
        assertTrue(exception.message?.contains("anagram_additional_seed.tsv") == true)
    }

    @Test
    fun db由来のseedがある場合はtsvより優先する() {
        val dbEntries = listOf(AnagramEntry(sortedKey = "ごりん", word = "りんご", length = 3))
        val tsvEntries = listOf(AnagramEntry(sortedKey = "くさら", word = "さくら", length = 3))

        val resolved = resolveSeedEntries(
            dbEntries = dbEntries,
            tsvEntries = tsvEntries,
        )

        assertEquals(dbEntries, resolved)
    }

    @Test
    fun db由来seedが空ならtsvを使う() {
        val tsvEntries = listOf(AnagramEntry(sortedKey = "くさら", word = "さくら", length = 3))

        val resolved = resolveSeedEntries(
            dbEntries = emptyList(),
            tsvEntries = tsvEntries,
        )

        assertEquals(tsvEntries, resolved)
    }

    @Test
    fun db由来seedがnullならtsvを使う() {
        val tsvEntries = listOf(AnagramEntry(sortedKey = "くさら", word = "さくら", length = 3))

        val resolved = resolveSeedEntries(
            dbEntries = null,
            tsvEntries = tsvEntries,
        )

        assertEquals(tsvEntries, resolved)
    }
}
