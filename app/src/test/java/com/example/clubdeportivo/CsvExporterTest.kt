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

        val csv = CsvExporter.resumenMensual("Agosto", resumen, ClubCurrency.ARS)

        assertTrue(csv.startsWith("Periodo,Agosto,2026"))
        assertTrue(csv.contains("Ingresos totales,255000.0"))
    }

    @Test
    fun resumenMensual_escapaMesConComa() {
        val resumen = DBHelper.ResumenPagosMes(2026, 8, 0, 0, 0, 0.0, 0.0, 0.0)

        val csv = CsvExporter.resumenMensual("Agosto, especial", resumen, ClubCurrency.ARS)

        assertEquals('"', csv.first { it == '"' })
        assertTrue(csv.startsWith("Periodo,\"Agosto, especial\",2026"))
    }

    @Test
    fun noSocios_generaCsvConActividadYFecha() {
        val csv = CsvExporter.noSocios(
            listOf(DBHelper.NoSocioCard("Ana", "Lopez", "30111222", "2026-08-01", "Yoga"))
        )

        assertTrue(csv.startsWith("Apellido,Nombre,DNI,\u00daltima actividad,Fecha ultimo pago"))
        assertTrue(csv.contains("Lopez,Ana,30111222,Yoga,2026-08-01"))
    }

    @Test
    fun socios_generaCsvConUltimoPago() {
        val csv = CsvExporter.socios(
            listOf(DBHelper.SocioCard("Juan", "Perez", "25111222", "2026-07-15"))
        )

        assertTrue(csv.startsWith("Apellido,Nombre,DNI,\u00daltimo pago"))
        assertTrue(csv.contains("Perez,Juan,25111222,2026-07-15"))
    }

    @Test
    fun vencimientos_generaCsvConEstado() {
        val csv = CsvExporter.vencimientos(
            listOf(DBHelper.VencimientoCard("Luis", "Diaz", "22111222", "2026-01-01", "2025-12-01"))
        )

        assertTrue(csv.startsWith("Apellido,Nombre,DNI,Vencimiento,\u00daltimo pago,Estado"))
        assertTrue(csv.contains("Diaz,Luis,22111222,2026-01-01,2025-12-01,Vencido"))
    }

}
