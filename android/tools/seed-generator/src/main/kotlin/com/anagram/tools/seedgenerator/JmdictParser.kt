package com.anagram.tools.seedgenerator

import java.io.InputStream
import java.nio.file.Path
import java.util.zip.GZIPInputStream
import javax.xml.stream.XMLInputFactory
import javax.xml.stream.XMLStreamConstants

data class AnagramRow(val sortedKey: String, val word: String, val length: Int)

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
            var inReb = false
            val buf = StringBuilder()

            while (reader.hasNext()) {
                when (reader.next()) {
                    XMLStreamConstants.START_ELEMENT ->
                        if (reader.localName == "reb") { inReb = true; buf.clear() }
                    XMLStreamConstants.CHARACTERS, XMLStreamConstants.CDATA ->
                        if (inReb) buf.append(reader.text)
                    XMLStreamConstants.END_ELEMENT ->
                        if (reader.localName == "reb") {
                            inReb = false
                            val word = HiraganaNormalizer.normalize(buf.toString().trim())
                            if (word.length in minLen..maxLen &&
                                HiraganaNormalizer.isAllHiragana(word) &&
                                seen.add(word)
                            ) {
                                rows.add(AnagramRow(HiraganaNormalizer.anagramKey(word), word, word.length))
                                if (limit > 0 && rows.size >= limit) {
                                    reader.close()
                                    return rows
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
