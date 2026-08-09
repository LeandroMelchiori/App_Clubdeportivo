package com.example.clubdeportivo

enum class ClubCurrency(val code: String, val label: String) {
    ARS("ARS", "Peso argentino (ARS)"),
    USD("USD", "Dolar estadounidense (USD)"),
    UYU("UYU", "Peso uruguayo (UYU)"),
    CLP("CLP", "Peso chileno (CLP)"),
    PYG("PYG", "Guarani paraguayo (PYG)"),
    BRL("BRL", "Real brasileno (BRL)");

    companion object {
        fun fromCode(code: String?): ClubCurrency =
            entries.firstOrNull { it.code == code } ?: ARS
    }
}

data class ClubConfiguration(
    val name: String,
    val address: String,
    val phone: String,
    val email: String,
    val currency: ClubCurrency,
    val monthlyFee: Double,
    val dueDay: Int,
    val graceDays: Int,
    val acceptsCash: Boolean,
    val acceptsTransfer: Boolean,
    val acceptsCard: Boolean,
    val logoUri: String?
) {
    companion object {
        val DEFAULT = ClubConfiguration(
            name = "Club deportivo",
            address = "",
            phone = "",
            email = "",
            currency = ClubCurrency.ARS,
            monthlyFee = 30000.0,
            dueDay = 10,
            graceDays = 0,
            acceptsCash = true,
            acceptsTransfer = true,
            acceptsCard = true,
            logoUri = null
        )
    }
}
