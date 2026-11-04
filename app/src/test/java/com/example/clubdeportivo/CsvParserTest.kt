package com.example.clubdeportivo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CsvParserTest {
    @Test
    fun parse_handlesQuotedFieldsEscapedQuotesAndLineBreaks() {
        val result = CsvParser.parse(
            "nombre,direccion,nota\r\n" +
                "Ana,\"Calle 1, Piso 2\",\"Dijo \"\"hola\"\"\"\r\n" +
                "Juan,\"Linea uno\nLinea dos\",ok\r\n" +
                "Eva,Calle 3,fin"
        )

        assertNull(result.error)
        assertEquals(4, result.records.size)
        assertEquals(listOf("Ana", "Calle 1, Piso 2", "Dijo \"hola\""), result.records[1].fields)
        assertEquals("Linea uno\nLinea dos", result.records[2].fields[1])
        assertEquals(5, result.records[3].lineNumber)
    }

    @Test
    fun parse_detectsSemicolonDelimiterFromSpanishExcel() {
        val result = CsvParser.parse("dni;nombre;importe\n30111222;Ana;\"1.500,50\"\n")

        assertEquals(listOf("30111222", "Ana", "1.500,50"), result.records[1].fields)
    }

    @Test
    fun parse_skipsBlankLinesAndKeepsPhysicalLineNumbers() {
        val result = CsvParser.parse("dni,nombre\n\n30111222,Ana\n , \n30111223,Juan\n")

        assertEquals(listOf(1, 3, 5), result.records.map { it.lineNumber })
    }

    @Test
    fun parse_reportsUnclosedQuotes() {
        val result = CsvParser.parse("dni,nombre\n30111222,\"Ana\n30111223,Juan\n")

        assertEquals("Hay comillas sin cerrar desde la línea 2", result.error)
        assertEquals(0, result.records.size)
    }

    @Test
    fun decode_removesBomAndFallsBackToWindows1252() {
        val bom = Char(0xFEFF).toString()
        val utf8 = (bom + "nombre\nJosé").toByteArray(Charsets.UTF_8)
        val windows1252 = "nombre\nNúñez".toByteArray(charset("windows-1252"))

        assertEquals("nombre\nJosé", CsvParser.decode(utf8))
        assertEquals("nombre\nNúñez", CsvParser.decode(windows1252))
    }
}
