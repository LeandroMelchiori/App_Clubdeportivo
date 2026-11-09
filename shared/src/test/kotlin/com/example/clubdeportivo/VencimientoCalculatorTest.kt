package com.example.clubdeportivo

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class VencimientoCalculatorTest {
    private val hoy = LocalDate.parse("2026-08-01")

    @Test
    fun clasificar_vencido() {
        val estado = VencimientoCalculator.clasificar("2026-07-20", hoy)

        assertEquals("Vencido", estado.categoria)
        assertEquals("Debe hace 12 dias", estado.texto)
    }

    @Test
    fun clasificar_venceHoy() {
        val estado = VencimientoCalculator.clasificar("2026-08-01", hoy)

        assertEquals("Por vencer", estado.categoria)
        assertEquals("Vence hoy", estado.texto)
    }

    @Test
    fun clasificar_porVencer() {
        val estado = VencimientoCalculator.clasificar("2026-08-05", hoy)

        assertEquals("Por vencer", estado.categoria)
    }

    @Test
    fun clasificar_alDia() {
        val estado = VencimientoCalculator.clasificar("2026-08-20", hoy)

        assertEquals("Al dia", estado.categoria)
    }
}
