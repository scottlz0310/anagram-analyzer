package io.github.scottlz0310.anagramanalyzer.domain.usecase

import io.github.scottlz0310.anagramanalyzer.data.db.AnagramDao
import io.github.scottlz0310.anagramanalyzer.data.seed.AdditionalSeedEntryLoader
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
