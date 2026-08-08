package com.example.clubdeportivo

import org.junit.Assert.assertEquals
import org.junit.Test

class PersonaDisplayFormatterTest {
    @Test
    fun nombreCompleto_usaApellidoYNombreSinNullVisible() {
        assertEquals("Perez, Ana", PersonaDisplayFormatter.nombreCompleto(" Ana ", " Perez "))
        assertEquals("Sin nombre", PersonaDisplayFormatter.nombreCompleto(null, "  "))
    }

    @Test
    fun etiqueta_muestraSinDatosParaCamposVacios() {
        assertEquals("Telefono: Sin datos", PersonaDisplayFormatter.etiqueta("Telefono", null))
    }

    @Test
    fun tipoSocio_formateaSocioYNoSocio() {
        assertEquals("Socio nro: 4", PersonaDisplayFormatter.tipoSocio(4, true))
        assertEquals("No socio nro: sin id", PersonaDisplayFormatter.tipoSocio(null, false))
    }
}
