package com.example.clubdeportivo

object LoginCredentials {
    private val usuariosValidos = setOf("admin", "charlie", "sacha", "javo", "heber")

    fun sonValidas(usuario: String, contrasena: String): Boolean {
        val usuarioLimpio = usuario.trim()
        val contrasenaLimpia = contrasena.trim()
        return usuarioLimpio in usuariosValidos && usuarioLimpio == contrasenaLimpia
    }
}
