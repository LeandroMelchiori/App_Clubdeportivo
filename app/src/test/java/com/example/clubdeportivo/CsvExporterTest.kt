package com.example.clubdeportivo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CsvExporterTest {
    @Test
    fun resumenMensual_generaCsvConTotales() {
        val resumen = DBHelper.ResumenPagosMes(
            anio = 2026,
            mes = 8,
            cantNoSocios = 3,
            cantSocios = 7,
            totalClientes = 10,
            montoCuotas = 210000.0,
            montoActividades = 45000.0,
            ingresosTotales = 255000.0
        )

        val csv = CsvExporter.resumenMensual("Agosto", resumen)

        assertTrue(csv.startsWith("Periodo,Agosto,2026"))
        assertTrue(csv.contains("Ingresos totales,255000.0"))
    }

    @Test
    fun resumenMensual_escapaMesConComa() {
        val resumen = DBHelper.ResumenPagosMes(2026, 8, 0, 0, 0, 0.0, 0.0, 0.0)

        val csv = CsvExporter.resumenMensual("Agosto, especial", resumen)

        assertEquals('"', csv.first { it == '"' })
        assertTrue(csv.startsWith("Periodo,\"Agosto, especial\",2026"))
    }
}
