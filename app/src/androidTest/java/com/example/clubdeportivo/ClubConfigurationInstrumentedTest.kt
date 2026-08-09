package com.example.clubdeportivo

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ClubConfigurationInstrumentedTest {
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
    fun nuevaBase_incluyeConfiguracionPredeterminada() {
        DBHelper(context).use { helper ->
            assertEquals(ClubConfiguration.DEFAULT, helper.obtenerConfiguracionClub())
        }
    }

    @Test
    fun guardarConfiguracion_persisteTodosLosCampos() {
        val expected = ClubConfiguration(
            name = "Club Central",
            address = "San Martin 123",
            phone = "+54 341 555-0000",
            email = "contacto@club.com",
            currency = ClubCurrency.USD,
            monthlyFee = 42500.5,
            dueDay = 12,
            graceDays = 4,
            acceptsCash = true,
            acceptsTransfer = true,
            acceptsCard = false,
            logoUri = "content://club/logo"
        )

        DBHelper(context).use { helper ->
            assertTrue(helper.guardarConfiguracionClub(expected))
        }

        DBHelper(context).use { helper ->
            assertEquals(expected, helper.obtenerConfiguracionClub())
        }
    }

    @Test
    fun guardarConfiguracion_admiteLogoVacio() {
        DBHelper(context).use { helper ->
            assertTrue(helper.guardarConfiguracionClub(ClubConfiguration.DEFAULT))
            assertNull(helper.obtenerConfiguracionClub().logoUri)
        }
    }

    private companion object {
        const val DB_NAME = "app_clubDeportivo.db"
    }
}
