package com.example.clubdeportivo

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class CuentaCorrienteCalculatorTest {
    private val hoy = LocalDate.parse("2026-08-01")
    private val cuotaConfigurada = 42500.0

    @Test
    fun evaluarSocio_sinPagosMarcaDeudaConfigurada() {
        val estado = CuentaCorrienteCalculator.evaluarSocio(null, cuotaConfigurada, hoy)

        assertEquals("Sin pagos", estado.estado)
        assertEquals(cuotaConfigurada, estado.deudaEstimada, 0.0)
    }

    @Test
    fun evaluarSocio_vencidoMarcaDeudaConfigurada() {
        val estado = CuentaCorrienteCalculator.evaluarSocio("2026-07-30", cuotaConfigurada, hoy)

        assertEquals("Vencido", estado.estado)
        assertEquals("Vencio hace 2 dias", estado.detalle)
        assertEquals(cuotaConfigurada, estado.deudaEstimada, 0.0)
    }

    @Test
    fun evaluarSocio_vencidoUsaElImporteConBeneficiosYRecargo() {
        val becaTotal = CuentaCorrienteCalculator.evaluarSocio("2026-07-30", 0.0, hoy)
        val conRecargo = CuentaCorrienteCalculator.evaluarSocio("2026-07-30", 28050.0, hoy)

        assertEquals("Vencido", becaTotal.estado)
        assertEquals(0.0, becaTotal.deudaEstimada, 0.0)
        assertEquals(28050.0, conRecargo.deudaEstimada, 0.0)
    }

    @Test
    fun evaluarSocio_rechazaImporteNegativo() {
        assertThrows(IllegalArgumentException::class.java) {
            CuentaCorrienteCalculator.evaluarSocio(null, -1.0, hoy)
        }
    }

    @Test
    fun evaluarSocio_porVencerNoMarcaDeuda() {
        val estado = CuentaCorrienteCalculator.evaluarSocio("2026-08-06", cuotaConfigurada, hoy)

        assertEquals("Por vencer", estado.estado)
        assertEquals(0.0, estado.deudaEstimada, 0.0)
    }

    @Test
    fun evaluarSocio_alDiaNoMarcaDeuda() {
        val estado = CuentaCorrienteCalculator.evaluarSocio("2026-08-20", cuotaConfigurada, hoy)

        assertEquals("Al dia", estado.estado)
        assertEquals(0.0, estado.deudaEstimada, 0.0)
    }
}
