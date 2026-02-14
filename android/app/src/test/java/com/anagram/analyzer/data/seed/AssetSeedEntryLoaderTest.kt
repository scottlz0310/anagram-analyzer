package com.anagram.analyzer.data.seed

import org.junit.Assert.assertEquals
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
}
