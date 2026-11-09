package com.example.clubdeportivo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UsuarioValidatorTest {
    @Test
    fun dniValido_aceptaOchoONueveDigitos() {
        assertTrue(UsuarioValidator.dniValido("40111222"))
        assertTrue(UsuarioValidator.dniValido("123456789"))
    }

    @Test
    fun dniValido_rechazaTexto() {
        assertFalse(UsuarioValidator.dniValido("40A11222"))
    }

    @Test
    fun telefonoValido_requiereEntreNueveYDoceDigitos() {
        assertTrue(UsuarioValidator.telefonoValido("3415551234"))
        assertFalse(UsuarioValidator.telefonoValido("123"))
    }

    @Test
    fun emailValido_validaFormatoBasico() {
        assertTrue(UsuarioValidator.emailValido("persona@club.com"))
        assertFalse(UsuarioValidator.emailValido("persona@club"))
    }

    @Test
    fun normalizarFechaNacimiento_convierteFechaArgentinaAIso() {
        assertEquals("1993-02-15", UsuarioValidator.normalizarFechaNacimiento("15/02/1993"))
    }

    @Test
    fun normalizarFechaNacimiento_rechazaFechaInexistente() {
        assertNull(UsuarioValidator.normalizarFechaNacimiento("31/02/1993"))
    }
    @Test
    fun fechaNacimientoValida_aceptaFormatoArgentinoOIso() {
        assertTrue(UsuarioValidator.fechaNacimientoValida("15/02/1993"))
        assertTrue(UsuarioValidator.fechaNacimientoValida("1993-02-15"))
        assertFalse(UsuarioValidator.fechaNacimientoValida("31/02/1993"))
    }

}
