package com.example.clubdeportivo

enum class ManualPaymentMethod(val displayName: String) {
    CASH("Efectivo"),
    TRANSFER("Transferencia"),
    CARD("Tarjeta");

    companion object {
        fun fromDisplayName(value: String?): ManualPaymentMethod? {
            val normalized = value?.trim().orEmpty()
            return entries.firstOrNull { method ->
                method.displayName.equals(normalized, ignoreCase = true) ||
                    method == CARD && normalized.equals("Tarjeta de credito", ignoreCase = true)
            }
        }
    }
}

fun ClubConfiguration.enabledPaymentMethods(): List<ManualPaymentMethod> = buildList {
    if (acceptsCash) add(ManualPaymentMethod.CASH)
    if (acceptsTransfer) add(ManualPaymentMethod.TRANSFER)
    if (acceptsCard) add(ManualPaymentMethod.CARD)
}
