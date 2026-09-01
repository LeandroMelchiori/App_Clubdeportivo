package com.example.clubdeportivo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class HorarioFormOptionsTest {
    @Test
    fun minutosCada_creaOpcionesDeMediaHora() {
        assertEquals(listOf(0, 30, 60), HorarioFormOptions.minutosCada(step = 30, desde = 0, hasta = 60))
    }

    @Test
    fun etiquetas_formateaOpcionesHHmm() {
        assertEquals(listOf("08:00", "08:30"), HorarioFormOptions.etiquetas(listOf(480, 510)))
    }

    @Test
    fun posicionMasCercana_usaLaAnteriorCuandoNoHayExacta() {
        assertEquals(1, HorarioFormOptions.posicionMasCercana(listOf(480, 510, 540), 525))
    }

    @Test
    fun slots30Min_generaRangoDePantalla() {
        assertEquals(listOf("06:00", "06:30", "07:00"), HorarioFormOptions.slots30Min(startHour = 6, endHour = 7, includeEndHalf = false))
    }

    @Test
    fun hhmmToMin_convierteYValidaFormato() {
        assertEquals(570, HorarioFormOptions.hhmmToMin("09:30"))
        assertThrows(IllegalArgumentException::class.java) {
            HorarioFormOptions.hhmmToMin("25:00")
        }
    }
}
