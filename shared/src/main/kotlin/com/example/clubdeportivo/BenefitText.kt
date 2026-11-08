package com.example.clubdeportivo

import java.math.BigDecimal
import java.time.LocalDate

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

    fun clientSummary(benefits: ClientBenefits, currency: ClubCurrency, today: LocalDate): String {
        if (benefits.isEmpty) return "Beneficios: ninguno"
        val lines = buildList {
            benefits.scholarship?.let { scholarship ->
                add(
                    buildString {
                        append("Beca ${scholarship.percent} % (${scholarship.scope.label})")
                        scholarship.until?.let { until ->
                            append(if (until.isBefore(today)) " - vencida el $until" else " hasta $until")
                        }
                        append(": ${scholarship.reason}")
                    }
                )
            }
            benefits.discount?.let { discount ->
                add("Descuento ${discount.name}: ${discountDetail(discount, currency)}")
            }
        }
        return "Beneficios:\n" + lines.joinToString("\n")
    }

    private fun percent(value: Double): String =
        BigDecimal.valueOf(value).stripTrailingZeros().toPlainString().replace('.', ',')
}
