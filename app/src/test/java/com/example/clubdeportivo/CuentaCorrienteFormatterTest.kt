package com.example.clubdeportivo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CuentaCorrienteFormatterTest {
    @Test
    fun historial_muestraEstadoVacio() {
        assertEquals(
            "Sin movimientos registrados",
            CuentaCorrienteFormatter.historial(emptyList(), ClubCurrency.ARS)
        )
    }

    @Test
    fun historial_formateaMovimientosConMonedaConfigurada() {
        val texto = CuentaCorrienteFormatter.historial(
            listOf(
                DBHelper.MovimientoCuenta("Cuota", "2026-08-01", 30000.0, "Efectivo"),
                DBHelper.MovimientoCuenta("Actividad", "2026-07-28", 5000.0, "Yoga")
            ),
            ClubCurrency.USD
        )

        assertTrue(texto.contains("2026-08-01 - Cuota: USD 30.000,00 Efectivo"))
        assertTrue(texto.contains("2026-07-28 - Actividad: USD 5.000,00 Yoga"))
    }

    @Test
    fun filtrar_devuelveSoloCuotasOActividades() {
        val movimientos = listOf(
            DBHelper.MovimientoCuenta("Cuota", "2026-08-01", 30000.0, "Efectivo"),
            DBHelper.MovimientoCuenta("Actividad", "2026-07-28", 5000.0, "Yoga")
        )

        assertEquals(
            listOf("Cuota"),
            CuentaCorrienteFormatter.filtrar(movimientos, CuentaCorrienteFormatter.Filtro.CUOTAS).map { it.tipo }
        )
        assertEquals(
            listOf("Actividad"),
            CuentaCorrienteFormatter.filtrar(movimientos, CuentaCorrienteFormatter.Filtro.ACTIVIDADES).map { it.tipo }
        )
    }

    @Test
    fun historial_conFiltroMuestraEstadoVacioSiNoHayTipo() {
        val movimientos = listOf(
            DBHelper.MovimientoCuenta("Cuota", "2026-08-01", 30000.0, "Efectivo")
        )

        assertEquals(
            "Sin movimientos registrados",
            CuentaCorrienteFormatter.historial(
                movimientos,
                ClubCurrency.ARS,
                CuentaCorrienteFormatter.Filtro.ACTIVIDADES
            )
        )
    }
}
