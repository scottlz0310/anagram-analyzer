package com.anagram.analyzer.domain.usecase

import com.anagram.analyzer.data.db.AnagramDao
import javax.inject.Inject

class SearchAnagramUseCase @Inject constructor(
    private val anagramDao: AnagramDao,
) {
    suspend fun execute(anagramKey: String): List<String> = anagramDao.lookupWords(anagramKey)
}
