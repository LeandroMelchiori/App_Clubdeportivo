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
        return rowsToCsv(rows)
    }

    fun noSocios(items: List<DBHelper.NoSocioCard>): String {
        val rows = listOf(listOf("Apellido", "Nombre", "DNI", "\u00daltima actividad", "Fecha ultimo pago")) +
            items.map { item ->
                listOf(
                    item.apellido,
                    item.nombre,
                    item.dni,
                    item.nombreAct.orEmpty(),
                    item.ultimaPago.orEmpty()
                )
            }
        return rowsToCsv(rows)
    }

    fun socios(items: List<DBHelper.SocioCard>): String {
        val rows = listOf(listOf("Apellido", "Nombre", "DNI", "\u00daltimo pago")) +
            items.map { item ->
                listOf(item.apellido, item.nombre, item.dni, item.ultimoPago.orEmpty())
            }
        return rowsToCsv(rows)
    }

    fun vencimientos(items: List<DBHelper.VencimientoCard>): String {
        val rows = listOf(listOf("Apellido", "Nombre", "DNI", "Vencimiento", "\u00daltimo pago", "Estado")) +
            items.map { item ->
                listOf(
                    item.apellido,
                    item.nombre,
                    item.dni,
                    item.fechaVenc,
                    item.ultimoPago.orEmpty(),
                    VencimientoCalculator.clasificar(item.fechaVenc).categoria
                )
            }
        return rowsToCsv(rows)
    }

    private fun rowsToCsv(rows: List<List<String>>): String =
        rows.joinToString(separator = "\n", postfix = "\n") { row ->
            row.joinToString(",") { escape(it) }
        }

    private fun escape(value: String): String {
        val needsQuotes = value.any { it == ',' || it == '"' || it == '\n' || it == '\r' }
        val escaped = value.replace("\"", "\"\"")
        return if (needsQuotes) "\"$escaped\"" else escaped
    }
}
