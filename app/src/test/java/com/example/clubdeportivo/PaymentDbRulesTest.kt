package com.example.clubdeportivo

import org.junit.Assert.assertEquals
import org.junit.Test

class PaymentDbRulesTest {
    @Test
    fun cuotaVencimiento_sumaUnMesAFechaDePago() {
        assertEquals("2026-09-10", PaymentDbRules.cuotaVencimiento("2026-08-10"))
    }

    @Test
    fun cuotaEstadoPagado_usaUnoParaRegistroConfirmado() {
        assertEquals(1, PaymentDbRules.cuotaEstadoPagado())
    }
}
