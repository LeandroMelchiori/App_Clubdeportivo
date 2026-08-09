package com.example.clubdeportivo

object ActividadDisplayFormatter {
    fun textoOpcional(valor: String?): String =
        valor?.trim().takeUnless { it.isNullOrEmpty() } ?: "Sin datos"

    fun mensajeEliminar(nombre: String, etiquetaHorario: String?): String =
        "Se eliminar\u00e1 \"${textoOpcional(nombre)}\" en el horario ${textoOpcional(etiquetaHorario)}. \u00bfContinuar?"
}
