package com.anagram.analyzer.domain.model

import java.text.Normalizer

class NormalizationException(message: String) : IllegalArgumentException(message)

object HiraganaNormalizer {
    fun normalizeHiragana(input: String): String {
        if (input.isEmpty()) {
            throw NormalizationException("空の文字列は処理できません")
        }

        val normalized = Normalizer.normalize(input, Normalizer.Form.NFKC)
            .replace(WHITESPACE_REGEX, "")
        if (normalized.isEmpty()) {
            throw NormalizationException("空白のみの文字列は処理できません")
        }

        val hiragana = katakanaToHiragana(normalized)
        if (!isAllHiragana(hiragana)) {
            val invalidChars = hiragana.filterNot(::isHiragana).toList()
            throw NormalizationException("ひらがな以外の文字が含まれています: $invalidChars")
        }

        return hiragana
    }

    fun katakanaToHiragana(input: String): String = buildString(input.length) {
        input.forEach { char ->
            val code = char.code
            if (code in KATAKANA_START..KATAKANA_END) {
                append((code - KATAKANA_HIRAGANA_OFFSET).toChar())
            } else {
                append(char)
            }
        }
    }

    fun isHiragana(char: Char): Boolean {
        val code = char.code
        return code in HIRAGANA_START..HIRAGANA_END || char == CHOONPU
    }

    fun isAllHiragana(input: String): Boolean = input.all(::isHiragana)

    fun anagramKey(input: String): String = input.toList().sorted().joinToString("")

    private val WHITESPACE_REGEX = "\\s+".toRegex()
    private const val KATAKANA_START = 0x30A1
    private const val KATAKANA_END = 0x30F6
    private const val HIRAGANA_START = 0x3041
    private const val HIRAGANA_END = 0x3096
    private const val KATAKANA_HIRAGANA_OFFSET = 0x60
    private const val CHOONPU = 'ー'
}
