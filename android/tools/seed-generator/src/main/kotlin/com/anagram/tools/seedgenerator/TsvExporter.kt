package com.anagram.tools.seedgenerator

import java.nio.file.Path
import kotlin.io.path.bufferedWriter
import kotlin.io.path.createDirectories

object TsvExporter {
    /** rows をword順にソートしてTSVへ出力する。 */
    fun export(rows: List<AnagramRow>, outPath: Path) {
        outPath.parent?.createDirectories()
        val sorted = rows.sortedBy { it.word }
        outPath.bufferedWriter(Charsets.UTF_8).use { w ->
            w.write("# sorted_key\tword\tlength\n")
            for (r in sorted) {
                w.write("${r.sortedKey}\t${r.word}\t${r.length}\n")
            }
        }
    }
}
