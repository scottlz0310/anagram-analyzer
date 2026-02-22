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

    @Query("SELECT COUNT(*) FROM anagram_entries WHERE length BETWEEN :minLen AND :maxLen")
    suspend fun countByLength(minLen: Int, maxLen: Int): Int

    @Query("SELECT * FROM anagram_entries WHERE length BETWEEN :minLen AND :maxLen LIMIT 1 OFFSET :offset")
    suspend fun getEntryAtOffset(minLen: Int, maxLen: Int, offset: Int): AnagramEntry?

    @Query("SELECT COUNT(*) FROM anagram_entries WHERE length BETWEEN :minLen AND :maxLen AND is_common = 1")
    suspend fun countCommonByLength(minLen: Int, maxLen: Int): Int

    @Query("SELECT * FROM anagram_entries WHERE length BETWEEN :minLen AND :maxLen AND is_common = 1 LIMIT 1 OFFSET :offset")
    suspend fun getCommonEntryAtOffset(minLen: Int, maxLen: Int, offset: Int): AnagramEntry?
}
