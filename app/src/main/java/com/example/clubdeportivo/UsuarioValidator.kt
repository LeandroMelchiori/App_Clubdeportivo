package com.example.clubdeportivo

import java.text.SimpleDateFormat
import java.util.Locale

object UsuarioValidator {
    private val emailRegex = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

    fun dniValido(dni: String): Boolean = dni.matches(Regex("^\\d{8,9}$"))

    fun telefonoValido(telefono: String): Boolean = telefono.matches(Regex("^\\d{9,12}$"))

    fun emailValido(email: String): Boolean = emailRegex.matches(email)

    fun normalizarFechaNacimiento(input: String): String? {
        if (input.isBlank()) return null
        return try {
            val inFmt = SimpleDateFormat("dd/MM/yyyy", Locale("es", "AR")).apply { isLenient = false }
            val outFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            outFmt.format(inFmt.parse(input)!!)
        } catch (_: Exception) {
            null
        }
    }
}
