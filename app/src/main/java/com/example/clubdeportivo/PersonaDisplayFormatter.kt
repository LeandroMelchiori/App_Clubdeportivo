package com.example.clubdeportivo

object PersonaDisplayFormatter {
    fun nombreCompleto(nombre: String?, apellido: String?): String {
        val partes = listOfNotNull(apellido?.limpio(), nombre?.limpio()).filter { it.isNotEmpty() }
        return if (partes.isEmpty()) "Sin nombre" else partes.joinToString(", ")
    }

    fun tipoSocio(id: Int?, esSocio: Boolean): String {
        val numero = id?.toString() ?: "sin id"
        return if (esSocio) "Socio nro: $numero" else "No socio nro: $numero"
    }

    fun etiqueta(label: String, value: String?): String =
        "$label: ${value.limpio().ifEmpty { "Sin datos" }}"

    private fun String?.limpio(): String = this?.trim().orEmpty()
}
