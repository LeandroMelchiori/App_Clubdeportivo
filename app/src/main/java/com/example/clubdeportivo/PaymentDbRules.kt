package com.example.clubdeportivo

object PaymentDbRules {
    const val activeClientMissing = "No existe un cliente activo con ese DNI"

    fun cuotaVencimiento(fechaPagoIso: String): String = ClubFormatters.proximoVencimiento(fechaPagoIso)

    fun cuotaEstadoPagado(): Int = 1
}
