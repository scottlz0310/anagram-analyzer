package com.anagram.tools.seedgenerator

import java.text.Normalizer

object HiraganaNormalizer {
    private val WHITESPACE_RE = Regex("""\s+""")

    fun normalize(input: String): String {
        val nfkc = Normalizer.normalize(input, Normalizer.Form.NFKC)
        val noWs = WHITESPACE_RE.replace(nfkc, "")
        return katakanaToHiragana(noWs)
    }

    fun katakanaToHiragana(input: String): String =
        input.map { c ->
            val cp = c.code
            if (cp in 0x30A1..0x30F6) (cp - 0x60).toChar() else c
        }.joinToString("")

    fun isHiragana(c: Char): Boolean {
        val cp = c.code
        return cp in 0x3041..0x3096 || c == 'ー'
    }

    fun isAllHiragana(s: String): Boolean = s.isNotEmpty() && s.all { isHiragana(it) }

    fun anagramKey(s: String): String = s.toCharArray().sorted().joinToString("")
}
