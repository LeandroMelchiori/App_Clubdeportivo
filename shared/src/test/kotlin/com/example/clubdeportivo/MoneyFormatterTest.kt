package com.example.clubdeportivo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class MoneyFormatterTest {
    @Test
    fun format_incluyeCodigoYFormatoRegional() {
        assertEquals("ARS 30.000,00", MoneyFormatter.format(30000.0, ClubCurrency.ARS))
        assertEquals("USD 1.250,50", MoneyFormatter.format(1250.5, ClubCurrency.USD))
        assertEquals("PYG 125.000", MoneyFormatter.format(125000.0, ClubCurrency.PYG))
    }

    @Test
    fun format_rechazaImportesNoFinitos() {
        assertThrows(IllegalArgumentException::class.java) {
            MoneyFormatter.format(Double.POSITIVE_INFINITY, ClubCurrency.ARS)
        }
    }
}
