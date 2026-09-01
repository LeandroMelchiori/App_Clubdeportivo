package com.example.clubdeportivo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PaymentValidatorTest {
    @Test
    fun validateManualPayment_rechazaMedioVacio() {
        val result = PaymentValidator.validateManualPayment(30000.0, "   ")

        assertFalse(result.isValid)
        assertEquals("Debe seleccionar una forma de pago", result.error)
    }

    @Test
    fun validateManualPayment_rechazaMontoNuloOCero() {
        assertFalse(PaymentValidator.validateManualPayment(null, "Efectivo").isValid)
        assertFalse(PaymentValidator.validateManualPayment(0.0, "Efectivo").isValid)
    }

    @Test
    fun validateManualPayment_aceptaMontoYMedioValidos() {
        assertTrue(PaymentValidator.validateManualPayment(15000.0, "Debito").isValid)
    }
}
