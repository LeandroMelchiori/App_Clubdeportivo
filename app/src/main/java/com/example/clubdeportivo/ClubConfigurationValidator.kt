package com.example.clubdeportivo

object ClubConfigurationValidator {
    enum class Field {
        NAME,
        ADDRESS,
        PHONE,
        EMAIL,
        CURRENCY,
        MONTHLY_FEE,
        DUE_DAY,
        GRACE_DAYS,
        PAYMENT_METHODS
    }

    data class Draft(
        val name: String,
        val address: String,
        val phone: String,
        val email: String,
        val currencyCode: String,
        val monthlyFee: String,
        val dueDay: String,
        val graceDays: String,
        val acceptsCash: Boolean,
        val acceptsTransfer: Boolean,
        val acceptsCard: Boolean,
        val logoUri: String?
    )

    data class Result(
        val configuration: ClubConfiguration? = null,
        val field: Field? = null,
        val error: String? = null
    ) {
        val isValid: Boolean
            get() = configuration != null
    }

    fun validate(draft: Draft): Result {
        val name = draft.name.trim()
        val address = draft.address.trim()
        val phone = draft.phone.trim()
        val email = draft.email.trim()
        val currency = ClubCurrency.entries.firstOrNull { it.code == draft.currencyCode }
            ?: return invalid(Field.CURRENCY, "Selecciona una moneda valida")
        val monthlyFee = parseMoney(draft.monthlyFee)
        val dueDay = draft.dueDay.trim().toIntOrNull()
        val graceDays = draft.graceDays.trim().toIntOrNull()

        if (name.isBlank()) return invalid(Field.NAME, "Ingresa el nombre del club")
        if (name.length > 80) return invalid(Field.NAME, "El nombre no puede superar 80 caracteres")
        if (address.length > 160) return invalid(Field.ADDRESS, "El domicilio no puede superar 160 caracteres")
        if (phone.isNotBlank() && !phone.matches(Regex("^[+0-9 ()-]{6,20}$"))) {
            return invalid(Field.PHONE, "Ingresa un telefono valido")
        }
        if (email.isNotBlank() && !UsuarioValidator.emailValido(email)) {
            return invalid(Field.EMAIL, "Ingresa un email valido")
        }
        if (monthlyFee == null || !monthlyFee.isFinite() || monthlyFee <= 0.0) {
            return invalid(Field.MONTHLY_FEE, "Ingresa una cuota mayor a cero")
        }
        if (dueDay == null || dueDay !in 1..28) {
            return invalid(Field.DUE_DAY, "El vencimiento debe estar entre los dias 1 y 28")
        }
        if (graceDays == null || graceDays !in 0..31) {
            return invalid(Field.GRACE_DAYS, "Los dias de gracia deben estar entre 0 y 31")
        }
        if (!draft.acceptsCash && !draft.acceptsTransfer && !draft.acceptsCard) {
            return invalid(Field.PAYMENT_METHODS, "Selecciona al menos un medio de pago")
        }

        return Result(
            configuration = ClubConfiguration(
                name = name,
                address = address,
                phone = phone,
                email = email,
                currency = currency,
                monthlyFee = monthlyFee,
                dueDay = dueDay,
                graceDays = graceDays,
                acceptsCash = draft.acceptsCash,
                acceptsTransfer = draft.acceptsTransfer,
                acceptsCard = draft.acceptsCard,
                logoUri = draft.logoUri?.trim()?.takeIf { it.isNotEmpty() }
            )
        )
    }

    internal fun parseMoney(value: String): Double? {
        val compact = value.trim().replace(" ", "")
        if (compact.isEmpty() || compact.any { !it.isDigit() && it != '.' && it != ',' }) {
            return null
        }
        if (compact.all(Char::isDigit)) return compact.toDoubleOrNull()

        val separatorKinds = listOf('.', ',').filter(compact::contains)
        if (separatorKinds.size == 1) {
            val separator = separatorKinds.single()
            val parts = compact.split(separator)
            return when {
                parts.size == 2 && parts.all(String::isNotEmpty) && parts[1].length in 1..2 ->
                    "${parts[0]}.${parts[1]}".toDoubleOrNull()
                parts.size >= 2 && isGroupedInteger(parts) ->
                    parts.joinToString("").toDoubleOrNull()
                else -> null
            }
        }

        val decimalSeparator = if (compact.lastIndexOf(',') > compact.lastIndexOf('.')) ',' else '.'
        val groupingSeparator = if (decimalSeparator == ',') '.' else ','
        val integerPart = compact.substringBeforeLast(decimalSeparator)
        val decimals = compact.substringAfterLast(decimalSeparator)
        val integerGroups = integerPart.split(groupingSeparator)
        if (decimals.length !in 1..2 || !decimals.all(Char::isDigit) || !isGroupedInteger(integerGroups)) {
            return null
        }
        return "${integerGroups.joinToString("")}.$decimals".toDoubleOrNull()
    }

    private fun isGroupedInteger(parts: List<String>): Boolean =
        parts.isNotEmpty() &&
            parts.first().length in 1..3 &&
            parts.all(String::isNotEmpty) &&
            parts.all { part -> part.all(Char::isDigit) } &&
            parts.drop(1).all { part -> part.length == 3 }

    private fun invalid(field: Field, message: String): Result =
        Result(field = field, error = message)
}
