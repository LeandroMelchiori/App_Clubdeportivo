package com.example.clubdeportivo

import java.time.LocalDate
import java.time.YearMonth

object PaymentDbRules {
    const val activeClientMissing = "No existe un cliente activo con ese DNI"
    const val disabledPaymentMethod = "El medio de pago no esta habilitado para el club"

    fun cuotaVencimiento(fechaPagoIso: String, dueDay: Int, graceDays: Int): String {
        require(dueDay in 1..28) { "El dia de vencimiento debe estar entre 1 y 28" }
        require(graceDays in 0..31) { "Los dias de gracia deben estar entre 0 y 31" }
        return LocalDate.parse(fechaPagoIso)
            .plusMonths(1)
            .withDayOfMonth(dueDay)
            .plusDays(graceDays.toLong())
            .toString()
    }

    fun configuredPaymentMethod(
        configuration: ClubConfiguration,
        value: String?
    ): ManualPaymentMethod {
        val method = ManualPaymentMethod.fromDisplayName(value)
            ?: throw IllegalArgumentException(disabledPaymentMethod)
        require(method in configuration.enabledPaymentMethods()) { disabledPaymentMethod }
        return method
    }

    fun paymentAlreadyRegistered(lastPaymentIso: String?, date: LocalDate): Boolean =
        runCatching { YearMonth.from(LocalDate.parse(lastPaymentIso)) == YearMonth.from(date) }
            .getOrDefault(false)

    fun cuotaEstadoPagado(): Int = 1
}
