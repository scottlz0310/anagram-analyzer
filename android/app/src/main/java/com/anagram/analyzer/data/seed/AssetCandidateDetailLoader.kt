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
    private var seedDetailsCache: Map<String, CandidateDetail>? = null

    override suspend fun loadDetails(): Map<String, CandidateDetail> {
        val seedDetails = loadSeedDetails()
        val cachedDetails = candidateDetailCacheDao.findAll().associate { it.word to it.toCandidateDetail() }
        return mergeCandidateDetails(
            seedDetails = seedDetails,
            cachedDetails = cachedDetails,
        )
    }

    override suspend fun fetchDetail(word: String): CandidateDetail? {
        val seedDetails = loadSeedDetails()
        val cachedDetail = candidateDetailCacheDao.findByWord(word)?.toCandidateDetail()
        val localDetail = resolveLocalCandidateDetail(
            word = word,
            seedDetails = seedDetails,
            cachedDetail = cachedDetail,
        )
        if (localDetail != null) {
            return localDetail
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

    private suspend fun loadSeedDetails(): Map<String, CandidateDetail> {
        seedDetailsCache?.let { return it }
        val seedDetails = try {
            context.assets.open(ASSET_FILE_NAME).bufferedReader().use { reader ->
                parseCandidateDetails(reader.lineSequence())
            }
        } catch (_: IOException) {
            emptyMap()
        }
        seedDetailsCache = seedDetails
        return seedDetails
    }

    private companion object {
        private const val ASSET_FILE_NAME = "candidate_detail_seed.tsv"
    }
}

internal fun mergeCandidateDetails(
    seedDetails: Map<String, CandidateDetail>,
    cachedDetails: Map<String, CandidateDetail>,
): Map<String, CandidateDetail> {
    return seedDetails + cachedDetails.filterKeys { key -> key !in seedDetails }
}

internal fun resolveLocalCandidateDetail(
    word: String,
    seedDetails: Map<String, CandidateDetail>,
    cachedDetail: CandidateDetail?,
): CandidateDetail? {
    return seedDetails[word] ?: cachedDetail
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
