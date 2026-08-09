package com.example.clubdeportivo

import org.junit.Assert.assertEquals
import org.junit.Test

class ListadoExportFilterTest {
    @Test
    fun noSocios_filtraPorNombreOApellido() {
        val items = listOf(
            DBHelper.NoSocioCard("Ana", "Lopez", "1", null, null),
            DBHelper.NoSocioCard("Beto", "Garcia", "2", null, null)
        )

        val result = ListadoExportFilter.noSocios(items, "gar")

        assertEquals(listOf("2"), result.map { it.dni })
    }

    @Test
    fun socios_sinBusquedaDevuelveTodo() {
        val items = listOf(DBHelper.SocioCard("Ana", "Lopez", "1", null))

        assertEquals(items, ListadoExportFilter.socios(items, "  "))
    }

    @Test
    fun vencimientos_respetaFiltroDeEstadoYBusqueda() {
        val items = listOf(
            DBHelper.VencimientoCard("Ana", "Lopez", "1", "2026-08-20", null),
            DBHelper.VencimientoCard("Beto", "Garcia", "2", "2026-07-20", null)
        )

        val result = ListadoExportFilter.vencimientos(items, "gar", VencimientoFilters.Tipo.VENCIDO)

        assertEquals(listOf("2"), result.map { it.dni })
    }
}
