package com.example.clubdeportivo

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ClubConfigurationMigrationInstrumentedTest {
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
    fun migracionDesdeDos_agregaConfiguracionSinBorrarDatos() {
        context.openOrCreateDatabase(DB_NAME, Context.MODE_PRIVATE, null).use { legacy ->
            legacy.execSQL("CREATE TABLE legacy_data (value TEXT NOT NULL)")
            legacy.execSQL("INSERT INTO legacy_data (value) VALUES ('preservado')")
            legacy.version = 2
        }

        DBHelper(context).use { helper ->
            assertEquals(4, helper.readableDatabase.version)
            assertEquals(ClubConfiguration.DEFAULT, helper.obtenerConfiguracionClub())
            helper.readableDatabase.rawQuery(
                "SELECT value FROM legacy_data",
                null
            ).use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("preservado", cursor.getString(0))
            }
        }
    }

    @Test
    fun migracionTresACuatro_conservaPagosYConfiguracion() {
        context.openOrCreateDatabase(DB_NAME, Context.MODE_PRIVATE, null).use { legacy ->
            legacy.execSQL(
                """
                    CREATE TABLE clientes (
                        id INTEGER PRIMARY KEY AUTOINCREMENT, nombre TEXT NOT NULL,
                        apellido TEXT NOT NULL, dni TEXT NOT NULL UNIQUE
                    )
                """.trimIndent()
            )
            legacy.execSQL(
                """
                    CREATE TABLE cuotas (
                        idCuota INTEGER PRIMARY KEY AUTOINCREMENT, idCliente INTEGER NOT NULL,
                        monto NUMERIC, fechaPago TEXT NOT NULL, formaPago TEXT,
                        estadoDelPago INTEGER NOT NULL, fechaVencimiento TEXT NOT NULL
                    )
                """.trimIndent()
            )
            legacy.execSQL(
                """
                    CREATE TABLE pagos_actividad (
                        id_pago INTEGER PRIMARY KEY AUTOINCREMENT, idCliente TEXT NOT NULL,
                        id_actividad INTEGER NOT NULL, fecha_pago TEXT NOT NULL,
                        forma_pago TEXT NOT NULL, monto NUMERIC NOT NULL
                    )
                """.trimIndent()
            )
            legacy.execSQL(
                """
                    CREATE TABLE club_configuration (
                        id INTEGER PRIMARY KEY CHECK (id = 1), name TEXT NOT NULL,
                        address TEXT NOT NULL DEFAULT '', phone TEXT NOT NULL DEFAULT '',
                        email TEXT NOT NULL DEFAULT '', currency TEXT NOT NULL,
                        monthly_fee REAL NOT NULL, due_day INTEGER NOT NULL,
                        grace_days INTEGER NOT NULL, accepts_cash INTEGER NOT NULL,
                        accepts_transfer INTEGER NOT NULL, accepts_card INTEGER NOT NULL,
                        logo_uri TEXT
                    )
                """.trimIndent()
            )
            legacy.execSQL(
                """
                    INSERT INTO club_configuration VALUES
                    (1, 'Club Norte', '', '', '', 'USD', 45000, 5, 3, 1, 0, 1, NULL)
                """.trimIndent()
            )
            legacy.execSQL("INSERT INTO clientes VALUES (1, 'Ana', 'Lopez', '40111111')")
            legacy.execSQL(
                "INSERT INTO cuotas VALUES (1, 1, 45000, '2026-08-01', 'Efectivo', 1, '2026-09-08')"
            )
            legacy.execSQL(
                "INSERT INTO pagos_actividad VALUES (1, '1', 3, '2026-08-02', 'Transferencia', 8000)"
            )
            legacy.version = 3
        }

        DBHelper(context).use { helper ->
            assertEquals(4, helper.readableDatabase.version)
            val configuration = helper.obtenerConfiguracionClub()
            assertEquals("Club Norte", configuration.name)
            assertEquals(45000.0, configuration.monthlyFee, 0.0)
            assertEquals(BillingRules.DEFAULT, configuration.billingRules)
            assertTrue(helper.obtenerDescuentos().isEmpty())
            assertEquals(ClientBenefits.NONE, helper.obtenerBeneficiosCliente(1))
            helper.readableDatabase.rawQuery(
                "SELECT monto, monto_base, descuento, recargo, detalle_ajustes FROM cuotas",
                null
            ).use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(45000.0, cursor.getDouble(0), 0.0)
                assertEquals(45000.0, cursor.getDouble(1), 0.0)
                assertEquals(0.0, cursor.getDouble(2), 0.0)
                assertEquals(0.0, cursor.getDouble(3), 0.0)
                assertTrue(cursor.isNull(4))
            }
            helper.readableDatabase.rawQuery(
                "SELECT monto, monto_base, descuento, recargo FROM pagos_actividad",
                null
            ).use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(8000.0, cursor.getDouble(0), 0.0)
                assertEquals(8000.0, cursor.getDouble(1), 0.0)
                assertEquals(0.0, cursor.getDouble(2), 0.0)
                assertEquals(0.0, cursor.getDouble(3), 0.0)
            }
        }
    }

    private companion object {
        const val DB_NAME = "app_clubDeportivo.db"
    }
}
