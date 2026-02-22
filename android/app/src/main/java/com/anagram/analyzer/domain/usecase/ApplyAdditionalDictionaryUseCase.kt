package com.anagram.analyzer.domain.usecase

import com.anagram.analyzer.data.db.AnagramDao
import com.anagram.analyzer.data.seed.AdditionalSeedEntryLoader
import javax.inject.Inject

class ApplyAdditionalDictionaryUseCase @Inject constructor(
    private val anagramDao: AnagramDao,
    private val additionalSeedEntryLoader: AdditionalSeedEntryLoader,
) {
    suspend fun execute(): Pair<Long, Int> {
        val additionalEntries = additionalSeedEntryLoader.loadEntries()
        require(additionalEntries.isNotEmpty()) { "追加辞書データが空です" }
        val beforeCount = anagramDao.count()
        anagramDao.insertAll(additionalEntries)
        val afterCount = anagramDao.count()
        return Pair((afterCount - beforeCount).coerceAtLeast(0), additionalEntries.size)
    }
}
