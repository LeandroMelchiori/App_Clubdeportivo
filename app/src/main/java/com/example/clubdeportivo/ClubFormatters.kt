package com.example.clubdeportivo

import java.time.LocalDate

object ClubFormatters {
    fun etiquetaDia(dia: Int): String = when (dia) {
        0 -> "Dom"
        1 -> "Lun"
        2 -> "Mar"
        3 -> "Mie"
        4 -> "Jue"
        5 -> "Vie"
        6 -> "Sab"
        else -> "Dia $dia"
    }

    fun nombreDia(dia: Int): String = when (dia) {
        0 -> "Domingo"
        1 -> "Lunes"
        2 -> "Martes"
        3 -> "Miercoles"
        4 -> "Jueves"
        5 -> "Viernes"
        6 -> "Sabado"
        else -> dia.toString()
    }

    fun hhmm(minutos: Int): String {
        require(minutos >= 0) { "Los minutos no pueden ser negativos" }
        return String.format("%02d:%02d", minutos / 60, minutos % 60)
    }

    fun proximoVencimiento(fechaPagoIso: String): String =
        LocalDate.parse(fechaPagoIso).plusMonths(1).toString()
}
