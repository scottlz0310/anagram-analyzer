package com.anagram.analyzer.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [AnagramEntry::class],
    version = 2,
    exportSchema = false,
)
abstract class AnagramDatabase : RoomDatabase() {
    abstract fun anagramDao(): AnagramDao

    companion object {
        @Volatile
        private var instance: AnagramDatabase? = null
        private val migration1To2 =
            object : Migration(1, 2) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        """
                        DELETE FROM anagram_entries
                        WHERE id NOT IN (
                            SELECT MIN(id)
                            FROM anagram_entries
                            GROUP BY sorted_key, word
                        )
                        """.trimIndent(),
                    )
                    db.execSQL(
                        "DROP INDEX IF EXISTS `index_anagram_entries_sorted_key_word`",
                    )
                    db.execSQL(
                        "CREATE UNIQUE INDEX `index_anagram_entries_sorted_key_word` ON `anagram_entries` (`sorted_key`, `word`)",
                    )
                }
            }

        fun getInstance(context: Context): AnagramDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AnagramDatabase::class.java,
                    "anagram.db",
                ).addMigrations(migration1To2)
                    .build()
                    .also { instance = it }
            }
        }
    }
}
