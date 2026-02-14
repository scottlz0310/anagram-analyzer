package com.anagram.analyzer.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class HiraganaNormalizerTest {
    @Test
    fun katakanaToHiragana_カタカナを変換できる() {
        assertEquals("あいうえお", HiraganaNormalizer.katakanaToHiragana("アイウエオ"))
    }

    @Test
    fun katakanaToHiragana_ひらがなはそのまま() {
        assertEquals("あいうえお", HiraganaNormalizer.katakanaToHiragana("あいうえお"))
    }

    @Test
    fun isHiragana_判定できる() {
        assertTrue(HiraganaNormalizer.isHiragana('あ'))
        assertTrue(HiraganaNormalizer.isHiragana('ー'))
        assertFalse(HiraganaNormalizer.isHiragana('ア'))
    }

    @Test
    fun normalizeHiragana_空白を除去して正規化する() {
        assertEquals("あいう", HiraganaNormalizer.normalizeHiragana(" あ　い う "))
    }

    @Test
    fun normalizeHiragana_非ひらがなを拒否する() {
        assertThrows(NormalizationException::class.java) {
            HiraganaNormalizer.normalizeHiragana("あいう123")
        }
    }

    @Test
    fun anagramKey_キーを生成できる() {
        assertEquals("ごりん", HiraganaNormalizer.anagramKey("りんご"))
    }
}
