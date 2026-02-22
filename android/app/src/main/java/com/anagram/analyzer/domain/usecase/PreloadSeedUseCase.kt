package com.anagram.analyzer.domain.usecase

import com.anagram.analyzer.data.db.AnagramDao
import com.anagram.analyzer.data.db.AnagramEntry
import com.anagram.analyzer.data.seed.CandidateDetail
import com.anagram.analyzer.data.seed.CandidateDetailLoader
import com.anagram.analyzer.data.seed.SeedEntryLoader
import com.anagram.analyzer.domain.model.PreloadLogger
import javax.inject.Inject
import kotlin.time.TimeSource

class PreloadSeedUseCase @Inject constructor(
    private val anagramDao: AnagramDao,
    private val seedEntryLoader: SeedEntryLoader,
    private val candidateDetailLoader: CandidateDetailLoader,
    private val preloadLogger: PreloadLogger,
) {
    data class PreloadResult(
        val preloadLog: String,
        val candidateDetails: Map<String, CandidateDetail>,
    )

    suspend fun execute(): PreloadResult {
        val metrics = preloadSeedDataIfNeeded()
        val candidateDetails = candidateDetailLoader.loadDetails()
        val logLine = metrics.toLogLine()
        preloadLogger.log(logLine)
        return PreloadResult(preloadLog = logLine, candidateDetails = candidateDetails)
    }

    private suspend fun preloadSeedDataIfNeeded(): PreloadMetrics {
        val started = TimeSource.Monotonic.markNow()
        val beforeCount = anagramDao.count()
        if (beforeCount > 0) {
            return PreloadMetrics(
                source = "existing_db",
                totalEntries = beforeCount,
                insertedEntries = 0,
                elapsedMillis = started.elapsedNow().inWholeMilliseconds,
            )
        }

        val seedEntries = seedEntryLoader.loadEntries()
        if (seedEntries.isNotEmpty()) {
            anagramDao.insertAll(seedEntries)
            val afterCount = anagramDao.count()
            return PreloadMetrics(
                source = "seed_asset",
                totalEntries = afterCount,
                insertedEntries = (afterCount - beforeCount).coerceAtLeast(0),
                elapsedMillis = started.elapsedNow().inWholeMilliseconds,
            )
        }

        anagramDao.insertAll(DEMO_ENTRIES)
        val afterCount = anagramDao.count()
        return PreloadMetrics(
            source = "demo_fallback",
            totalEntries = afterCount,
            insertedEntries = (afterCount - beforeCount).coerceAtLeast(0),
            elapsedMillis = started.elapsedNow().inWholeMilliseconds,
        )
    }

    private data class PreloadMetrics(
        val source: String,
        val totalEntries: Long,
        val insertedEntries: Long,
        val elapsedMillis: Long,
    )

    private fun PreloadMetrics.toLogLine(): String =
        "preload source=$source total=$totalEntries inserted=$insertedEntries elapsedMs=$elapsedMillis"

    private companion object {
        private val DEMO_ENTRIES = listOf(
            AnagramEntry(sortedKey = "ごりん", word = "りんご", length = 3),
            AnagramEntry(sortedKey = "くさら", word = "さくら", length = 3),
            AnagramEntry(sortedKey = "あいう", word = "あいう", length = 3),
        )
    }
}
