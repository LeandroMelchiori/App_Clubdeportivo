package com.example.clubdeportivo

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class PaymentDbRulesTest {
    @Test
    fun cuotaVencimiento_aplicaDiaConfiguradoYGraciaAlMesSiguiente() {
        assertEquals(
            "2026-09-15",
            PaymentDbRules.cuotaVencimiento("2026-08-31", dueDay = 10, graceDays = 5)
        )
    }

    @Test
    fun cuotaVencimiento_resuelveCambioDeAnio() {
        assertEquals(
            "2027-01-05",
            PaymentDbRules.cuotaVencimiento("2026-12-20", dueDay = 5, graceDays = 0)
        )
    }

    @Test
    fun configuredPaymentMethod_aceptaSoloMetodosHabilitados() {
        val configuration = ClubConfiguration.DEFAULT.copy(
            acceptsCash = false,
            acceptsTransfer = true,
            acceptsCard = false
        )

        assertEquals(
            ManualPaymentMethod.TRANSFER,
            PaymentDbRules.configuredPaymentMethod(configuration, "Transferencia")
        )
        assertThrows(IllegalArgumentException::class.java) {
            PaymentDbRules.configuredPaymentMethod(configuration, "Efectivo")
        }
    }

    @Test
    fun paymentAlreadyRegistered_comparaMesYAnio() {
        assertTrue(
            PaymentDbRules.paymentAlreadyRegistered(
                "2026-08-01",
                LocalDate.parse("2026-08-31")
            )
        )
        assertFalse(
            PaymentDbRules.paymentAlreadyRegistered(
                "2025-08-01",
                LocalDate.parse("2026-08-01")
            )
        )
        assertFalse(PaymentDbRules.paymentAlreadyRegistered("sin-fecha", LocalDate.parse("2026-08-01")))
    }

    @Test
    fun cuotaEstadoPagado_usaUnoParaRegistroConfirmado() {
        assertEquals(1, PaymentDbRules.cuotaEstadoPagado())
    }
}
