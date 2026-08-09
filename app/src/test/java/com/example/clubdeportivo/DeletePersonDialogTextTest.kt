package com.example.clubdeportivo

import org.junit.Assert.assertEquals
import org.junit.Test

class DeletePersonDialogTextTest {
    @Test
    fun message_incluyeNombreYDni() {
        assertEquals(
            "Vas a eliminar a Perez, Ana (DNI 30111222). Esta acci\u00f3n no se puede deshacer.",
            DeletePersonDialogText.message("Perez, Ana", "30111222")
        )
    }
}
