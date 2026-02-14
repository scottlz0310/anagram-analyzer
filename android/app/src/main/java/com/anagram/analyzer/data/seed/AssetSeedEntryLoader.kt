package com.anagram.analyzer.data.seed

import android.content.Context
import com.anagram.analyzer.data.db.AnagramEntry
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import javax.inject.Inject

interface SeedEntryLoader {
    suspend fun loadEntries(): List<AnagramEntry>
}

class AssetSeedEntryLoader @Inject constructor(
    @ApplicationContext private val context: Context,
) : SeedEntryLoader {
    override suspend fun loadEntries(): List<AnagramEntry> {
        val lines = try {
            context.assets.open(ASSET_FILE_NAME).bufferedReader().use { it.readLines() }
        } catch (_: IOException) {
            return emptyList()
        }

        return parseSeedEntries(lines.asSequence())
    }

    private companion object {
        private const val ASSET_FILE_NAME = "anagram_seed.tsv"
    }
}

internal fun parseSeedEntries(lines: Sequence<String>): List<AnagramEntry> {
    val entries = mutableListOf<AnagramEntry>()
    lines.forEachIndexed { index, rawLine ->
        val line = rawLine.trim()
        if (line.isEmpty() || line.startsWith("#")) {
            return@forEachIndexed
        }

        val columns = line.split('\t')
        require(columns.size == 3) {
            "anagram_seed.tsv ${index + 1}行目の列数が不正です: '$line'"
        }

        val sortedKey = columns[0].trim()
        val word = columns[1].trim()
        val length = columns[2].trim().toIntOrNull()
            ?: throw IllegalArgumentException("anagram_seed.tsv ${index + 1}行目のlengthが不正です: '$line'")

        require(sortedKey.isNotEmpty() && word.isNotEmpty()) {
            "anagram_seed.tsv ${index + 1}行目に空列があります: '$line'"
        }

        entries.add(
            AnagramEntry(
                sortedKey = sortedKey,
                word = word,
                length = length,
            ),
        )
    }
    return entries
}
