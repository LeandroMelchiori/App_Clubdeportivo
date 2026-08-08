package com.example.clubdeportivo

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class VencimientoFiltersTest {
    private val hoy = LocalDate.parse("2026-08-01")
    private val items = listOf(
        DBHelper.VencimientoCard("Ana", "Alvarez", "111", "2026-08-20", "2026-07-20"),
        DBHelper.VencimientoCard("Beto", "Benitez", "222", "2026-08-05", "2026-07-05"),
        DBHelper.VencimientoCard("Carla", "Cruz", "333", "2026-07-20", "2026-06-20")
    )

    @Test
    fun filtrar_todosDevuelveListaOriginal() {
        assertEquals(items, VencimientoFilters.filtrar(items, VencimientoFilters.Tipo.TODOS, hoy))
    }


    @Test
    fun filtrar_alDiaDevuelveSoloSociosSinRiesgoCercano() {
        val filtrados = VencimientoFilters.filtrar(items, VencimientoFilters.Tipo.AL_DIA, hoy)

        assertEquals(listOf("111"), filtrados.map { it.dni })
    }


    @Test
    fun filtrar_porVencerDevuelveSoloCategoria() {
        val filtrados = VencimientoFilters.filtrar(items, VencimientoFilters.Tipo.POR_VENCER, hoy)

        assertEquals(listOf("222"), filtrados.map { it.dni })
    }

    @Test
    fun filtrar_vencidoDevuelveSoloVencidos() {
        val filtrados = VencimientoFilters.filtrar(items, VencimientoFilters.Tipo.VENCIDO, hoy)

        assertEquals(listOf("333"), filtrados.map { it.dni })
    }
}
