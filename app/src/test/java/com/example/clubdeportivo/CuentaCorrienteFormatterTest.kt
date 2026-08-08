package com.example.clubdeportivo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CuentaCorrienteFormatterTest {
    @Test
    fun historial_muestraEstadoVacio() {
        assertEquals("Sin movimientos registrados", CuentaCorrienteFormatter.historial(emptyList()))
    }

    @Test
    fun historial_formateaMovimientosEnLineas() {
        val texto = CuentaCorrienteFormatter.historial(
            listOf(
                DBHelper.MovimientoCuenta("Cuota", "2026-08-01", 30000.0, "Efectivo"),
                DBHelper.MovimientoCuenta("Actividad", "2026-07-28", 5000.0, "Yoga")
            )
        )

        assertTrue(texto.contains("2026-08-01 - Cuota: $30000.0 Efectivo"))
        assertTrue(texto.contains("2026-07-28 - Actividad: $5000.0 Yoga"))
    }
}
