package com.example.clubdeportivo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ClubConfigurationValidatorTest {
    @Test
    fun validate_normalizaDatosValidos() {
        val result = ClubConfigurationValidator.validate(validDraft())

        assertTrue(result.isValid)
        assertEquals("Club Central", result.configuration?.name)
        assertEquals(30000.0, result.configuration?.monthlyFee ?: 0.0, 0.0)
        assertEquals(ClubCurrency.ARS, result.configuration?.currency)
        assertNull(result.error)
    }

    @Test
    fun parseMoney_aceptaFormatosLocales() {
        assertEquals(30000.0, ClubConfigurationValidator.parseMoney("30.000") ?: 0.0, 0.0)
        assertEquals(1000000.0, ClubConfigurationValidator.parseMoney("1.000.000") ?: 0.0, 0.0)
        assertEquals(30000.5, ClubConfigurationValidator.parseMoney("30.000,50") ?: 0.0, 0.0)
        assertEquals(30000.5, ClubConfigurationValidator.parseMoney("30,000.50") ?: 0.0, 0.0)
        assertEquals(30000.5, ClubConfigurationValidator.parseMoney("30000,50") ?: 0.0, 0.0)
        assertNull(ClubConfigurationValidator.parseMoney("12.34.56"))
    }

    @Test
    fun validate_rechazaNombreVacio() {
        val result = ClubConfigurationValidator.validate(validDraft().copy(name = " "))

        assertFalse(result.isValid)
        assertEquals(ClubConfigurationValidator.Field.NAME, result.field)
    }

    @Test
    fun validate_rechazaEmailInvalido() {
        val result = ClubConfigurationValidator.validate(validDraft().copy(email = "club@invalido"))

        assertEquals(ClubConfigurationValidator.Field.EMAIL, result.field)
    }

    @Test
    fun validate_rechazaCuotaNoPositiva() {
        val result = ClubConfigurationValidator.validate(validDraft().copy(monthlyFee = "0"))

        assertEquals(ClubConfigurationValidator.Field.MONTHLY_FEE, result.field)
    }

    @Test
    fun validate_rechazaDiaDeVencimientoFueraDeRango() {
        val result = ClubConfigurationValidator.validate(validDraft().copy(dueDay = "31"))

        assertEquals(ClubConfigurationValidator.Field.DUE_DAY, result.field)
    }

    @Test
    fun validate_rechazaDiasDeGraciaFueraDeRango() {
        val result = ClubConfigurationValidator.validate(validDraft().copy(graceDays = "32"))

        assertEquals(ClubConfigurationValidator.Field.GRACE_DAYS, result.field)
    }

    @Test
    fun validate_exigeUnMedioDePago() {
        val result = ClubConfigurationValidator.validate(
            validDraft().copy(
                acceptsCash = false,
                acceptsTransfer = false,
                acceptsCard = false
            )
        )

        assertEquals(ClubConfigurationValidator.Field.PAYMENT_METHODS, result.field)
    }

    private fun validDraft() = ClubConfigurationValidator.Draft(
        name = "  Club Central  ",
        address = "San Martin 123",
        phone = "+54 341 555-0000",
        email = "contacto@club.com",
        currencyCode = "ARS",
        monthlyFee = "30.000",
        dueDay = "10",
        graceDays = "5",
        acceptsCash = true,
        acceptsTransfer = true,
        acceptsCard = false,
        logoUri = null
    )
}
