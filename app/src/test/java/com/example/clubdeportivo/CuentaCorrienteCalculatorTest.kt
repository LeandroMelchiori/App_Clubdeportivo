package com.example.clubdeportivo

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class CuentaCorrienteCalculatorTest {
    private val hoy = LocalDate.parse("2026-08-01")

    @Test
    fun evaluarSocio_sinPagosMarcaDeuda() {
        val estado = CuentaCorrienteCalculator.evaluarSocio(null, hoy)

        assertEquals("Sin pagos", estado.estado)
        assertEquals(30000.0, estado.deudaEstimada, 0.0)
    }

    @Test
    fun evaluarSocio_vencidoMarcaDeuda() {
        val estado = CuentaCorrienteCalculator.evaluarSocio("2026-07-30", hoy)

        assertEquals("Vencido", estado.estado)
        assertEquals("Vencio hace 2 dias", estado.detalle)
        assertEquals(30000.0, estado.deudaEstimada, 0.0)
    }

    @Test
    fun evaluarSocio_porVencerNoMarcaDeuda() {
        val estado = CuentaCorrienteCalculator.evaluarSocio("2026-08-06", hoy)

        assertEquals("Por vencer", estado.estado)
        assertEquals(0.0, estado.deudaEstimada, 0.0)
    }

    @Test
    fun evaluarSocio_alDiaNoMarcaDeuda() {
        val estado = CuentaCorrienteCalculator.evaluarSocio("2026-08-20", hoy)

        assertEquals("Al dia", estado.estado)
        assertEquals(0.0, estado.deudaEstimada, 0.0)
    }
}
