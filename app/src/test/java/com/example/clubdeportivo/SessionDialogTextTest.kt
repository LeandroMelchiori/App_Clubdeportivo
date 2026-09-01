package com.example.clubdeportivo

import org.junit.Assert.assertEquals
import org.junit.Test

class SessionDialogTextTest {
    @Test
    fun logoutMessage_usaPreguntaClara() {
        assertEquals("Cerrar sesi\u00f3n", SessionDialogText.logoutTitle)
        assertEquals("\u00bfEst\u00e1s seguro de que quer\u00e9s cerrar sesi\u00f3n?", SessionDialogText.logoutMessage)
    }

    @Test
    fun acciones_logoutSonBreves() {
        assertEquals("S\u00ed", SessionDialogText.confirm)
        assertEquals("No", SessionDialogText.cancel)
    }
}
