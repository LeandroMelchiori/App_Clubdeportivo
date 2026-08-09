package com.example.clubdeportivo

object PaymentDialogText {
    const val confirmTitle = "Confirmar pago"
    const val confirmActivityTitle = "Confirmar pago de actividad"
    const val confirm = "S\u00ed"
    const val cancel = "Cancelar"
    const val quotaSuccess = "\u00a1Pago exitoso!"

    fun cuota(monto: Double, medioPago: String): String =
        "\u00bfConfirm\u00e1s registrar el pago de $${formatAmount(monto)} por \"$medioPago\"?"

    fun convertirNoSocio(monto: Double, medioPago: String, nombre: String): String =
        "\u00bfConfirm\u00e1s registrar el pago de $${formatAmount(monto)} por \"$medioPago\" y convertir a $nombre en socio?"

    fun actividad(monto: Double, actividad: String): String =
        "\u00bfConfirm\u00e1s registrar el pago de $${formatAmount(monto)} por la actividad $actividad?"

    fun socioCreado(idSocio: Int?): String = "\u00a1Pago exitoso! Ahora es socio (id ${idSocio ?: "sin id"})"

    private fun formatAmount(monto: Double): String =
        if (monto % 1.0 == 0.0) monto.toLong().toString() else "%.2f".format(monto)
}
