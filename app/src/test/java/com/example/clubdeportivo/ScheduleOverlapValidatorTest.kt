package com.example.clubdeportivo

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScheduleOverlapValidatorTest {
    @Test
    fun isValidRange_rechazaFinMenorOIgualAlInicio() {
        assertFalse(ScheduleOverlapValidator.isValidRange(600, 600))
        assertFalse(ScheduleOverlapValidator.isValidRange(660, 600))
    }

    @Test
    fun overlaps_detectaSolapamientoParcial() {
        assertTrue(ScheduleOverlapValidator.overlaps(600, 660, 630, 690))
    }

    @Test
    fun overlaps_permiteHorariosContiguos() {
        assertFalse(ScheduleOverlapValidator.overlaps(600, 660, 660, 720))
    }
}
