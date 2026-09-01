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
    fun migrationDosATres_agregaConfiguracionSinBorrarDatos() {
        context.openOrCreateDatabase(DB_NAME, Context.MODE_PRIVATE, null).use { legacy ->
            legacy.execSQL("CREATE TABLE legacy_data (value TEXT NOT NULL)")
            legacy.execSQL("INSERT INTO legacy_data (value) VALUES ('preservado')")
            legacy.version = 2
        }

        DBHelper(context).use { helper ->
            assertEquals(3, helper.readableDatabase.version)
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

    private companion object {
        const val DB_NAME = "app_clubDeportivo.db"
    }
}
