package com.example.clubdeportivo

object ScheduleOverlapValidator {
    fun isValidRange(startMinutes: Int, endMinutes: Int): Boolean =
        startMinutes >= 0 && endMinutes <= 24 * 60 && endMinutes > startMinutes

    fun overlaps(
        startA: Int,
        endA: Int,
        startB: Int,
        endB: Int
    ): Boolean = startA < endB && startB < endA
}
