package com.example.clubdeportivo

import org.junit.Assert.assertEquals
import org.junit.Test

class ActividadDisplayFormatterTest {
    @Test
    fun textoOpcional_devuelveSinDatosParaNulosOVacios() {
        assertEquals("Sin datos", ActividadDisplayFormatter.textoOpcional(null))
        assertEquals("Sin datos", ActividadDisplayFormatter.textoOpcional("   "))
    }

    @Test
    fun textoOpcional_conservaValorTrimmeado() {
        assertEquals("Natacion", ActividadDisplayFormatter.textoOpcional(" Natacion "))
    }

    @Test
    fun mensajeEliminar_armaCopiaClara() {
        assertEquals(
            "Se eliminar\u00e1 \"Funcional\" en el horario Lun 08:00-09:00. \u00bfContinuar?",
            ActividadDisplayFormatter.mensajeEliminar("Funcional", "Lun 08:00-09:00")
        )
    }
}
