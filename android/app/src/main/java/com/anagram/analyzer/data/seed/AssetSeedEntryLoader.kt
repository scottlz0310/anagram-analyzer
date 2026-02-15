package com.anagram.analyzer.data.seed

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import com.anagram.analyzer.data.db.ANAGRAM_DATABASE_VERSION
import com.anagram.analyzer.data.db.AnagramEntry
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.IOException
import javax.inject.Inject

interface SeedEntryLoader {
    suspend fun loadEntries(): List<AnagramEntry>
}

class AssetSeedEntryLoader @Inject constructor(
    @ApplicationContext private val context: Context,
) : SeedEntryLoader {
    override suspend fun loadEntries(): List<AnagramEntry> {
        val dbEntries = loadSeedEntriesFromDatabaseAsset(context)
        if (!dbEntries.isNullOrEmpty()) {
            return dbEntries
        }
        val tsvEntries = try {
            context.assets.open(ASSET_FILE_NAME).bufferedReader().use { reader ->
                parseSeedEntries(reader.lineSequence())
            }
        } catch (_: IOException) {
            emptyList()
        }
        return resolveSeedEntries(
            dbEntries = dbEntries,
            tsvEntries = tsvEntries,
        )
    }
}

internal fun resolveSeedEntries(
    dbEntries: List<AnagramEntry>?,
    tsvEntries: List<AnagramEntry>,
): List<AnagramEntry> {
    return dbEntries?.takeIf { it.isNotEmpty() } ?: tsvEntries
}

internal fun loadSeedEntriesFromDatabaseAsset(context: Context): List<AnagramEntry>? {
    if (!hasAsset(context, DB_ASSET_FILE_NAME)) {
        return null
    }

    var tempFile: File? = null
    return try {
        val workingFile = File.createTempFile("anagram_seed", ".db", context.cacheDir)
        tempFile = workingFile
        context.assets.open(DB_ASSET_FILE_NAME).use { input ->
            workingFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        loadSeedEntriesFromDatabaseFile(workingFile.path)
    } catch (_: IOException) {
        null
    } finally {
        tempFile?.delete()
    }
}

internal fun loadSeedEntriesFromDatabaseFile(path: String): List<AnagramEntry>? {
    return try {
        SQLiteDatabase.openDatabase(path, null, SQLiteDatabase.OPEN_READONLY).use { database ->
            if (database.version != ANAGRAM_DATABASE_VERSION) {
                return null
            }
            database.query(
                DB_TABLE_NAME,
                DB_COLUMNS,
                null,
                null,
                null,
                null,
                null,
            ).use { cursor ->
                val sortedKeyColumn = cursor.getColumnIndexOrThrow(DB_COLUMNS[0])
                val wordColumn = cursor.getColumnIndexOrThrow(DB_COLUMNS[1])
                val lengthColumn = cursor.getColumnIndexOrThrow(DB_COLUMNS[2])
                val entries = ArrayList<AnagramEntry>(cursor.count)
                while (cursor.moveToNext()) {
                    val sortedKey = cursor.getString(sortedKeyColumn) ?: return null
                    val word = cursor.getString(wordColumn) ?: return null
                    entries.add(
                        AnagramEntry(
                            sortedKey = sortedKey,
                            word = word,
                            length = cursor.getInt(lengthColumn),
                        ),
                    )
                }
                entries
            }
        }
    } catch (_: SQLiteException) {
        null
    } catch (_: IllegalArgumentException) {
        null
    }
}

private fun hasAsset(context: Context, fileName: String): Boolean {
    return try {
        context.assets.open(fileName).use { }
        true
    } catch (_: IOException) {
        false
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
private const val DB_ASSET_FILE_NAME = "anagram_seed.db"
private const val DB_TABLE_NAME = "anagram_entries"
private val DB_COLUMNS = arrayOf("sorted_key", "word", "length")
