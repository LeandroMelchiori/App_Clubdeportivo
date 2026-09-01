package com.example.clubdeportivo

import org.junit.Assert.assertEquals
import org.junit.Test

class EmptyStateTextTest {
    @Test
    fun listado_muestraCantidadCuandoHayRegistros() {
        assertEquals("Mostrando 3 registros de socios", EmptyStateText.listado(3, "socios"))
    }

    @Test
    fun listado_muestraFiltroCuandoNoHayResultados() {
        assertEquals("Sin resultados de socios para \"ana\"", EmptyStateText.listado(0, "socios", "ana"))
    }

    @Test
    fun actividades_muestraEstadoVacioInicial() {
        assertEquals("Todav\u00eda no hay actividades cargadas", EmptyStateText.actividades(0))
    }
}
