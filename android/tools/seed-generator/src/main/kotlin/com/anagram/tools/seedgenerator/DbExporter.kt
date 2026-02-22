package com.anagram.tools.seedgenerator

import java.nio.file.Path
import java.sql.DriverManager

object DbExporter {
    private const val USER_VERSION = 5

    fun export(rows: List<AnagramRow>, outPath: Path, force: Boolean = false) {
        if (outPath.toFile().exists()) {
            check(force) { "出力先が既に存在します: $outPath（上書きする場合は --force を指定）" }
            outPath.toFile().delete()
        }
        outPath.parent?.toFile()?.mkdirs()

        Class.forName("org.sqlite.JDBC")
        DriverManager.getConnection("jdbc:sqlite:${outPath.toAbsolutePath()}").use { conn ->
            conn.autoCommit = false
            conn.createStatement().use { st ->
                st.execute(
                    """
                    CREATE TABLE IF NOT EXISTS `anagram_entries` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `sorted_key` TEXT NOT NULL,
                        `word` TEXT NOT NULL,
                        `length` INTEGER NOT NULL,
                        `is_common` INTEGER NOT NULL DEFAULT 0
                    )
                    """.trimIndent(),
                )
                st.execute(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_anagram_entries_sorted_key_word`" +
                        " ON `anagram_entries` (`sorted_key`, `word`)",
                )
                st.execute(
                    "CREATE INDEX IF NOT EXISTS `index_anagram_entries_sorted_key`" +
                        " ON `anagram_entries` (`sorted_key`)",
                )
                st.execute(
                    "CREATE INDEX IF NOT EXISTS `index_anagram_entries_length`" +
                        " ON `anagram_entries` (`length`)",
                )
                st.execute(
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
                st.execute(
                    "CREATE INDEX IF NOT EXISTS `index_candidate_detail_cache_updated_at`" +
                        " ON `candidate_detail_cache` (`updated_at`)",
                )
                st.execute("PRAGMA user_version = $USER_VERSION")
            }
            conn.prepareStatement(
                "INSERT OR IGNORE INTO `anagram_entries` (`sorted_key`, `word`, `length`, `is_common`) VALUES (?, ?, ?, ?)",
            ).use { ps ->
                for (row in rows) {
                    ps.setString(1, row.sortedKey)
                    ps.setString(2, row.word)
                    ps.setInt(3, row.length)
                    ps.setInt(4, if (row.isCommon) 1 else 0)
                    ps.addBatch()
                }
                ps.executeBatch()
            }
            conn.commit()
        }
    }
}
