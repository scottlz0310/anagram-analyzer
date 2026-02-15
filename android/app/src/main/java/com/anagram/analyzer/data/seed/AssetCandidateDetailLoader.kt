package com.anagram.analyzer.data.seed

import android.content.Context
import com.anagram.analyzer.data.db.CandidateDetailCacheDao
import com.anagram.analyzer.data.db.CandidateDetailCacheEntry
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import javax.inject.Inject

data class CandidateDetail(
    val kanji: String,
    val meaning: String,
)

interface CandidateDetailLoader {
    suspend fun loadDetails(): Map<String, CandidateDetail>
    suspend fun fetchDetail(word: String): CandidateDetail?
}

class AssetCandidateDetailLoader @Inject constructor(
    @ApplicationContext private val context: Context,
    private val candidateDetailCacheDao: CandidateDetailCacheDao,
    private val candidateDetailRemoteDataSource: CandidateDetailRemoteDataSource,
) : CandidateDetailLoader {
    override suspend fun loadDetails(): Map<String, CandidateDetail> {
        val seedDetails = try {
            context.assets.open(ASSET_FILE_NAME).bufferedReader().use { reader ->
                parseCandidateDetails(reader.lineSequence())
            }
        } catch (_: IOException) {
            emptyMap()
        }
        val cachedDetails = candidateDetailCacheDao.findAll().associate { it.word to it.toCandidateDetail() }
        return seedDetails + cachedDetails
    }

    override suspend fun fetchDetail(word: String): CandidateDetail? {
        val cachedDetail = candidateDetailCacheDao.findByWord(word)?.toCandidateDetail()
        if (cachedDetail != null) {
            return cachedDetail
        }
        val remoteDetail = candidateDetailRemoteDataSource.fetchDetail(word) ?: return null
        candidateDetailCacheDao.upsert(
            CandidateDetailCacheEntry(
                word = word,
                kanji = remoteDetail.kanji,
                meaning = remoteDetail.meaning,
                updatedAt = System.currentTimeMillis(),
            ),
        )
        return remoteDetail
    }

    private companion object {
        private const val ASSET_FILE_NAME = "candidate_detail_seed.tsv"
    }
}

private fun CandidateDetailCacheEntry.toCandidateDetail(): CandidateDetail {
    return CandidateDetail(
        kanji = kanji,
        meaning = meaning,
    )
}

internal fun parseCandidateDetails(lines: Sequence<String>): Map<String, CandidateDetail> {
    val details = mutableMapOf<String, CandidateDetail>()
    lines.forEachIndexed { index, rawLine ->
        val line = rawLine.trimEnd('\r', '\n')
        if (line.isEmpty() || line.startsWith("#")) {
            return@forEachIndexed
        }

        val columns = line.split('\t', limit = 3)
        require(columns.size == 3) {
            "candidate_detail_seed.tsv ${index + 1}行目の列数が不正です: '$line'"
        }

        val word = columns[0].trim()
        val kanji = columns[1].trim()
        val meaning = columns[2].trim()

        require(word.isNotEmpty() && kanji.isNotEmpty() && meaning.isNotEmpty()) {
            "candidate_detail_seed.tsv ${index + 1}行目に空列があります: '$line'"
        }

        details[word] = CandidateDetail(
            kanji = kanji,
            meaning = meaning,
        )
    }
    return details
}
