package com.example.clubdeportivo

import org.junit.Assert.assertEquals
import org.junit.Test

class UsuarioFormTextTest {
    @Test
    fun mensajes_validacionSonClaros() {
        assertEquals("Todos los campos son obligatorios", UsuarioFormText.requiredFields)
        assertEquals("Debe tener 8 o 9 n\u00fameros", UsuarioFormText.invalidDni)
        assertEquals("Us\u00e1 el formato dd/mm/aaaa", UsuarioFormText.invalidBirthDate)
    }

    @Test
    fun mensajes_confirmacionSonConsistentes() {
        assertEquals("Confirmar registro", UsuarioFormText.confirmCreateTitle)
        assertEquals("\u00bfConfirm\u00e1s registrar este usuario?", UsuarioFormText.confirmCreateMessage)
        assertEquals("\u00bfConfirm\u00e1s editar al cliente con DNI 30111222?", UsuarioFormText.confirmEditMessage("30111222"))
    }
}
