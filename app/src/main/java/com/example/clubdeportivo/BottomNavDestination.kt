package com.example.clubdeportivo

object BottomNavDestination {
    const val HOME = "home"
    const val ACTIVIDADES = "actividades"
    const val CONFIGURACION = "configuracion"
    const val LISTADOS = "listados"
    const val PAGOS = "pagos"

    fun keyFor(itemId: Int): String? = when (itemId) {
        R.id.nav_home -> HOME
        R.id.nav_activity -> ACTIVIDADES
        R.id.nav_settings -> CONFIGURACION
        R.id.nav_listas -> LISTADOS
        R.id.nav_pagos -> PAGOS
        else -> null
    }
}
