package com.example.clubdeportivo

object EmptyStateText {
    fun listado(cantidad: Int, tipo: String, filtro: String = ""): String {
        val tipoLimpio = tipo.trim()
        val filtroLimpio = filtro.trim()
        return when {
            cantidad > 0 -> "Mostrando $cantidad registros de $tipoLimpio"
            filtroLimpio.isNotEmpty() -> "Sin resultados de $tipoLimpio para \"$filtroLimpio\""
            else -> "Sin registros de $tipoLimpio"
        }
    }

    fun actividades(cantidad: Int, filtro: String = ""): String {
        val filtroLimpio = filtro.trim()
        return when {
            cantidad > 0 -> "Mostrando $cantidad actividades"
            filtroLimpio.isNotEmpty() -> "Sin actividades para \"$filtroLimpio\""
            else -> "Todav\u00eda no hay actividades cargadas"
        }
    }
}
