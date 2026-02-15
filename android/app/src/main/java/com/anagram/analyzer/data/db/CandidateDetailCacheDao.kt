package com.anagram.analyzer.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface CandidateDetailCacheDao {
    @Query("SELECT * FROM candidate_detail_cache")
    suspend fun findAll(): List<CandidateDetailCacheEntry>

    @Query("SELECT * FROM candidate_detail_cache WHERE word = :word LIMIT 1")
    suspend fun findByWord(word: String): CandidateDetailCacheEntry?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: CandidateDetailCacheEntry)
}
