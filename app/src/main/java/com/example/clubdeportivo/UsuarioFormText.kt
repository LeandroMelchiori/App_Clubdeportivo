package com.example.clubdeportivo

object UsuarioFormText {
    const val requiredFields = "Todos los campos son obligatorios"
    const val invalidDni = "Debe tener 8 o 9 n\u00fameros"
    const val invalidPhone = "Debe tener entre 9 y 12 n\u00fameros"
    const val invalidEmail = "Correo inv\u00e1lido"
    const val invalidBirthDate = "Us\u00e1 el formato dd/mm/aaaa"
    const val duplicatedDni = "El DNI ya est\u00e1 registrado"
    const val confirmCreateTitle = "Confirmar registro"
    const val confirmCreateMessage = "\u00bfConfirm\u00e1s registrar este usuario?"
    const val confirmEditTitle = "Confirmar edici\u00f3n"
    fun confirmEditMessage(dni: String): String = "\u00bfConfirm\u00e1s editar al cliente con DNI $dni?"
}
