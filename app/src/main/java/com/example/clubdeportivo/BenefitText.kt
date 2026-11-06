package com.example.clubdeportivo

import java.math.BigDecimal

object BenefitText {
    fun discountValue(discount: Discount, currency: ClubCurrency): String = when (discount.type) {
        AdjustmentType.PERCENT -> "${percent(discount.value)} %"
        AdjustmentType.FIXED -> MoneyFormatter.format(discount.value, currency)
    }

    fun discountDetail(discount: Discount, currency: ClubCurrency): String = buildString {
        append(discountValue(discount, currency))
        append(" - ")
        append(discount.scope.label)
        if (!discount.active) append(" - Inactivo")
    }

    private fun percent(value: Double): String =
        BigDecimal.valueOf(value).stripTrailingZeros().toPlainString().replace('.', ',')
}
