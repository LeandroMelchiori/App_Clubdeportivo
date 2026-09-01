package com.example.clubdeportivo

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertTrue
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
    fun demo_creaDatosIniciales() {
        DBHelper(context).use { helper ->
            assertTrue(helper.countRows("clientes") > 0)
            assertTrue(helper.countRows("profesores") > 0)
            assertTrue(helper.countRows("actividades") > 0)
            assertTrue(helper.countRows("cuotas") > 0)
            assertTrue(helper.countRows("club_configuration") == 1)
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
