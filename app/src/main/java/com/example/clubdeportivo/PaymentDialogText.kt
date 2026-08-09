package com.example.clubdeportivo

object PaymentDialogText {
    const val confirmTitle = "Confirmar pago"
    const val confirmActivityTitle = "Confirmar pago de actividad"
    const val confirm = "Si"
    const val cancel = "Cancelar"
    const val quotaSuccess = "Pago registrado"

    fun cuota(
        monto: Double,
        medioPago: String,
        currency: ClubCurrency
    ): String =
        "Confirmas registrar el pago de ${MoneyFormatter.format(monto, currency)} por \"$medioPago\"?"

    fun convertirNoSocio(
        monto: Double,
        medioPago: String,
        nombre: String,
        currency: ClubCurrency
    ): String =
        "Confirmas registrar el pago de ${MoneyFormatter.format(monto, currency)} por \"$medioPago\" y convertir a $nombre en socio?"

    fun actividad(
        monto: Double,
        actividad: String,
        medioPago: String,
        currency: ClubCurrency
    ): String =
        "Confirmas registrar el pago de ${MoneyFormatter.format(monto, currency)} por la actividad $actividad mediante $medioPago?"

    fun socioCreado(idSocio: Int?): String =
        "Pago registrado. Ahora es socio (id ${idSocio ?: "sin id"})"
}
