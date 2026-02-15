package com.anagram.analyzer.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "candidate_detail_cache",
    indices = [
        Index(value = ["updated_at"]),
    ],
)
data class CandidateDetailCacheEntry(
    @PrimaryKey
    @ColumnInfo(name = "word")
    val word: String,
    @ColumnInfo(name = "kanji")
    val kanji: String,
    @ColumnInfo(name = "meaning")
    val meaning: String,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,
)
