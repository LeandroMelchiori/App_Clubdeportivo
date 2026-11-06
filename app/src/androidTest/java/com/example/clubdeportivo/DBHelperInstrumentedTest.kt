package com.example.clubdeportivo

import android.content.Context
import android.database.DatabaseUtils
import android.database.sqlite.SQLiteConstraintException
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
            formaPago = "Efectivo",
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
            assertEquals("2026-09-10", cursor.getString(4))
        }
    }

    @Test
    fun registrarPagoCuota_aplicaDescuentoYRecargoConfigurados() {
        helper.guardarConfiguracionClub(
            ClubConfiguration.DEFAULT.copy(billingRules = BillingRules(lateFeeValue = 10.0))
        )
        val discountId = helper.insertarDescuento(
            Discount(-1L, "Grupo familiar", AdjustmentType.PERCENT, 15.0, ChargeScope.BOTH)
        )
        helper.guardarBeneficiosCliente(1, discountId, null)

        val rowId = helper.registrarPagoCuota(CLIENT_DNI, "Efectivo", "2026-08-15")

        helper.readableDatabase.rawQuery(
            """
                SELECT monto, monto_base, descuento, recargo, detalle_ajustes, formaPago
                FROM cuotas
                WHERE idCuota = ?
            """.trimIndent(),
            arrayOf(rowId.toString())
        ).use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(28050.0, cursor.getDouble(0), 0.0)
            assertEquals(30000.0, cursor.getDouble(1), 0.0)
            assertEquals(4500.0, cursor.getDouble(2), 0.0)
            assertEquals(2550.0, cursor.getDouble(3), 0.0)
            assertEquals("descuento Grupo familiar, recargo por mora", cursor.getString(4))
            assertEquals("Efectivo", cursor.getString(5))
        }
    }

    @Test
    fun registrarPagoCuota_conBecaTotalRegistraSinCargoNiMedio() {
        helper.guardarBeneficiosCliente(1, null, Scholarship(100, ChargeScope.FEE, "Convenio"))

        val rowId = helper.registrarPagoCuota(CLIENT_DNI, null, "2026-08-15")

        helper.readableDatabase.rawQuery(
            "SELECT monto, descuento, formaPago, detalle_ajustes, fechaVencimiento FROM cuotas WHERE idCuota = ?",
            arrayOf(rowId.toString())
        ).use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(0.0, cursor.getDouble(0), 0.0)
            assertEquals(30000.0, cursor.getDouble(1), 0.0)
            assertEquals(PaymentDbRules.noChargeMethod, cursor.getString(2))
            assertEquals("beca 100 %", cursor.getString(3))
            assertEquals("2026-09-10", cursor.getString(4))
        }
    }

    @Test
    fun registrarPagoActividad_aplicaBecaDeActividades() {
        helper.writableDatabase.execSQL(
            """
                INSERT INTO clientes
                (id, nombre, apellido, dni, fecha_nac, telefono, direccion,
                 fecha_inscripcion, ficha_medica, email, esSocio, activo, carnet)
                VALUES (2, 'Ana', 'Rios', '30111222', '2000-01-01', '3415552222',
                        'Calle 3', '2026-01-01', 1, 'ana@example.com', 0, 1, 0)
            """.trimIndent()
        )
        helper.guardarBeneficiosCliente(2, null, Scholarship(50, ChargeScope.ACTIVITIES, "Convenio escolar"))

        assertEquals(2500.0, helper.calcularCobroActividad(2, 1)?.total ?: -1.0, 0.0)
        val rowId = helper.registrarPagoActividadNoSocio("2", 1, "Transferencia", "2026-08-15")

        helper.readableDatabase.rawQuery(
            "SELECT monto, monto_base, descuento, recargo, forma_pago FROM pagos_actividad WHERE id_pago = ?",
            arrayOf(rowId.toString())
        ).use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(2500.0, cursor.getDouble(0), 0.0)
            assertEquals(5000.0, cursor.getDouble(1), 0.0)
            assertEquals(2500.0, cursor.getDouble(2), 0.0)
            assertEquals(0.0, cursor.getDouble(3), 0.0)
            assertEquals("Transferencia", cursor.getString(4))
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

    @Test
    fun profesores_permiteAltaEdicionYBajaLogica() {
        val professor = DBHelper.Profesor(
            dni = "30123456",
            nombre = "Laura",
            apellido = "Gomez",
            fechaNac = "1991-05-20",
            telefono = "3415551000",
            direccion = "Calle 10",
            fechaInscripcion = "2026-09-01",
            fichaMedica = true,
            email = "laura@example.com",
            activo = true,
            titulo = "Entrenadora"
        )

        assertTrue(helper.insertarProfesor(professor) > 0L)
        assertEquals("Laura", helper.obtenerProfesor(professor.dni)?.nombre)
        assertEquals(1, helper.actualizarProfesor(professor.copy(nombre = "Laurita")))
        assertEquals("Laurita", helper.obtenerProfesor(professor.dni)?.nombre)
        assertTrue(helper.darDeBajaProfesor(professor.dni))
        assertEquals(false, helper.obtenerProfesor(professor.dni)?.activo)
    }

    @Test
    fun catalogo_permiteCrudYProtegeActividadesAsignadas() {
        val activityId = helper.insertarCatalogoActividad(
            DBHelper.CatalogoActividad(-1L, "Pilates", 8500.0)
        )

        assertTrue(activityId > 0L)
        assertEquals(8500.0, helper.obtenerCatalogoActividad(activityId)?.precio ?: 0.0, 0.0)
        assertEquals(
            1,
            helper.actualizarCatalogoActividad(
                DBHelper.CatalogoActividad(activityId, "Pilates reformer", 9000.0)
            )
        )
        assertEquals("Pilates reformer", helper.obtenerCatalogoActividad(activityId)?.nombre)
        assertTrue(helper.eliminarCatalogoActividad(activityId))
        assertEquals(null, helper.obtenerCatalogoActividad(activityId))
        assertEquals(false, helper.eliminarCatalogoActividad(1L))
    }

    @Test
    fun descuentos_permiteCrudYProtegeLosAsignados() {
        val discountId = helper.insertarDescuento(
            Discount(-1L, "Grupo familiar", AdjustmentType.PERCENT, 15.0, ChargeScope.BOTH)
        )
        val unusedId = helper.insertarDescuento(
            Discount(-1L, "Jubilados", AdjustmentType.FIXED, 5000.0, ChargeScope.FEE)
        )

        assertTrue(discountId > 0L)
        assertEquals(
            1,
            helper.actualizarDescuento(
                Discount(discountId, "Familia", AdjustmentType.PERCENT, 20.0, ChargeScope.FEE, active = false)
            )
        )
        val updated = helper.obtenerDescuento(discountId)
        assertEquals("Familia", updated?.name)
        assertEquals(20.0, updated?.value ?: 0.0, 0.0)
        assertEquals(ChargeScope.FEE, updated?.scope)
        assertEquals(false, updated?.active)
        assertEquals(listOf("Jubilados"), helper.obtenerDescuentos(soloActivos = true).map { it.name })

        assertTrue(helper.guardarBeneficiosCliente(1, discountId, null))
        assertEquals(false, helper.eliminarDescuento(discountId))
        assertTrue(helper.eliminarDescuento(unusedId))
        assertEquals(null, helper.obtenerDescuento(unusedId))
    }

    @Test
    fun beneficios_guardaDescuentoYBecaPorCliente() {
        val discountId = helper.insertarDescuento(
            Discount(-1L, "Grupo familiar", AdjustmentType.PERCENT, 15.0, ChargeScope.BOTH)
        )
        val scholarship = Scholarship(
            percent = 50,
            scope = ChargeScope.FEE,
            reason = "Deportista federado",
            until = java.time.LocalDate.parse("2026-12-31")
        )

        assertEquals(ClientBenefits.NONE, helper.obtenerBeneficiosCliente(1))
        assertTrue(helper.guardarBeneficiosCliente(1, discountId, scholarship))
        val benefits = helper.obtenerBeneficiosCliente(1)
        assertEquals("Grupo familiar", benefits.discount?.name)
        assertEquals(scholarship, benefits.scholarship)

        assertTrue(helper.guardarBeneficiosCliente(1, null, scholarship.copy(until = null)))
        assertEquals(null, helper.obtenerBeneficiosCliente(1).discount)
        assertEquals(null, helper.obtenerBeneficiosCliente(1).scholarship?.until)

        assertTrue(helper.guardarBeneficiosCliente(1, null, null))
        assertEquals(ClientBenefits.NONE, helper.obtenerBeneficiosCliente(1))
    }

    @Test
    fun importarClientes_guardaNoSociosActivosConFechaDeInscripcion() {
        val imported = helper.importarClientes(
            listOf(clienteImportado("30111222", "ana@club.com"), clienteImportado("30111223", "eva@club.com")),
            fechaInscripcion = "2026-11-04"
        )

        assertEquals(2, imported)
        helper.readableDatabase.rawQuery(
            """
                SELECT esSocio, activo, carnet, ficha_medica, fecha_inscripcion, fecha_nac
                FROM clientes
                WHERE dni = ?
            """.trimIndent(),
            arrayOf("30111222")
        ).use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(0, cursor.getInt(0))
            assertEquals(1, cursor.getInt(1))
            assertEquals(0, cursor.getInt(2))
            assertEquals(1, cursor.getInt(3))
            assertEquals("2026-11-04", cursor.getString(4))
            assertEquals("1990-04-05", cursor.getString(5))
        }
        val identities = helper.obtenerIdentidadesClientes()
        assertTrue(identities.dnis.containsAll(listOf(CLIENT_DNI, "30111222", "30111223")))
        assertTrue("eva@club.com" in identities.emails)
    }

    @Test
    fun importarClientes_noGuardaNadaSiUnaFilaFalla() {
        assertThrows(SQLiteConstraintException::class.java) {
            helper.importarClientes(
                listOf(clienteImportado("30111222", "ana@club.com"), clienteImportado(CLIENT_DNI, "otro@club.com")),
                fechaInscripcion = "2026-11-04"
            )
        }

        assertEquals(
            0L,
            DatabaseUtils.longForQuery(
                helper.readableDatabase,
                "SELECT COUNT(*) FROM clientes WHERE dni = ?",
                arrayOf("30111222")
            )
        )
    }

    private fun clienteImportado(dni: String, email: String) = ClientCsvImport.Client(
        dni = dni,
        name = "Ana",
        lastName = "Perez",
        birthDate = "1990-04-05",
        phone = "3415551111",
        address = "Calle 1",
        email = email
    )

    private fun prepararDatosDePrueba() {
        helper.writableDatabase.apply {
            beginTransaction()
            try {
                execSQL("DELETE FROM cliente_beneficios")
                execSQL("DELETE FROM descuentos")
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
