package com.example.clubdeportivo

import org.junit.Assert.assertEquals
import org.junit.Test

class LoginMessagesTest {
    @Test
    fun mensajes_loginUsanCopiaClara() {
        assertEquals("Por favor ingres\u00e1 usuario y contrase\u00f1a", LoginMessages.emptyFields)
        assertEquals("Sesi\u00f3n iniciada", LoginMessages.success)
        assertEquals("Usuario o contrase\u00f1a incorrectos", LoginMessages.invalidCredentials)
    }
}
