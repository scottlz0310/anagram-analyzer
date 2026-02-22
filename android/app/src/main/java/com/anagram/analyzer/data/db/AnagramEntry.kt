package com.anagram.analyzer.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "anagram_entries",
    indices = [
        Index(value = ["sorted_key", "word"], unique = true),
        Index(value = ["sorted_key"]),
        Index(value = ["length"]),
        Index(value = ["length", "is_common"]),
    ],
)
data class AnagramEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "sorted_key")
    val sortedKey: String,
    @ColumnInfo(name = "word")
    val word: String,
    @ColumnInfo(name = "length")
    val length: Int,
    @ColumnInfo(name = "is_common", defaultValue = "0")
    val isCommon: Boolean = false,
)
