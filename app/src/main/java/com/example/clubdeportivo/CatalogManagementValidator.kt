package com.example.clubdeportivo

import java.time.LocalDate

object CatalogActivityValidator {
    data class Value(val name: String, val price: Double)
    data class Result(val value: Value? = null, val field: String? = null, val error: String? = null)

    fun validate(nameInput: String, priceInput: String): Result {
        val name = nameInput.trim()
        if (name.isEmpty()) return Result(field = "name", error = "Ingresa el nombre de la actividad")
        if (name.length > 80) return Result(field = "name", error = "El nombre no puede superar 80 caracteres")
        val price = ClubConfigurationValidator.parseMoney(priceInput)
        if (price == null || !price.isFinite() || price <= 0.0) {
            return Result(field = "price", error = "Ingresa un precio mayor a cero")
        }
        return Result(value = Value(name, price))
    }
}

object ProfessorValidator {
    data class Draft(
        val dni: String,
        val name: String,
        val lastName: String,
        val birthDate: String,
        val phone: String,
        val email: String,
        val address: String,
        val title: String
    )

    data class Value(
        val dni: String,
        val name: String,
        val lastName: String,
        val birthDate: String,
        val phone: String,
        val email: String,
        val address: String,
        val title: String?
    )

    data class Result(val value: Value? = null, val field: String? = null, val error: String? = null)

    fun validate(draft: Draft, today: LocalDate = LocalDate.now()): Result {
        val dni = draft.dni.trim()
        val name = draft.name.trim()
        val lastName = draft.lastName.trim()
        val birthDate = draft.birthDate.trim()
        val phone = draft.phone.trim()
        val email = draft.email.trim()
        val address = draft.address.trim()
        val title = draft.title.trim()

        if (!dni.matches(Regex("^[0-9]{7,9}$"))) {
            return Result(field = "dni", error = "Ingresa un DNI de 7 a 9 digitos")
        }
        if (name.isEmpty()) return Result(field = "name", error = "Ingresa el nombre")
        if (lastName.isEmpty()) return Result(field = "lastName", error = "Ingresa el apellido")
        val parsedBirthDate = runCatching { LocalDate.parse(birthDate) }.getOrNull()
        if (parsedBirthDate == null || parsedBirthDate.isAfter(today)) {
            return Result(field = "birthDate", error = "Ingresa una fecha valida en formato AAAA-MM-DD")
        }
        if (phone.isNotEmpty() && !phone.matches(Regex("^[+0-9 ()-]{6,20}$"))) {
            return Result(field = "phone", error = "Ingresa un telefono valido")
        }
        if (email.isNotEmpty() && !UsuarioValidator.emailValido(email)) {
            return Result(field = "email", error = "Ingresa un email valido")
        }
        if (address.length > 160) return Result(field = "address", error = "El domicilio es demasiado largo")
        if (title.length > 100) return Result(field = "title", error = "El titulo es demasiado largo")

        return Result(
            value = Value(
                dni = dni,
                name = name,
                lastName = lastName,
                birthDate = birthDate,
                phone = phone,
                email = email,
                address = address,
                title = title.ifEmpty { null }
            )
        )
    }
}
