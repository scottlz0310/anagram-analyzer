package com.anagram.tools.seedgenerator

import java.io.InputStream
import java.nio.file.Path
import java.util.zip.GZIPInputStream
import javax.xml.stream.XMLInputFactory
import javax.xml.stream.XMLStreamConstants

data class AnagramRow(val sortedKey: String, val word: String, val length: Int, val isCommon: Boolean = false)

/** `re_pri` 値が「一般語」とみなせるかを判定する正規表現。 */
private val COMMON_PRI_REGEX = Regex("^(news[12]|ichi[12]|spec[12]|gai[12]|nf\\d+)$")

object JmdictParser {
    fun parse(xmlPath: Path, minLen: Int, maxLen: Int, limit: Int = 0): List<AnagramRow> {
        val rows = mutableListOf<AnagramRow>()
        val seen = mutableSetOf<String>()

        openStream(xmlPath).use { stream ->
            val factory = XMLInputFactory.newInstance().apply {
                setProperty(XMLInputFactory.IS_VALIDATING, false)
                setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false)
            }
            val reader = factory.createXMLStreamReader(stream)
            var inREle = false
            var inReb = false
            var inRePri = false
            val rebBuf = StringBuilder()
            val rePriBuf = StringBuilder()
            val rePris = mutableListOf<String>()

            while (reader.hasNext()) {
                when (reader.next()) {
                    XMLStreamConstants.START_ELEMENT -> when (reader.localName) {
                        "r_ele" -> { inREle = true; rebBuf.clear(); rePris.clear() }
                        "reb"   -> if (inREle) { inReb = true; rebBuf.clear() }
                        "re_pri" -> if (inREle) { inRePri = true; rePriBuf.clear() }
                    }
                    XMLStreamConstants.CHARACTERS, XMLStreamConstants.CDATA -> {
                        if (inReb) rebBuf.append(reader.text)
                        if (inRePri) rePriBuf.append(reader.text)
                    }
                    XMLStreamConstants.END_ELEMENT -> when (reader.localName) {
                        "reb"    -> inReb = false
                        "re_pri" -> if (inREle) { rePris.add(rePriBuf.toString().trim()); inRePri = false }
                        "r_ele"  -> {
                            inREle = false
                            val word = HiraganaNormalizer.normalize(rebBuf.toString().trim())
                            val isCommon = rePris.any { COMMON_PRI_REGEX.matches(it) }
                            if (word.length in minLen..maxLen &&
                                HiraganaNormalizer.isAllHiragana(word) &&
                                seen.add(word)
                            ) {
                                rows.add(AnagramRow(HiraganaNormalizer.anagramKey(word), word, word.length, isCommon))
                                if (limit > 0 && rows.size >= limit) {
                                    reader.close()
                                    return rows
                                }
                            }
                        }
                    }
                }
            }
            reader.close()
        }
        return rows
    }

    private fun openStream(path: Path): InputStream {
        val raw = path.toFile().inputStream().buffered()
        return if (path.fileName.toString().endsWith(".gz")) GZIPInputStream(raw) else raw
    }
}
