package com.example.clubdeportivo

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EnvironmentDatabaseInstrumentedTest {
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
    fun production_creaEsquemaSinDatosFicticios() {
        DBHelper(context).use { helper ->
            listOf(
                "clientes",
                "profesores",
                "actividades",
                "cuotas",
                "pagos_actividad",
                "actividad_profesor",
                "dias_horarios"
            ).forEach { table ->
                assertEquals("$table debe iniciar vacia", 0, helper.countRows(table))
            }
        }
    }

    private fun DBHelper.countRows(table: String): Int =
        readableDatabase.rawQuery("SELECT COUNT(*) FROM $table", null).use { cursor ->
            cursor.moveToFirst()
            cursor.getInt(0)
        }

    private companion object {
        const val DB_NAME = "app_clubDeportivo.db"
    }
}
