package com.example.clubdeportivo

object DashboardFormatters {
    fun monto(valor: Double, currency: ClubCurrency): String =
        MoneyFormatter.format(valor, currency)
}
