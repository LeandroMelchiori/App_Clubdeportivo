package com.example.clubdeportivo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UsuarioFormValidationTest {
    @Test
    fun firstMissing_detectaPrimerCampoFaltante() {
        assertEquals(
            UsuarioFormValidation.Field.APELLIDO,
            UsuarioFormValidation.firstMissing("Ana", "", "01/01/1990", "30111222", "Calle 1", "3415551234", "a@b.com")
        )
    }

    @Test
    fun firstMissing_devuelveNullSiTodoEstaCompleto() {
        assertNull(
            UsuarioFormValidation.firstMissing("Ana", "Perez", "01/01/1990", "30111222", "Calle 1", "3415551234", "a@b.com")
        )
    }
}
