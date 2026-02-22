package com.anagram.tools.seedgenerator

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.nio.file.Paths

class SeedGeneratorIntegrationTest {
    @get:Rule
    val tmp = TemporaryFolder()

    private val fixtureXml by lazy {
        Paths.get(javaClass.classLoader.getResource("jmdict_sample.xml")!!.toURI())
    }

    private val expectedTsv by lazy {
        javaClass.classLoader.getResource("expected_anagram_seed.tsv")!!.readText(Charsets.UTF_8)
    }

    @Test
    fun `fixture XMLから正しい行数を解析できる`() {
        val rows = JmdictParser.parse(fixtureXml, minLen = 2, maxLen = 8)
        // いぬ / こね / ごりん / ねこ / りんご の5件
        assertEquals(5, rows.size)
    }

    @Test
    fun `TSVゴールデン比較（ソート済み）`() {
        val outTsv = tmp.newFile("out.tsv").toPath()
        val rows = JmdictParser.parse(fixtureXml, minLen = 2, maxLen = 8)
        TsvExporter.export(rows, outTsv)
        val actual = outTsv.toFile().readText(Charsets.UTF_8)
        assertEquals(expectedTsv, actual)
    }

    @Test
    fun `DBクエリ検証`() {
        val outDb = tmp.newFile("out.db").toPath()
        val rows = JmdictParser.parse(fixtureXml, minLen = 2, maxLen = 8)
        DbExporter.export(rows, outDb, force = true)

        Class.forName("org.sqlite.JDBC")
        java.sql.DriverManager.getConnection("jdbc:sqlite:${outDb.toAbsolutePath()}").use { conn ->
            // user_version = 3
            conn.createStatement().use { st ->
                st.executeQuery("PRAGMA user_version").use { rs ->
                    assertEquals(3, rs.getInt(1))
                }
            }
            // anagram_entries テーブル: 5件
            conn.createStatement().use { st ->
                st.executeQuery("SELECT COUNT(*) FROM anagram_entries").use { rs ->
                    assertEquals(5, rs.getInt(1))
                }
            }
            // candidate_detail_cache テーブル存在（空でも可）
            conn.createStatement().use { st ->
                st.executeQuery("SELECT COUNT(*) FROM candidate_detail_cache").use { rs ->
                    assertEquals(0, rs.getInt(1))
                }
            }
            // 一意インデックス存在確認
            conn.createStatement().use { st ->
                st.executeQuery(
                    "SELECT name FROM sqlite_master" +
                        " WHERE type='index' AND name='index_anagram_entries_sorted_key_word'",
                ).use { rs ->
                    assertTrue(rs.next())
                }
            }
            // 代表行: ねこ の sorted_key = こね
            conn.prepareStatement(
                "SELECT sorted_key FROM anagram_entries WHERE word = ?",
            ).use { ps ->
                ps.setString(1, "ねこ")
                ps.executeQuery().use { rs ->
                    assertTrue(rs.next())
                    assertEquals("こね", rs.getString(1))
                }
            }
        }
    }
}
