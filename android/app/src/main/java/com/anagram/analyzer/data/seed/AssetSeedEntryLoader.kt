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
        return try {
            context.assets.open(ASSET_FILE_NAME).bufferedReader().use { reader ->
                parseSeedEntries(reader.lineSequence())
            }
        } catch (_: IOException) {
            emptyList()
        }
    }
}

internal fun parseSeedEntries(lines: Sequence<String>): List<AnagramEntry> {
    return parseSeedEntries(lines, ASSET_FILE_NAME)
}

internal fun parseSeedEntries(
    lines: Sequence<String>,
    fileName: String,
): List<AnagramEntry> {
    val entries = mutableListOf<AnagramEntry>()
    lines.forEachIndexed { index, rawLine ->
        val line = rawLine.trimEnd('\r', '\n')
        if (line.isEmpty() || line.startsWith("#")) {
            return@forEachIndexed
        }

        val columns = line.split('\t', limit = 3)
        require(columns.size == 3) {
            "$fileName ${index + 1}行目の列数が不正です: '$line'"
        }

        val sortedKey = columns[0].trim()
        val word = columns[1].trim()
        val lengthText = columns[2].trim()

        require(sortedKey.isNotEmpty() && word.isNotEmpty() && lengthText.isNotEmpty()) {
            "$fileName ${index + 1}行目に空列があります: '$line'"
        }

        val length = lengthText.toIntOrNull()
            ?: throw IllegalArgumentException("$fileName ${index + 1}行目のlengthが不正です: '$line'")

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

private const val ASSET_FILE_NAME = "anagram_seed.tsv"
