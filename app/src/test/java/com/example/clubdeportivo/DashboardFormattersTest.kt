package com.example.clubdeportivo

import org.junit.Assert.assertEquals
import org.junit.Test

class DashboardFormattersTest {
    @Test
    fun montoPesos_formateaMilesSinDecimales() {
        assertEquals("$30.000", DashboardFormatters.montoPesos(30000.0))
        assertEquals("$1.250.000", DashboardFormatters.montoPesos(1250000.0))
    }
}
