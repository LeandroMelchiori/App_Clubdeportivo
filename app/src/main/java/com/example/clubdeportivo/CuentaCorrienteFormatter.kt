package com.example.clubdeportivo

object CuentaCorrienteFormatter {
    enum class Filtro(val tipo: String?) {
        TODOS(null),
        CUOTAS("Cuota"),
        ACTIVIDADES("Actividad")
    }

    fun filtrar(
        movimientos: List<DBHelper.MovimientoCuenta>,
        filtro: Filtro
    ): List<DBHelper.MovimientoCuenta> {
        val tipo = filtro.tipo ?: return movimientos
        return movimientos.filter { it.tipo.equals(tipo, ignoreCase = true) }
    }

    fun historial(
        movimientos: List<DBHelper.MovimientoCuenta>,
        currency: ClubCurrency,
        filtro: Filtro = Filtro.TODOS
    ): String = historial(filtrar(movimientos, filtro), currency)

    private fun historial(
        movimientos: List<DBHelper.MovimientoCuenta>,
        currency: ClubCurrency
    ): String {
        if (movimientos.isEmpty()) return "Sin movimientos registrados"
        return movimientos.joinToString("\n") { item ->
            "${item.fecha} - ${item.tipo}: ${MoneyFormatter.format(item.monto, currency)} ${item.detalle}".trimEnd()
        }
    }
}
