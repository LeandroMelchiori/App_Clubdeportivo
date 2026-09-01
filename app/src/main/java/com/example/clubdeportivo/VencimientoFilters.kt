package com.example.clubdeportivo

import java.time.LocalDate

object VencimientoFilters {
    enum class Tipo(val categoria: String?) {
        TODOS(null),
        AL_DIA("Al dia"),
        POR_VENCER("Por vencer"),
        VENCIDO("Vencido")
    }

    fun filtrar(
        items: List<DBHelper.VencimientoCard>,
        tipo: Tipo,
        hoy: LocalDate = LocalDate.now()
    ): List<DBHelper.VencimientoCard> {
        val categoria = tipo.categoria ?: return items
        return items.filter { item ->
            VencimientoCalculator.clasificar(item.fechaVenc, hoy).categoria == categoria
        }
    }
}
