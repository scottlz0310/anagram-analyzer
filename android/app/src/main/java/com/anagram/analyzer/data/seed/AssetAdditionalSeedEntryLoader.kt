package com.anagram.analyzer.data.seed

import android.content.Context
import com.anagram.analyzer.data.db.AnagramEntry
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import javax.inject.Inject

interface AdditionalSeedEntryLoader {
    suspend fun loadEntries(): List<AnagramEntry>
}

class AssetAdditionalSeedEntryLoader @Inject constructor(
    @ApplicationContext private val context: Context,
) : AdditionalSeedEntryLoader {
    override suspend fun loadEntries(): List<AnagramEntry> {
        return try {
            context.assets.open(ASSET_FILE_NAME).bufferedReader().use { reader ->
                parseSeedEntries(
                    lines = reader.lineSequence(),
                    fileName = ASSET_FILE_NAME,
                )
            }
        } catch (error: IOException) {
            throw IllegalStateException("追加辞書データの読み込みに失敗しました", error)
        }
    }

    private companion object {
        private const val ASSET_FILE_NAME = "anagram_additional_seed.tsv"
    }
}
