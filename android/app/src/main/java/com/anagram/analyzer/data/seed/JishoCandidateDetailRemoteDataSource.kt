package com.anagram.analyzer.data.seed

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import org.json.JSONObject
import javax.inject.Inject

interface CandidateDetailRemoteDataSource {
    @Throws(IOException::class)
    suspend fun fetchDetail(word: String): CandidateDetail?
}

class JishoCandidateDetailRemoteDataSource @Inject constructor() : CandidateDetailRemoteDataSource {
    override suspend fun fetchDetail(word: String): CandidateDetail? {
        val encodedWord = URLEncoder.encode(word, Charsets.UTF_8.name())
        val connection = (URL("$BASE_URL$encodedWord").openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = CONNECT_TIMEOUT_MILLIS
            readTimeout = READ_TIMEOUT_MILLIS
        }
        return try {
            if (connection.responseCode !in HTTP_SUCCESS_MIN..HTTP_SUCCESS_MAX) {
                return null
            }
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            parseCandidateDetail(word = word, body = body)
        } finally {
            connection.disconnect()
        }
    }

    private fun parseCandidateDetail(word: String, body: String): CandidateDetail? {
        val data = JSONObject(body).optJSONArray("data") ?: return null
        for (entryIndex in 0 until data.length()) {
            val entry = data.optJSONObject(entryIndex) ?: continue
            val japanese = entry.optJSONArray("japanese") ?: continue
            val matchedJapanese = findBestMatch(japanese, word) ?: continue
            val kanji = matchedJapanese.optString("word").ifBlank {
                matchedJapanese.optString("reading").ifBlank { word }
            }
            val meaning = extractMeaning(entry) ?: continue
            return CandidateDetail(
                kanji = kanji,
                meaning = meaning,
            )
        }
        return null
    }

    private fun findBestMatch(
        japanese: org.json.JSONArray,
        word: String,
    ): JSONObject? {
        var fallback: JSONObject? = null
        for (japaneseIndex in 0 until japanese.length()) {
            val candidate = japanese.optJSONObject(japaneseIndex) ?: continue
            if (fallback == null) {
                fallback = candidate
            }
            val reading = candidate.optString("reading")
            val written = candidate.optString("word")
            if (reading == word || written == word) {
                return candidate
            }
        }
        return fallback
    }

    private fun extractMeaning(entry: JSONObject): String? {
        val senses = entry.optJSONArray("senses") ?: return null
        val definitions = mutableListOf<String>()
        for (senseIndex in 0 until senses.length()) {
            val sense = senses.optJSONObject(senseIndex) ?: continue
            val englishDefinitions = sense.optJSONArray("english_definitions") ?: continue
            for (definitionIndex in 0 until englishDefinitions.length()) {
                val definition = englishDefinitions.optString(definitionIndex).trim()
                if (definition.isNotEmpty()) {
                    definitions.add(definition)
                }
                if (definitions.size >= MAX_MEANING_ITEMS) {
                    return definitions.joinToString(separator = ", ")
                }
            }
        }
        return definitions.takeIf { it.isNotEmpty() }?.joinToString(separator = ", ")
    }

    private companion object {
        private const val BASE_URL = "https://jisho.org/api/v1/search/words?keyword="
        private const val CONNECT_TIMEOUT_MILLIS = 5_000
        private const val READ_TIMEOUT_MILLIS = 5_000
        private const val HTTP_SUCCESS_MIN = 200
        private const val HTTP_SUCCESS_MAX = 299
        private const val MAX_MEANING_ITEMS = 3
    }
}
