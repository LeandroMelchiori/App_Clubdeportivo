package com.example.clubdeportivo

import org.junit.Assert.assertEquals
import org.junit.Test

class AccessibilityTextTest {
    @Test
    fun labels_accionesClaveSonDescriptivos() {
        assertEquals("Ingresar al sistema", AccessibilityText.login)
        assertEquals("Eliminar cliente", AccessibilityText.deletePerson)
        assertEquals("Registrar pago manual", AccessibilityText.pay)
    }
}
