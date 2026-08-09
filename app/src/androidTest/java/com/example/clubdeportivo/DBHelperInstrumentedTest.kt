package com.example.clubdeportivo

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DBHelperInstrumentedTest {
    private lateinit var context: Context
    private lateinit var helper: DBHelper

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.deleteDatabase(DB_NAME)
        helper = DBHelper(context)
        prepararDatosDePrueba()
    }

    @After
    fun tearDown() {
        helper.close()
        context.deleteDatabase(DB_NAME)
    }

    @Test
    fun registrarPagoCuota_guardaPagoConIdClienteYVencimiento() {
        val rowId = helper.registrarPagoCuota(
            dni = CLIENT_DNI,
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
    }

    @Test
    fun insertarHorario_rechazaSolapamientoDeProfesor() {
        val error = assertThrows(IllegalArgumentException::class.java) {
            helper.insertarHorario(
                actividadId = 1L,
                profesorDni = PROFESSOR_DNI,
                dia = 1,
                horaInicio = 18 * 60 + 30,
                horaFin = 19 * 60 + 30
            )
        }

        assertEquals("El profesor ya tiene un horario activo en ese rango", error.message)
    }

    @Test
    fun obtenerCuentaCorriente_incluyeHistorialDeMovimientos() {
        val cuenta = helper.obtenerCuentaCorriente(CLIENT_DNI)

        assertTrue(cuenta != null)
        assertTrue(cuenta!!.movimientos.isNotEmpty())
    }

    private fun prepararDatosDePrueba() {
        helper.writableDatabase.apply {
            beginTransaction()
            try {
                execSQL("DELETE FROM pagos_actividad")
                execSQL("DELETE FROM cuotas")
                execSQL("DELETE FROM dias_horarios")
                execSQL("DELETE FROM actividad_profesor")
                execSQL("DELETE FROM profesores")
                execSQL("DELETE FROM actividades")
                execSQL("DELETE FROM clientes")

                execSQL(
                    "INSERT INTO actividades (id_actividad, nombre, precio) VALUES (1, 'Funcional', 5000)"
                )
                execSQL(
                    """
                        INSERT INTO profesores
                        (dni, nombre, apellido, fecha_nac, telefono, direccion, fecha_inscripcion,
                         ficha_medica, email, activo, titulo)
                        VALUES (?, 'Ana', 'Perez', '1990-01-01', '3415550000', 'Calle 1',
                                '2026-01-01', 1, 'ana@example.com', 1, 'Profesora')
                    """.trimIndent(),
                    arrayOf(PROFESSOR_DNI)
                )
                execSQL(
                    """
                        INSERT INTO clientes
                        (id, nombre, apellido, dni, fecha_nac, telefono, direccion,
                         fecha_inscripcion, ficha_medica, email, esSocio, activo, carnet)
                        VALUES (1, 'Juan', 'Perez', ?, '1995-01-01', '3415551111',
                                'Calle 2', '2026-01-01', 1, 'juan@example.com', 1, 1, 1)
                    """.trimIndent(),
                    arrayOf(CLIENT_DNI)
                )
                execSQL(
                    """
                        INSERT INTO actividad_profesor
                        (id, actividad_id, profesor_dni, activo)
                        VALUES (1, 1, ?, 1)
                    """.trimIndent(),
                    arrayOf(PROFESSOR_DNI)
                )
                execSQL(
                    """
                        INSERT INTO dias_horarios
                        (id, actividad_profesor_id, dia, hora_inicio, hora_fin, activo)
                        VALUES (1, 1, 1, 1080, 1140, 1)
                    """.trimIndent()
                )
                execSQL(
                    """
                        INSERT INTO cuotas
                        (idCliente, monto, fechaPago, formaPago, estadoDelPago, fechaVencimiento)
                        VALUES (1, 30000, '2026-07-01', 'Efectivo', 1, '2026-07-31')
                    """.trimIndent()
                )
                setTransactionSuccessful()
            } finally {
                endTransaction()
            }
        }
    }

    private companion object {
        const val DB_NAME = "app_clubDeportivo.db"
        const val CLIENT_DNI = "40111111"
        const val PROFESSOR_DNI = "27999888"
    }
}
