package com.example.clubdeportivo

import java.time.LocalDate

enum class AdjustmentType(val code: String, val label: String) {
    PERCENT("percent", "Porcentaje"),
    FIXED("fixed", "Monto fijo");

    companion object {
        fun fromCode(code: String?): AdjustmentType =
            entries.firstOrNull { it.code == code } ?: PERCENT
    }
}

enum class LateFeeFrequency(val code: String, val label: String) {
    ONCE("once", "Una vez por cuota vencida"),
    MONTHLY("monthly", "Por mes o fraccion de atraso");

    companion object {
        fun fromCode(code: String?): LateFeeFrequency =
            entries.firstOrNull { it.code == code } ?: ONCE
    }
}

enum class BenefitStacking(val code: String, val label: String) {
    BEST_ONLY("best", "Solo el beneficio mayor"),
    COMBINED("combined", "Sumar descuento y beca");

    companion object {
        fun fromCode(code: String?): BenefitStacking =
            entries.firstOrNull { it.code == code } ?: BEST_ONLY
    }
}

enum class ChargeKind {
    FEE,
    ACTIVITY
}

enum class ChargeScope(val code: String, val label: String) {
    FEE("fee", "Cuota"),
    ACTIVITIES("activities", "Actividades"),
    BOTH("both", "Cuota y actividades");

    fun covers(kind: ChargeKind): Boolean = when (this) {
        FEE -> kind == ChargeKind.FEE
        ACTIVITIES -> kind == ChargeKind.ACTIVITY
        BOTH -> true
    }

    companion object {
        fun fromCode(code: String?): ChargeScope =
            entries.firstOrNull { it.code == code } ?: BOTH
    }
}

data class BillingRules(
    val lateFeeType: AdjustmentType = AdjustmentType.PERCENT,
    val lateFeeValue: Double = 0.0,
    val lateFeeFrequency: LateFeeFrequency = LateFeeFrequency.ONCE,
    val lateFeeMaxMonths: Int = 3,
    val benefitStacking: BenefitStacking = BenefitStacking.BEST_ONLY,
    val scholarshipWaivesLateFee: Boolean = true,
    val lateLosesDiscount: Boolean = false
) {
    val lateFeeEnabled: Boolean
        get() = lateFeeValue > 0.0

    companion object {
        val DEFAULT = BillingRules()
    }
}

data class Discount(
    val id: Long,
    val name: String,
    val type: AdjustmentType,
    val value: Double,
    val scope: ChargeScope,
    val active: Boolean = true
)

data class Scholarship(
    val percent: Int,
    val scope: ChargeScope,
    val reason: String,
    val until: LocalDate? = null
) {
    fun appliesOn(date: LocalDate, kind: ChargeKind): Boolean =
        scope.covers(kind) && (until == null || !date.isAfter(until))
}

data class ClientBenefits(
    val discount: Discount? = null,
    val scholarship: Scholarship? = null
) {
    val isEmpty: Boolean
        get() = discount == null && scholarship == null

    companion object {
        val NONE = ClientBenefits()
    }
}
