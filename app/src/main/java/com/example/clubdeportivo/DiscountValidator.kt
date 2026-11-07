package com.example.clubdeportivo

object DiscountValidator {
    enum class Field {
        NAME,
        VALUE
    }

    data class Result(
        val discount: Discount? = null,
        val field: Field? = null,
        val error: String? = null
    )

    fun validate(
        id: Long,
        nameInput: String,
        type: AdjustmentType,
        valueInput: String,
        scope: ChargeScope,
        active: Boolean
    ): Result {
        val name = nameInput.trim()
        if (name.isEmpty()) return Result(field = Field.NAME, error = "Ingresa el nombre del descuento")
        if (name.length > 60) {
            return Result(field = Field.NAME, error = "El nombre no puede superar 60 caracteres")
        }
        val value = ClubConfigurationValidator.parseMoney(valueInput)
        if (value == null || !value.isFinite() || value <= 0.0) {
            return Result(field = Field.VALUE, error = "Ingresa un valor mayor a cero")
        }
        if (type == AdjustmentType.PERCENT && value > 100.0) {
            return Result(field = Field.VALUE, error = "El porcentaje no puede superar 100")
        }
        return Result(discount = Discount(id, name, type, value, scope, active))
    }
}
