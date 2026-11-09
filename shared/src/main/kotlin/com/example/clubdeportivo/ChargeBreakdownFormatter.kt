package com.example.clubdeportivo

object ChargeBreakdownFormatter {
    fun lines(
        breakdown: ChargeBreakdown,
        kind: ChargeKind,
        currency: ClubCurrency
    ): List<String> = buildList {
        val baseLabel = if (kind == ChargeKind.FEE) "Cuota base" else "Precio base"
        add("$baseLabel: ${MoneyFormatter.format(breakdown.base, currency)}")
        breakdown.scholarshipPercent?.let { percent ->
            add("Beca $percent %: -${MoneyFormatter.format(breakdown.scholarshipAmount, currency)}")
        }
        breakdown.discountName?.let { name ->
            add("Descuento $name: -${MoneyFormatter.format(breakdown.discountAmount, currency)}")
        }
        if (breakdown.lateFeeWaived) {
            add("Recargo por mora: exento por beca")
        } else if (breakdown.lateFeePeriods > 0) {
            add("${lateFeeLabel(breakdown.lateFeePeriods)}: ${MoneyFormatter.format(breakdown.lateFee, currency)}")
        }
        add("Total a cobrar: ${MoneyFormatter.format(breakdown.total, currency)}")
    }

    fun text(breakdown: ChargeBreakdown, kind: ChargeKind, currency: ClubCurrency): String =
        lines(breakdown, kind, currency).joinToString("\n")

    fun summary(breakdown: ChargeBreakdown): String? {
        val parts = buildList {
            breakdown.scholarshipPercent?.let { add("beca $it %") }
            breakdown.discountName?.let { add("descuento $it") }
            if (breakdown.lateFeeWaived) add("recargo exento por beca")
            if (breakdown.lateFeePeriods > 0) add(lateFeeLabel(breakdown.lateFeePeriods).lowercase())
        }
        return parts.takeIf { it.isNotEmpty() }?.joinToString(", ")
    }

    private fun lateFeeLabel(periods: Int): String =
        if (periods == 1) "Recargo por mora" else "Recargo por mora ($periods meses)"
}
