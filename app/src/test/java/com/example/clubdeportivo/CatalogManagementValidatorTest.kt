package com.example.clubdeportivo

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CatalogManagementValidatorTest {
    @Test
    fun activity_acceptsRegionalPriceAndNormalizesName() {
        val result = CatalogActivityValidator.validate("  Yoga  ", "12.500,50")

        assertEquals("Yoga", result.value?.name)
        assertEquals(12500.5, result.value?.price ?: 0.0, 0.0)
        assertNull(result.error)
    }

    @Test
    fun activity_rejectsEmptyNameAndNonPositivePrice() {
        assertEquals("name", CatalogActivityValidator.validate(" ", "100").field)
        assertEquals("price", CatalogActivityValidator.validate("Yoga", "0").field)
    }

    @Test
    fun professor_acceptsValidDataAndNormalizesOptionalTitle() {
        val result = ProfessorValidator.validate(validProfessor(), LocalDate.parse("2026-09-01"))

        assertTrue(result.value != null)
        assertEquals("30111222", result.value?.dni)
        assertNull(result.value?.title)
    }

    @Test
    fun professor_rejectsInvalidIdentityContactAndFutureDate() {
        assertEquals("dni", ProfessorValidator.validate(validProfessor().copy(dni = "12")).field)
        assertEquals("email", ProfessorValidator.validate(validProfessor().copy(email = "mail-invalido")).field)
        assertEquals(
            "birthDate",
            ProfessorValidator.validate(
                validProfessor().copy(birthDate = "2027-01-01"),
                LocalDate.parse("2026-09-01")
            ).field
        )
    }

    private fun validProfessor() = ProfessorValidator.Draft(
        dni = "30111222",
        name = "Ana",
        lastName = "Perez",
        birthDate = "1990-05-10",
        phone = "+54 341 555-0000",
        email = "ana@club.com",
        address = "San Martin 123",
        title = ""
    )
}
