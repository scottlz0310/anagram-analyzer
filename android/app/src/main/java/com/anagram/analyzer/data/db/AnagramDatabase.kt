package com.anagram.analyzer.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

internal const val ANAGRAM_DATABASE_VERSION = 4

@Database(
    entities = [AnagramEntry::class, CandidateDetailCacheEntry::class],
    version = ANAGRAM_DATABASE_VERSION,
    exportSchema = false,
)
abstract class AnagramDatabase : RoomDatabase() {
    abstract fun anagramDao(): AnagramDao
    abstract fun candidateDetailCacheDao(): CandidateDetailCacheDao

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
        private val migration2To3 =
            object : Migration(2, 3) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `candidate_detail_cache` (
                            `word` TEXT NOT NULL,
                            `kanji` TEXT NOT NULL,
                            `meaning` TEXT NOT NULL,
                            `updated_at` INTEGER NOT NULL,
                            PRIMARY KEY(`word`)
                        )
                        """.trimIndent(),
                    )
                    db.execSQL(
                        "CREATE INDEX IF NOT EXISTS `index_candidate_detail_cache_updated_at` ON `candidate_detail_cache` (`updated_at`)",
                    )
                }
            }

        private val migration3To4 =
            object : Migration(3, 4) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        "ALTER TABLE `anagram_entries` ADD COLUMN `is_common` INTEGER NOT NULL DEFAULT 0",
                    )
                }
            }

        fun getInstance(context: Context): AnagramDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AnagramDatabase::class.java,
                    "anagram.db",
                ).addMigrations(migration1To2, migration2To3, migration3To4)
                    .build()
                    .also { instance = it }
            }
        }
    }
}
