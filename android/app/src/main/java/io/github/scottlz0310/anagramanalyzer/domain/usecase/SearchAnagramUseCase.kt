package io.github.scottlz0310.anagramanalyzer.domain.usecase

import io.github.scottlz0310.anagramanalyzer.data.db.AnagramDao
import javax.inject.Inject

class SearchAnagramUseCase @Inject constructor(
    private val anagramDao: AnagramDao,
) {
    suspend fun execute(anagramKey: String): List<String> = anagramDao.lookupWords(anagramKey)
}
