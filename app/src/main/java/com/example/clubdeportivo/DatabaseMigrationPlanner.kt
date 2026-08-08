package com.example.clubdeportivo

object DatabaseMigrationPlanner {
    fun pendingSteps(oldVersion: Int, newVersion: Int): List<Int> {
        require(oldVersion >= 1) { "La version anterior debe ser al menos 1" }
        require(newVersion >= oldVersion) { "La version nueva no puede ser menor" }
        return ((oldVersion + 1)..newVersion).filter { it == 2 }
    }
}
