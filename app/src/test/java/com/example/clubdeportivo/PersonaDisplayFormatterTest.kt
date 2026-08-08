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
        assertEquals("Tel\u00e9fono: Sin datos", PersonaDisplayFormatter.etiqueta("Tel\u00e9fono", null))
    }

    @Test
    fun tipoSocio_formateaSocioYNoSocio() {
        assertEquals("Socio nro: 4", PersonaDisplayFormatter.tipoSocio(4, true))
        assertEquals("No socio nro: sin id", PersonaDisplayFormatter.tipoSocio(null, false))
    }
}
