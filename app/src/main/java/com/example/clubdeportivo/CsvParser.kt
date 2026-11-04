package com.example.clubdeportivo

import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.Charset
import java.nio.charset.CodingErrorAction

object CsvParser {
    data class Record(val lineNumber: Int, val fields: List<String>)
    data class Result(val records: List<Record>, val error: String? = null)

    private val candidateDelimiters = listOf(',', ';', '\t')

    // Excel en espanol suele guardar en Windows-1252; si los bytes no son UTF-8 valido se reinterpretan.
    fun decode(bytes: ByteArray): String {
        val text = try {
            Charsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(bytes))
                .toString()
        } catch (_: CharacterCodingException) {
            String(bytes, Charset.forName("windows-1252"))
        }
        return text.removePrefix("\uFEFF")
    }

    fun parse(text: String): Result {
        val content = text.removePrefix("\uFEFF")
        val delimiter = detectDelimiter(content)
        val records = mutableListOf<Record>()
        val fields = mutableListOf<String>()
        val field = StringBuilder()
        var line = 1
        var recordLine = 1
        var inQuotes = false
        var quoteStartLine = 1
        var i = 0

        fun endRecord() {
            fields.add(field.toString())
            field.setLength(0)
            if (fields.any { it.isNotBlank() }) records.add(Record(recordLine, fields.toList()))
            fields.clear()
        }

        while (i < content.length) {
            val char = content[i]
            when {
                inQuotes && char == '"' && content.getOrNull(i + 1) == '"' -> {
                    field.append('"')
                    i++
                }
                inQuotes && char == '"' -> inQuotes = false
                inQuotes -> {
                    if (char == '\n') line++
                    field.append(char)
                }
                char == '"' && field.isBlank() -> {
                    field.setLength(0)
                    inQuotes = true
                    quoteStartLine = line
                }
                char == delimiter -> {
                    fields.add(field.toString())
                    field.setLength(0)
                }
                char == '\r' || char == '\n' -> {
                    if (char == '\r' && content.getOrNull(i + 1) == '\n') i++
                    endRecord()
                    line++
                    recordLine = line
                }
                else -> field.append(char)
            }
            i++
        }

        if (inQuotes) {
            return Result(emptyList(), "Hay comillas sin cerrar desde la línea $quoteStartLine")
        }
        if (field.isNotEmpty() || fields.isNotEmpty()) endRecord()
        return Result(records)
    }

    private fun detectDelimiter(content: String): Char {
        val firstLine = content.lineSequence().firstOrNull { it.isNotBlank() }.orEmpty()
        val counts = candidateDelimiters.associateWith { delimiter ->
            var inQuotes = false
            firstLine.count { char ->
                if (char == '"') inQuotes = !inQuotes
                !inQuotes && char == delimiter
            }
        }
        return counts.maxByOrNull { it.value }?.takeIf { it.value > 0 }?.key ?: ','
    }
}
