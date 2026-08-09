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
    fun pendingSteps_deVersionDosATresIncluyeConfiguracion() {
        assertEquals(listOf(3), DatabaseMigrationPlanner.pendingSteps(2, 3))
    }

    @Test
    fun pendingSteps_deVersionUnoATresConservaElOrden() {
        assertEquals(listOf(2, 3), DatabaseMigrationPlanner.pendingSteps(1, 3))
    }

    @Test
    fun pendingSteps_mismaVersionNoTieneCambios() {
        assertEquals(emptyList<Int>(), DatabaseMigrationPlanner.pendingSteps(3, 3))
    }

    @Test
    fun pendingSteps_rechazaDowngrade() {
        assertThrows(IllegalArgumentException::class.java) {
            DatabaseMigrationPlanner.pendingSteps(3, 2)
        }
    }

    @Test
    fun pendingSteps_rechazaVersionAnteriorInvalida() {
        assertThrows(IllegalArgumentException::class.java) {
            DatabaseMigrationPlanner.pendingSteps(0, 3)
        }
    }
}
