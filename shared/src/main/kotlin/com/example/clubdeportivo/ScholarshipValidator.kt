package com.example.clubdeportivo

import java.time.LocalDate

object ScholarshipValidator {
    enum class Field {
        PERCENT,
        REASON,
        UNTIL
    }

    data class Result(
        val scholarship: Scholarship? = null,
        val field: Field? = null,
        val error: String? = null
    )

    fun validate(
        percentInput: String,
        scope: ChargeScope,
        reasonInput: String,
        untilInput: String,
        today: LocalDate = LocalDate.now()
    ): Result {
        val percent = percentInput.trim().toIntOrNull()
        if (percent == null || percent !in 1..100) {
            return Result(field = Field.PERCENT, error = "Ingresa un porcentaje entre 1 y 100")
        }
        val reason = reasonInput.trim()
        if (reason.isEmpty()) return Result(field = Field.REASON, error = "Ingresa el motivo de la beca")
        if (reason.length > 120) {
            return Result(field = Field.REASON, error = "El motivo no puede superar 120 caracteres")
        }
        val untilText = untilInput.trim()
        val until = if (untilText.isEmpty()) {
            null
        } else {
            runCatching { LocalDate.parse(untilText) }.getOrNull()
                ?: return Result(field = Field.UNTIL, error = "Ingresa una fecha valida en formato AAAA-MM-DD")
        }
        if (until != null && until.isBefore(today)) {
            return Result(field = Field.UNTIL, error = "La fecha de fin no puede ser anterior a hoy")
        }
        return Result(scholarship = Scholarship(percent, scope, reason, until))
    }
}
