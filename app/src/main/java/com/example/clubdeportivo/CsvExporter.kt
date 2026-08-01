package com.example.clubdeportivo

object CsvExporter {
    fun resumenMensual(nombreMes: String, resumen: DBHelper.ResumenPagosMes): String {
        val rows = listOf(
            listOf("Periodo", nombreMes, resumen.anio.toString()),
            listOf("No socios", resumen.cantNoSocios.toString()),
            listOf("Socios", resumen.cantSocios.toString()),
            listOf("Total clientes", resumen.totalClientes.toString()),
            listOf("Monto cuotas", resumen.montoCuotas.toString()),
            listOf("Monto actividades", resumen.montoActividades.toString()),
            listOf("Ingresos totales", resumen.ingresosTotales.toString())
        )
        return rows.joinToString(separator = "\n", postfix = "\n") { row ->
            row.joinToString(",") { escape(it) }
        }
    }

    private fun escape(value: String): String {
        val needsQuotes = value.any { it == ',' || it == '"' || it == '\n' || it == '\r' }
        val escaped = value.replace("\"", "\"\"")
        return if (needsQuotes) "\"$escaped\"" else escaped
    }
}
