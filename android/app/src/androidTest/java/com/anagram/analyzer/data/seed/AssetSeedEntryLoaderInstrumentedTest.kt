package com.anagram.analyzer.data.seed

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.anagram.analyzer.data.db.ANAGRAM_DATABASE_VERSION
import com.anagram.analyzer.data.db.AnagramEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import kotlinx.coroutines.runBlocking
import java.io.File

@RunWith(AndroidJUnit4::class)
class AssetSeedEntryLoaderInstrumentedTest {
    @Test
    fun loadEntriesはdb未同梱時にtsvから読み込む() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val loader = AssetSeedEntryLoader(context)
        val dbEntries = loadSeedEntriesFromDatabaseAsset(context)

        assertNull(dbEntries)
        val entries = loader.loadEntries()

        assertTrue(entries.isNotEmpty())
    }

    @Test
    fun dbファイルからseedエントリを読み込める() {
        val dbFile = createTemporarySeedDb {
            version = ANAGRAM_DATABASE_VERSION
            execSQL("CREATE TABLE anagram_entries (sorted_key TEXT NOT NULL, word TEXT NOT NULL, length INTEGER NOT NULL)")
            execSQL("INSERT INTO anagram_entries (sorted_key, word, length) VALUES ('ごりん', 'りんご', 3)")
        }

        try {
            val entries = loadSeedEntriesFromDatabaseFile(dbFile.path)
            assertEquals(listOf(AnagramEntry(sortedKey = "ごりん", word = "りんご", length = 3)), entries)
        } finally {
            dbFile.delete()
        }
    }

    @Test
    fun dbバージョン不一致ならnullを返す() {
        val dbFile = createTemporarySeedDb {
            version = 2
            execSQL("CREATE TABLE anagram_entries (sorted_key TEXT NOT NULL, word TEXT NOT NULL, length INTEGER NOT NULL)")
            execSQL("INSERT INTO anagram_entries (sorted_key, word, length) VALUES ('ごりん', 'りんご', 3)")
        }

        try {
            val entries = loadSeedEntriesFromDatabaseFile(dbFile.path)
            assertNull(entries)
        } finally {
            dbFile.delete()
        }
    }

    @Test
    fun 必須列が不足しているdbはnullを返す() {
        val dbFile = createTemporarySeedDb {
            version = ANAGRAM_DATABASE_VERSION
            execSQL("CREATE TABLE anagram_entries (sorted_key TEXT NOT NULL, word TEXT NOT NULL)")
            execSQL("INSERT INTO anagram_entries (sorted_key, word) VALUES ('ごりん', 'りんご')")
        }

        try {
            val entries = loadSeedEntriesFromDatabaseFile(dbFile.path)
            assertNull(entries)
        } finally {
            dbFile.delete()
        }
    }

    @Test
    fun null列を含むdbはnullを返す() {
        val dbFile = createTemporarySeedDb {
            version = ANAGRAM_DATABASE_VERSION
            execSQL("CREATE TABLE anagram_entries (sorted_key TEXT, word TEXT, length INTEGER NOT NULL)")
            execSQL("INSERT INTO anagram_entries (sorted_key, word, length) VALUES (NULL, 'りんご', 3)")
        }

        try {
            val entries = loadSeedEntriesFromDatabaseFile(dbFile.path)
            assertNull(entries)
        } finally {
            dbFile.delete()
        }
    }

    private fun createTemporarySeedDb(configure: SQLiteDatabase.() -> Unit): File {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dbFile = File.createTempFile("seed_fixture", ".db", context.cacheDir)
        SQLiteDatabase.openOrCreateDatabase(dbFile, null).use { database ->
            database.configure()
        }
        return dbFile
    }
}
