package com.example.clubdeportivo

object HorarioFormOptions {
    val diasCortos = listOf("Dom", "Lun", "Mar", "Mie", "Jue", "Vie", "Sab")
    val diasLargos = listOf("Domingo", "Lunes", "Martes", "Miercoles", "Jueves", "Viernes", "Sabado")

    fun minutosCada(step: Int = 30, desde: Int = 0, hasta: Int = 24 * 60 - step): List<Int> {
        require(step > 0) { "El intervalo debe ser positivo" }
        require(desde >= 0 && hasta >= desde) { "El rango horario no es valido" }
        return (desde..hasta step step).toList()
    }

    fun etiquetas(minutos: List<Int>): List<String> = minutos.map(ClubFormatters::hhmm)

    fun posicionMasCercana(opciones: List<Int>, valor: Int): Int {
        if (opciones.isEmpty()) return 0
        val exacta = opciones.indexOf(valor)
        return if (exacta >= 0) exacta else opciones.indexOfLast { it <= valor }.coerceAtLeast(0)
    }

    fun slots30Min(startHour: Int = 6, endHour: Int = 23, includeEndHalf: Boolean = true): List<String> {
        val desde = startHour * 60
        val hasta = endHour * 60 + if (includeEndHalf) 30 else 0
        return etiquetas(minutosCada(step = 30, desde = desde, hasta = hasta))
    }

    fun hhmmToMin(hhmm: String): Int {
        val parts = hhmm.split(":")
        require(parts.size == 2) { "La hora debe tener formato HH:mm" }
        val horas = parts[0].toInt()
        val minutos = parts[1].toInt()
        require(horas in 0..23 && minutos in 0..59) { "La hora debe estar dentro del dia" }
        return horas * 60 + minutos
    }
}
