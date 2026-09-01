package com.example.clubdeportivo

object UsuarioFormValidation {
    enum class Field { NOMBRE, APELLIDO, FECHA_NACIMIENTO, DNI, DIRECCION, TELEFONO, EMAIL }

    fun firstMissing(
        nombre: String,
        apellido: String,
        fechaNacimiento: String,
        dni: String,
        direccion: String,
        telefono: String,
        email: String
    ): Field? = when {
        nombre.isBlank() -> Field.NOMBRE
        apellido.isBlank() -> Field.APELLIDO
        fechaNacimiento.isBlank() -> Field.FECHA_NACIMIENTO
        dni.isBlank() -> Field.DNI
        direccion.isBlank() -> Field.DIRECCION
        telefono.isBlank() -> Field.TELEFONO
        email.isBlank() -> Field.EMAIL
        else -> null
    }
}
