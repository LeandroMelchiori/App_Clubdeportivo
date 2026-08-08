package com.example.clubdeportivo

object CuentaCorrienteFormatter {
    fun historial(movimientos: List<DBHelper.MovimientoCuenta>): String {
        if (movimientos.isEmpty()) return "Sin movimientos registrados"
        return movimientos.joinToString("\n") { item ->
            "${item.fecha} - ${item.tipo}: $${item.monto} ${item.detalle}".trimEnd()
        }
    }
}
