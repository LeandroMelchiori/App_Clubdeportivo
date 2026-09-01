package com.example.clubdeportivo

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LoginCredentialsTest {
    @Test
    fun sonValidas_aceptaUsuariosDePrueba() {
        assertTrue(LoginCredentials.sonValidas("admin", "admin"))
        assertTrue(LoginCredentials.sonValidas("charlie", "charlie"))
    }

    @Test
    fun sonValidas_rechazaPasswordIncorrecta() {
        assertFalse(LoginCredentials.sonValidas("admin", "1234"))
    }

    @Test
    fun sonValidas_rechazaCamposVacios() {
        assertFalse(LoginCredentials.sonValidas("", ""))
    }
}
