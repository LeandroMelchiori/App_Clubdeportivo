package com.example.clubdeportivo

import org.junit.Assert.assertEquals
import org.junit.Test

class SessionExtrasTest {
    @Test
    fun nombreUsuario_usaValorPorDefectoSiNoHaySesion() {
        assertEquals("Usuario", SessionExtras.nombreUsuario(null))
        assertEquals("Usuario", SessionExtras.nombreUsuario("   "))
    }

    @Test
    fun nombreUsuario_normalizaEspacios() {
        assertEquals("QA", SessionExtras.nombreUsuario("  QA  "))
    }
}
