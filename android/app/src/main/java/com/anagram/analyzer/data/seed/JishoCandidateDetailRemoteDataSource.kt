package com.anagram.analyzer.data.seed

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import javax.inject.Inject
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

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
            parseJishoCandidateDetail(word = word, body = body)
        } finally {
            connection.disconnect()
        }
    }

    private companion object {
        private const val BASE_URL = "https://jisho.org/api/v1/search/words?keyword="
        private const val CONNECT_TIMEOUT_MILLIS = 5_000
        private const val READ_TIMEOUT_MILLIS = 5_000
        private const val HTTP_SUCCESS_MIN = 200
        private const val HTTP_SUCCESS_MAX = 299
    }
}

internal fun parseJishoCandidateDetail(word: String, body: String): CandidateDetail? {
    val rootObject = runCatching {
        Json.parseToJsonElement(body) as? JsonObject
    }.getOrNull() ?: return null
    val data = rootObject["data"] as? JsonArray ?: return null
    for (entryElement in data) {
        val entry = entryElement as? JsonObject ?: continue
        val japanese = entry["japanese"] as? JsonArray ?: continue
        val matchedJapanese = findBestJishoMatch(japanese, word) ?: continue
        val kanji = matchedJapanese["word"].asStringOrNull().orEmpty().ifBlank {
            matchedJapanese["reading"].asStringOrNull().orEmpty().ifBlank { word }
        }
        val meaning = extractJishoMeaning(entry) ?: continue
        return CandidateDetail(
            kanji = kanji,
            meaning = meaning,
        )
    }
    return null
}

private fun findBestJishoMatch(
    japanese: JsonArray,
    word: String,
): JsonObject? {
    var fallback: JsonObject? = null
    for (candidateElement in japanese) {
        val candidate = candidateElement as? JsonObject ?: continue
        if (fallback == null) {
            fallback = candidate
        }
        val reading = candidate["reading"].asStringOrNull().orEmpty()
        val written = candidate["word"].asStringOrNull().orEmpty()
        if (reading == word || written == word) {
            return candidate
        }
    }
    return fallback
}

private fun extractJishoMeaning(entry: JsonObject): String? {
    val senses = entry["senses"] as? JsonArray ?: return null
    val definitions = mutableListOf<String>()
    for (senseElement in senses) {
        val sense = senseElement as? JsonObject ?: continue
        val englishDefinitions = sense["english_definitions"] as? JsonArray ?: continue
        for (definitionElement in englishDefinitions) {
            val definition = definitionElement.asStringOrNull().orEmpty().trim()
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

private const val MAX_MEANING_ITEMS = 3

private fun JsonElement?.asStringOrNull(): String? {
    val primitive = this as? JsonPrimitive ?: return null
    return primitive.contentOrNull
}
