package io.github.scottlz0310.anagramanalyzer.domain.usecase

import io.github.scottlz0310.anagramanalyzer.data.seed.CandidateDetail
import io.github.scottlz0310.anagramanalyzer.data.seed.CandidateDetailLoader
import javax.inject.Inject

class LoadCandidateDetailUseCase @Inject constructor(
    private val candidateDetailLoader: CandidateDetailLoader,
) {
    suspend fun execute(word: String): CandidateDetail? = candidateDetailLoader.fetchDetail(word)
}
