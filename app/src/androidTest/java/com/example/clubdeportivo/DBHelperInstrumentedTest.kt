package com.example.clubdeportivo

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DBHelperInstrumentedTest {
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.deleteDatabase(DB_NAME)
    }

    @After
    fun tearDown() {
        context.deleteDatabase(DB_NAME)
    }

    @Test
    fun registrarPagoCuota_guardaPagoConIdClienteYVencimiento() {
        val helper = DBHelper(context)

        val rowId = helper.registrarPagoCuota(
            dni = "40111111",
            monto = 30000.0,
            formaPago = "Efectivo",
            ultimoPago = "2026-07-31",
            fechaPago = "2026-08-01"
        )

        assertTrue(rowId > 0L)
        helper.readableDatabase.rawQuery(
            """
                SELECT idCliente, monto, fechaPago, formaPago, fechaVencimiento
                FROM cuotas
                WHERE idCuota = ?
            """.trimIndent(),
            arrayOf(rowId.toString())
        ).use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(1, cursor.getInt(0))
            assertEquals(30000.0, cursor.getDouble(1), 0.0)
            assertEquals("2026-08-01", cursor.getString(2))
            assertEquals("Efectivo", cursor.getString(3))
            assertEquals("2026-08-31", cursor.getString(4))
        }

        helper.close()
    }


    @Test
    fun insertarHorario_rechazaSolapamientoDeProfesor() {
        val helper = DBHelper(context)

        val error = assertThrows(IllegalArgumentException::class.java) {
            helper.insertarHorario(
                actividadId = 2L,
                profesorDni = "27999888",
                dia = 1,
                horaInicio = 8 * 60 + 30,
                horaFin = 9 * 60 + 30
            )
        }

        assertEquals("El profesor ya tiene un horario activo en ese rango", error.message)
        helper.close()
    }


    @Test
    fun obtenerCuentaCorriente_incluyeHistorialDeMovimientos() {
        val helper = DBHelper(context)

        val cuenta = helper.obtenerCuentaCorriente("30111222")

        assertTrue(cuenta != null)
        assertTrue(cuenta!!.movimientos.isNotEmpty())
        helper.close()
    }

    private companion object {
        const val DB_NAME = "app_clubDeportivo.db"
    }
}
