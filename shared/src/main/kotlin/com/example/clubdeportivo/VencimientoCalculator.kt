package com.example.clubdeportivo

import java.time.LocalDate
import java.time.temporal.ChronoUnit

object VencimientoCalculator {
    fun clasificar(fechaVencimientoIso: String, hoy: LocalDate = LocalDate.now()): EstadoVencimiento {
        val vencimiento = LocalDate.parse(fechaVencimientoIso)
        val dias = ChronoUnit.DAYS.between(hoy, vencimiento)
        return when {
            dias < 0 -> EstadoVencimiento("Vencido", "Debe hace ${-dias} dias")
            dias == 0L -> EstadoVencimiento("Por vencer", "Vence hoy")
            dias <= 7 -> EstadoVencimiento("Por vencer", "Vence en $dias dias")
            else -> EstadoVencimiento("Al dia", "Vence en $dias dias")
        }
    }
}

data class EstadoVencimiento(
    val categoria: String,
    val texto: String
)
