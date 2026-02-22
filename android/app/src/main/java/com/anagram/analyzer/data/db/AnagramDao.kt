package com.anagram.analyzer.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface AnagramDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(entries: List<AnagramEntry>)

    @Query("SELECT word FROM anagram_entries WHERE sorted_key = :sortedKey ORDER BY word")
    suspend fun lookupWords(sortedKey: String): List<String>

    @Query("SELECT COUNT(*) FROM anagram_entries")
    suspend fun count(): Long

    @Query("SELECT * FROM anagram_entries WHERE length BETWEEN :minLen AND :maxLen ORDER BY RANDOM() LIMIT 1")
    suspend fun getRandomEntry(minLen: Int, maxLen: Int): AnagramEntry?
}
