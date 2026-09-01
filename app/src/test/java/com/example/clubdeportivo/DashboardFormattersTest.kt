package com.example.clubdeportivo

import org.junit.Assert.assertEquals
import org.junit.Test

class DashboardFormattersTest {
    @Test
    fun monto_usaMonedaConfigurada() {
        assertEquals("ARS 30.000,00", DashboardFormatters.monto(30000.0, ClubCurrency.ARS))
        assertEquals("USD 1.250.000,00", DashboardFormatters.monto(1250000.0, ClubCurrency.USD))
    }
}
