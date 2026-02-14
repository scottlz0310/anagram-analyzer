package com.anagram.analyzer.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class HiraganaNormalizerTest {
    @Test
    fun katakanaToHiragana_python版テストケースと一致する() {
        assertEquals("あいうえお", HiraganaNormalizer.katakanaToHiragana("アイウエオ"))
        assertEquals("かきくけこ", HiraganaNormalizer.katakanaToHiragana("カキクケコ"))
        assertEquals("あいうえお", HiraganaNormalizer.katakanaToHiragana("あイうエお"))
        assertEquals("あいうえお", HiraganaNormalizer.katakanaToHiragana("あいうえお"))
        assertEquals("", HiraganaNormalizer.katakanaToHiragana(""))
    }

    @Test
    fun isHiragana_python版テストケースと一致する() {
        assertTrue(HiraganaNormalizer.isHiragana('あ'))
        assertTrue(HiraganaNormalizer.isHiragana('ん'))
        assertTrue(HiraganaNormalizer.isHiragana('ゃ'))
        assertFalse(HiraganaNormalizer.isHiragana('ア'))
        assertFalse(HiraganaNormalizer.isHiragana('ン'))
        assertFalse(HiraganaNormalizer.isHiragana('a'))
        assertFalse(HiraganaNormalizer.isHiragana('漢'))
        assertFalse(HiraganaNormalizer.isHiragana('1'))
    }

    @Test
    fun isAllHiragana_python版テストケースと一致する() {
        assertTrue(HiraganaNormalizer.isAllHiragana("あいうえお"))
        assertTrue(HiraganaNormalizer.isAllHiragana("りんご"))
        assertTrue(HiraganaNormalizer.isAllHiragana(""))
        assertFalse(HiraganaNormalizer.isAllHiragana("あいアウエオ"))
        assertFalse(HiraganaNormalizer.isAllHiragana("りんご1"))
    }

    @Test
    fun normalizeHiragana_python版テストケースと一致する() {
        val cases = listOf(
            "あいうえお" to "あいうえお",
            "アイウエオ" to "あいうえお",
            "あ い う" to "あいう",
            "  あいう  " to "あいう",
            "あ　い　う" to "あいう",
        )
        cases.forEach { (input, expected) ->
            assertEquals("input: $input", expected, HiraganaNormalizer.normalizeHiragana(input))
        }
    }

    @Test
    fun normalizeHiragana_python版と同じ条件で例外を返す() {
        val emptyError = assertThrows(NormalizationException::class.java) {
            HiraganaNormalizer.normalizeHiragana("")
        }
        assertEquals("空の文字列は処理できません", emptyError.message)

        val whitespaceOnlyError = assertThrows(NormalizationException::class.java) {
            HiraganaNormalizer.normalizeHiragana("   ")
        }
        assertEquals("空白のみの文字列は処理できません", whitespaceOnlyError.message)

        val nonHiraganaCases = listOf("apple", "漢字", "あいう123")
        nonHiraganaCases.forEach { input ->
            val error = assertThrows(NormalizationException::class.java) {
                HiraganaNormalizer.normalizeHiragana(input)
            }
            assertTrue(
                "input=$input, message=${error.message}",
                error.message?.contains("ひらがな以外の文字が含まれています") ?: false,
            )
        }
    }

    @Test
    fun anagramKey_python版テストケースと一致する() {
        assertEquals("ごりん", HiraganaNormalizer.anagramKey("りんご"))
        assertEquals("くさら", HiraganaNormalizer.anagramKey("さくら"))
        assertEquals(HiraganaNormalizer.anagramKey("いろは"), HiraganaNormalizer.anagramKey("はろい"))
        assertEquals(HiraganaNormalizer.anagramKey("いろは"), HiraganaNormalizer.anagramKey("ろいは"))
        assertNotEquals(HiraganaNormalizer.anagramKey("あいう"), HiraganaNormalizer.anagramKey("えおか"))
        assertEquals("", HiraganaNormalizer.anagramKey(""))
        assertEquals("あ", HiraganaNormalizer.anagramKey("あ"))
    }
}
