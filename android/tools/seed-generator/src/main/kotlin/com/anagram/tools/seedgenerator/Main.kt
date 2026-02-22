package com.anagram.tools.seedgenerator

import java.nio.file.Paths

fun main(args: Array<String>) {
    val parsed = parseArgs(args)
    val jmdictArg = parsed["--jmdict"] ?: error("--jmdict が必要です")
    val xmlPath = Paths.get(jmdictArg)
    require(xmlPath.toFile().exists()) { "JMdict XMLが見つかりません: $xmlPath" }

    val minLen = parsed["--min-len"]?.toInt() ?: 2
    val maxLen = parsed["--max-len"]?.toInt() ?: 8
    val limit = parsed["--limit"]?.toInt() ?: 0
    val force = "--force" in args
    val mode = parsed["--mode"] ?: "both"

    println("JMdict XMLを解析中: $xmlPath")
    val rows = JmdictParser.parse(xmlPath, minLen, maxLen, limit)
    println("解析完了: ${rows.size}件")

    when (mode) {
        "tsv" -> {
            val outTsv = Paths.get(parsed["--out-tsv"] ?: "anagram_seed.tsv")
            TsvExporter.export(rows, outTsv)
            println("TSV出力完了: $outTsv")
        }
        "db" -> {
            val outDb = Paths.get(parsed["--out-db"] ?: "anagram_seed.db")
            DbExporter.export(rows, outDb, force)
            println("DB出力完了: $outDb")
        }
        "both" -> {
            val outTsv = Paths.get(parsed["--out-tsv"] ?: "anagram_seed.tsv")
            val outDb = Paths.get(parsed["--out-db"] ?: "anagram_seed.db")
            TsvExporter.export(rows, outTsv)
            println("TSV出力完了: $outTsv")
            DbExporter.export(rows, outDb, force)
            println("DB出力完了: $outDb")
        }
        else -> error("--mode は tsv|db|both を指定してください")
    }
}

private fun parseArgs(args: Array<String>): Map<String, String> {
    val map = mutableMapOf<String, String>()
    var i = 0
    while (i < args.size) {
        val arg = args[i]
        if (arg.startsWith("--") && i + 1 < args.size && !args[i + 1].startsWith("--")) {
            map[arg] = args[i + 1]
            i += 2
        } else {
            i++
        }
    }
    return map
}
