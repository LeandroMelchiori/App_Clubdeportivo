package com.example.clubdeportivo

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ScholarshipValidatorTest {
    private val hoy = LocalDate.parse("2026-11-06")

    @Test
    fun validate_armaBecaConFechaOpcional() {
        val sinFecha = validate(until = " ")
        val conFecha = validate(until = "2026-12-31")

        assertEquals(Scholarship(50, ChargeScope.FEE, "Deportista federado"), sinFecha.scholarship)
        assertEquals(LocalDate.parse("2026-12-31"), conFecha.scholarship?.until)
        assertNull(conFecha.error)
    }

    @Test
    fun validate_aceptaBecaTotalYRechazaPorcentajesFueraDeRango() {
        assertEquals(100, validate(percent = "100").scholarship?.percent)
        assertEquals(ScholarshipValidator.Field.PERCENT, validate(percent = "0").field)
        assertEquals(ScholarshipValidator.Field.PERCENT, validate(percent = "101").field)
        assertEquals(ScholarshipValidator.Field.PERCENT, validate(percent = "").field)
    }

    @Test
    fun validate_exigeMotivo() {
        assertEquals("Ingresa el motivo de la beca", validate(reason = "  ").error)
        assertEquals(ScholarshipValidator.Field.REASON, validate(reason = "x".repeat(121)).field)
    }

    @Test
    fun validate_rechazaFechaInvalidaOPasada() {
        assertEquals(ScholarshipValidator.Field.UNTIL, validate(until = "31/12/2026").field)
        assertEquals(
            "La fecha de fin no puede ser anterior a hoy",
            validate(until = "2026-11-05").error
        )
        assertEquals(hoy, validate(until = "2026-11-06").scholarship?.until)
    }

    private fun validate(
        percent: String = "50",
        reason: String = " Deportista federado ",
        until: String = ""
    ) = ScholarshipValidator.validate(percent, ChargeScope.FEE, reason, until, hoy)
}
