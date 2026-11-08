package com.example.clubdeportivo

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.temporal.ChronoUnit

data class ChargeBreakdown(
    val base: Double,
    val discountName: String? = null,
    val discountAmount: Double = 0.0,
    val scholarshipPercent: Int? = null,
    val scholarshipAmount: Double = 0.0,
    val lateFee: Double = 0.0,
    val lateFeePeriods: Int = 0,
    val lateFeeWaived: Boolean = false,
    val total: Double = base
) {
    val benefitsAmount: Double
        get() = discountAmount + scholarshipAmount

    val hasAdjustments: Boolean
        get() = discountName != null || scholarshipPercent != null || lateFeePeriods > 0 || lateFeeWaived

    val isFree: Boolean
        get() = total == 0.0
}

object ChargeCalculator {
    fun calculate(
        base: Double,
        kind: ChargeKind,
        benefits: ClientBenefits,
        rules: BillingRules,
        currency: ClubCurrency,
        paymentDate: LocalDate,
        dueDate: LocalDate? = null
    ): ChargeBreakdown {
        require(base >= 0.0 && base.isFinite()) { "El importe base debe ser valido" }
        val scale = MoneyFormatter.fractionDigits(currency)
        val baseAmount = BigDecimal.valueOf(base).setScale(scale, RoundingMode.HALF_UP)

        val overdue = kind == ChargeKind.FEE && dueDate != null && paymentDate.isAfter(dueDate)
        val discount = benefits.discount?.takeIf {
            it.active && it.scope.covers(kind) && !(overdue && rules.lateLosesDiscount)
        }
        val scholarship = benefits.scholarship?.takeIf { it.appliesOn(paymentDate, kind) }

        var scholarshipAmount = scholarship
            ?.let { percentOf(baseAmount, it.percent.toDouble(), scale) }
            ?: BigDecimal.ZERO.setScale(scale)
        var discountAmount = discount
            ?.let { adjustmentOf(it.type, it.value, baseAmount, scale).min(baseAmount - scholarshipAmount) }
            ?: BigDecimal.ZERO.setScale(scale)
        var appliedDiscount = discount
        var appliedScholarship = scholarship

        if (rules.benefitStacking == BenefitStacking.BEST_ONLY && discount != null && scholarship != null) {
            val fullDiscount = adjustmentOf(discount.type, discount.value, baseAmount, scale).min(baseAmount)
            if (scholarshipAmount >= fullDiscount) {
                appliedDiscount = null
                discountAmount = BigDecimal.ZERO.setScale(scale)
            } else {
                appliedScholarship = null
                scholarshipAmount = BigDecimal.ZERO.setScale(scale)
                discountAmount = fullDiscount
            }
        }

        val subtotal = baseAmount - scholarshipAmount - discountAmount
        var lateFee = BigDecimal.ZERO.setScale(scale)
        var periods = 0
        var waived = false
        if (overdue && rules.lateFeeEnabled) {
            // La exencion depende de que el cliente tenga una beca vigente para la cuota,
            // aunque con "solo el mayor" se haya aplicado el descuento en su lugar.
            if (rules.scholarshipWaivesLateFee && scholarship != null) {
                waived = true
            } else {
                periods = when (rules.lateFeeFrequency) {
                    LateFeeFrequency.ONCE -> 1
                    LateFeeFrequency.MONTHLY ->
                        monthsStarted(dueDate!!, paymentDate).coerceAtMost(rules.lateFeeMaxMonths)
                }
                val perPeriod = adjustmentOf(rules.lateFeeType, rules.lateFeeValue, subtotal, scale)
                lateFee = perPeriod.multiply(BigDecimal.valueOf(periods.toLong()))
            }
        }

        return ChargeBreakdown(
            base = baseAmount.toDouble(),
            discountName = appliedDiscount?.name,
            discountAmount = discountAmount.toDouble(),
            scholarshipPercent = appliedScholarship?.percent,
            scholarshipAmount = scholarshipAmount.toDouble(),
            lateFee = lateFee.toDouble(),
            lateFeePeriods = periods,
            lateFeeWaived = waived,
            total = (subtotal + lateFee).toDouble()
        )
    }

    internal fun monthsStarted(dueDate: LocalDate, paymentDate: LocalDate): Int {
        if (!paymentDate.isAfter(dueDate)) return 0
        val complete = ChronoUnit.MONTHS.between(dueDate, paymentDate)
        val started = if (dueDate.plusMonths(complete).isBefore(paymentDate)) complete + 1 else complete
        return started.toInt()
    }

    private fun adjustmentOf(
        type: AdjustmentType,
        value: Double,
        amount: BigDecimal,
        scale: Int
    ): BigDecimal = when (type) {
        AdjustmentType.PERCENT -> percentOf(amount, value, scale)
        AdjustmentType.FIXED -> BigDecimal.valueOf(value).setScale(scale, RoundingMode.HALF_UP)
    }

    private fun percentOf(amount: BigDecimal, percent: Double, scale: Int): BigDecimal =
        amount.multiply(BigDecimal.valueOf(percent))
            .divide(BigDecimal(100), scale, RoundingMode.HALF_UP)
}
