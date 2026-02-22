package com.anagram.tools.seedgenerator

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NormalizerTest {
    @Test
    fun `カタカナをひらがなに変換する`() {
        assertEquals("ねこ", HiraganaNormalizer.katakanaToHiragana("ネコ"))
    }

    @Test
    fun `小書きカタカナをひらがなに変換する`() {
        assertEquals("ぁぃぅぇぉ", HiraganaNormalizer.katakanaToHiragana("ァィゥェォ"))
    }

    @Test
    fun `ー（長音符）はひらがな扱い`() {
        assertTrue(HiraganaNormalizer.isHiragana('ー'))
    }

    @Test
    fun `NFKC正規化と空白除去`() {
        // 全角スペース
        assertEquals("あい", HiraganaNormalizer.normalize("あ　い"))
        // ASCII space
        assertEquals("あい", HiraganaNormalizer.normalize("あ い"))
    }

    @Test
    fun `アナグラムキー`() {
        assertEquals("ごりん", HiraganaNormalizer.anagramKey("りんご"))
        assertEquals("ごりん", HiraganaNormalizer.anagramKey("ごりん"))
        assertEquals("こね", HiraganaNormalizer.anagramKey("ねこ"))
        assertEquals("こね", HiraganaNormalizer.anagramKey("こね"))
    }

    @Test
    fun `isAllHiragana`() {
        assertTrue(HiraganaNormalizer.isAllHiragana("りんご"))
        assertFalse(HiraganaNormalizer.isAllHiragana("東京"))
        assertFalse(HiraganaNormalizer.isAllHiragana(""))
        assertFalse(HiraganaNormalizer.isAllHiragana("ネコ"))
    }
}
