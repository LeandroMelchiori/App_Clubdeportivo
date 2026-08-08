package com.example.clubdeportivo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class DatabaseMigrationPlannerTest {
    @Test
    fun pendingSteps_deVersionUnoADosIncluyeMigracionDos() {
        assertEquals(listOf(2), DatabaseMigrationPlanner.pendingSteps(1, 2))
    }

    @Test
    fun pendingSteps_mismaVersionNoTieneCambios() {
        assertEquals(emptyList<Int>(), DatabaseMigrationPlanner.pendingSteps(2, 2))
    }

    @Test
    fun pendingSteps_rechazaDowngrade() {
        assertThrows(IllegalArgumentException::class.java) {
            DatabaseMigrationPlanner.pendingSteps(2, 1)
        }
    }
}
