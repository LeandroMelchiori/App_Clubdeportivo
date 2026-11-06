package com.example.clubdeportivo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ChargeBreakdownFormatterTest {
    @Test
    fun lines_muestraBaseBeneficiosRecargoYTotal() {
        val breakdown = ChargeBreakdown(
            base = 30000.0,
            discountName = "Grupo familiar",
            discountAmount = 4500.0,
            scholarshipPercent = 10,
            scholarshipAmount = 3000.0,
            lateFee = 2250.0,
            lateFeePeriods = 1,
            total = 24750.0
        )

        assertEquals(
            listOf(
                "Cuota base: ARS 30.000,00",
                "Beca 10 %: -ARS 3.000,00",
                "Descuento Grupo familiar: -ARS 4.500,00",
                "Recargo por mora: ARS 2.250,00",
                "Total a cobrar: ARS 24.750,00"
            ),
            ChargeBreakdownFormatter.lines(breakdown, ChargeKind.FEE, ClubCurrency.ARS)
        )
    }

    @Test
    fun lines_indicaMesesDeRecargoYExencionPorBeca() {
        val mensual = ChargeBreakdown(base = 8000.0, lateFee = 2000.0, lateFeePeriods = 2, total = 10000.0)
        val exento = ChargeBreakdown(
            base = 8000.0,
            scholarshipPercent = 100,
            scholarshipAmount = 8000.0,
            lateFeeWaived = true,
            total = 0.0
        )

        assertEquals(
            "Recargo por mora (2 meses): USD 2.000,00",
            ChargeBreakdownFormatter.lines(mensual, ChargeKind.FEE, ClubCurrency.USD)[1]
        )
        assertEquals(
            "Precio base: USD 8.000,00\nBeca 100 %: -USD 8.000,00\nRecargo por mora: exento por beca\nTotal a cobrar: USD 0,00",
            ChargeBreakdownFormatter.text(exento, ChargeKind.ACTIVITY, ClubCurrency.USD)
        )
    }

    @Test
    fun summary_resumeLosAjustesParaElHistorial() {
        val breakdown = ChargeBreakdown(
            base = 30000.0,
            scholarshipPercent = 50,
            scholarshipAmount = 15000.0,
            lateFee = 3000.0,
            lateFeePeriods = 2,
            total = 18000.0
        )

        assertEquals(
            "beca 50 %, recargo por mora (2 meses)",
            ChargeBreakdownFormatter.summary(breakdown)
        )
        assertNull(ChargeBreakdownFormatter.summary(ChargeBreakdown(base = 30000.0)))
    }
}
