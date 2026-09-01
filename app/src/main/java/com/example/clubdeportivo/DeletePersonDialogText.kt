package com.example.clubdeportivo

object DeletePersonDialogText {
    const val title = "Eliminar registro"
    const val confirm = "Eliminar"
    const val cancel = "Cancelar"

    fun message(nombreCompleto: String, dni: String): String {
        val nombre = nombreCompleto.ifBlank { "esta persona" }
        val dniTexto = dni.ifBlank { "sin datos" }
        return "Vas a eliminar a $nombre (DNI $dniTexto). Esta acci\u00f3n no se puede deshacer."
    }
}
