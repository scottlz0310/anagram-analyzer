package com.anagram.analyzer.domain.usecase

import com.anagram.analyzer.data.seed.CandidateDetail
import com.anagram.analyzer.data.seed.CandidateDetailLoader
import javax.inject.Inject

class LoadCandidateDetailUseCase @Inject constructor(
    private val candidateDetailLoader: CandidateDetailLoader,
) {
    suspend fun execute(word: String): CandidateDetail? = candidateDetailLoader.fetchDetail(word)
}
