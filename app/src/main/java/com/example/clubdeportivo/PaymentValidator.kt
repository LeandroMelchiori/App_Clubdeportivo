package com.example.clubdeportivo

object PaymentValidator {
    data class Result(val isValid: Boolean, val error: String? = null)

    fun validateManualPayment(amount: Double?, method: String?): Result {
        if (method.isNullOrBlank()) {
            return Result(false, "Debe seleccionar una forma de pago")
        }
        if (amount == null || amount <= 0.0) {
            return Result(false, "Monto invalido")
        }
        return Result(true)
    }
}
