package com.example.clubdeportivo

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Currency
import java.util.Locale

object MoneyFormatter {
    private val spanishSymbols = DecimalFormatSymbols(Locale.forLanguageTag("es-AR"))

    fun format(amount: Double, currency: ClubCurrency): String {
        require(amount.isFinite()) { "El importe debe ser finito" }
        val fractionDigits = Currency.getInstance(currency.code).defaultFractionDigits.coerceAtLeast(0)
        val pattern = buildString {
            append("#,##0")
            if (fractionDigits > 0) {
                append('.')
                repeat(fractionDigits) { append('0') }
            }
        }
        return "${currency.code} ${DecimalFormat(pattern, spanishSymbols).format(amount)}"
    }
}
