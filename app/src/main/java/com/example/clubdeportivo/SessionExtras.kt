package com.example.clubdeportivo

object SessionExtras {
    const val USUARIO = "usuario"
    const val DEFAULT_USUARIO = "Usuario"

    fun nombreUsuario(raw: String?): String =
        raw?.trim()?.takeIf { it.isNotEmpty() } ?: DEFAULT_USUARIO
}
