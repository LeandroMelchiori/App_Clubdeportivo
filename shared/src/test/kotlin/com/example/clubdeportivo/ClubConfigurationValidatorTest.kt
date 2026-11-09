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

    @Test
    fun validate_sinRecargoUsaLasReglasPredeterminadas() {
        val result = ClubConfigurationValidator.validate(validDraft())

        assertEquals(BillingRules.DEFAULT, result.configuration?.billingRules)
    }

    @Test
    fun validate_armaLasReglasDeCobroConfiguradas() {
        val result = ClubConfigurationValidator.validate(
            validDraft().copy(
                lateFeeTypeCode = "fixed",
                lateFeeValue = "1.500",
                lateFeeFrequencyCode = "monthly",
                lateFeeMaxMonths = "6",
                benefitStackingCode = "combined",
                scholarshipWaivesLateFee = false,
                lateLosesDiscount = true
            )
        )

        assertEquals(
            BillingRules(
                lateFeeType = AdjustmentType.FIXED,
                lateFeeValue = 1500.0,
                lateFeeFrequency = LateFeeFrequency.MONTHLY,
                lateFeeMaxMonths = 6,
                benefitStacking = BenefitStacking.COMBINED,
                scholarshipWaivesLateFee = false,
                lateLosesDiscount = true
            ),
            result.configuration?.billingRules
        )
    }

    @Test
    fun validate_rechazaRecargoInvalido() {
        val negativo = ClubConfigurationValidator.validate(validDraft().copy(lateFeeValue = "-5"))
        val vacio = ClubConfigurationValidator.validate(validDraft().copy(lateFeeValue = ""))
        val porcentajeAlto = ClubConfigurationValidator.validate(validDraft().copy(lateFeeValue = "120"))
        val montoAlto = ClubConfigurationValidator.validate(
            validDraft().copy(lateFeeTypeCode = "fixed", lateFeeValue = "120")
        )

        assertEquals(ClubConfigurationValidator.Field.LATE_FEE_VALUE, negativo.field)
        assertEquals(ClubConfigurationValidator.Field.LATE_FEE_VALUE, vacio.field)
        assertEquals("El recargo porcentual no puede superar 100", porcentajeAlto.error)
        assertTrue(montoAlto.isValid)
    }

    @Test
    fun validate_exigeTopeDeMesesSoloConRecargoMensual() {
        val mensual = ClubConfigurationValidator.validate(
            validDraft().copy(lateFeeFrequencyCode = "monthly", lateFeeMaxMonths = "0")
        )
        val unico = ClubConfigurationValidator.validate(validDraft().copy(lateFeeMaxMonths = ""))

        assertEquals(ClubConfigurationValidator.Field.LATE_FEE_MAX_MONTHS, mensual.field)
        assertTrue(unico.isValid)
        assertEquals(3, unico.configuration?.billingRules?.lateFeeMaxMonths)
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
