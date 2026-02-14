package com.anagram.analyzer.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [AnagramEntry::class],
    version = 1,
    exportSchema = false,
)
abstract class AnagramDatabase : RoomDatabase() {
    abstract fun anagramDao(): AnagramDao

    companion object {
        @Volatile
        private var instance: AnagramDatabase? = null

        fun getInstance(context: Context): AnagramDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AnagramDatabase::class.java,
                    "anagram.db",
                ).build().also { instance = it }
            }
        }
    }
}
