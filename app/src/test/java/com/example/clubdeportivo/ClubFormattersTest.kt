package com.example.clubdeportivo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class ClubFormattersTest {
    @Test
    fun etiquetaDia_mapeaDomingoASabado() {
        assertEquals("Dom", ClubFormatters.etiquetaDia(0))
        assertEquals("Mie", ClubFormatters.etiquetaDia(3))
        assertEquals("Sab", ClubFormatters.etiquetaDia(6))
    }

    @Test
    fun nombreDia_mapeaDiasParaPantallas() {
        assertEquals("Domingo", ClubFormatters.nombreDia(0))
        assertEquals("Jueves", ClubFormatters.nombreDia(4))
        assertEquals("Sabado", ClubFormatters.nombreDia(6))
    }

    @Test
    fun hhmm_formateaMinutosDesdeMedianoche() {
        assertEquals("00:00", ClubFormatters.hhmm(0))
        assertEquals("09:00", ClubFormatters.hhmm(540))
        assertEquals("19:30", ClubFormatters.hhmm(1170))
    }

    @Test
    fun hhmm_rechazaMinutosNegativos() {
        assertThrows(IllegalArgumentException::class.java) {
            ClubFormatters.hhmm(-1)
        }
    }

    @Test
    fun proximoVencimiento_sumaUnMesAFechaIso() {
        assertEquals("2026-08-31", ClubFormatters.proximoVencimiento("2026-07-31"))
    }
}
