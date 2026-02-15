package com.anagram.analyzer.data.seed

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class JishoCandidateDetailRemoteDataSourceTest {
    @Test
    fun wordまたはreadingが一致する候補から詳細を抽出できる() {
        val body =
            """
            {
              "data": [
                {
                  "japanese": [
                    { "word": "同じ", "reading": "おなじ" }
                  ],
                  "senses": [
                    { "english_definitions": ["same", "similar", "identical", "equal"] }
                  ]
                }
              ]
            }
            """.trimIndent()

        val detail = parseJishoCandidateDetail(
            word = "おなじ",
            body = body,
        )

        assertEquals("同じ", detail?.kanji)
        assertEquals("same, similar, identical", detail?.meaning)
    }

    @Test
    fun wordが空でもreadingを漢字表示へフォールバックする() {
        val body =
            """
            {
              "data": [
                {
                  "japanese": [
                    { "reading": "おはよう" }
                  ],
                  "senses": [
                    { "english_definitions": ["good morning"] }
                  ]
                }
              ]
            }
            """.trimIndent()

        val detail = parseJishoCandidateDetail(
            word = "おはよう",
            body = body,
        )

        assertEquals("おはよう", detail?.kanji)
        assertEquals("good morning", detail?.meaning)
    }

    @Test
    fun 定義が空ならnullを返す() {
        val body =
            """
            {
              "data": [
                {
                  "japanese": [
                    { "word": "同じ", "reading": "おなじ" }
                  ],
                  "senses": [
                    { "english_definitions": [] }
                  ]
                }
              ]
            }
            """.trimIndent()

        val detail = parseJishoCandidateDetail(
            word = "おなじ",
            body = body,
        )

        assertNull(detail)
    }
}
