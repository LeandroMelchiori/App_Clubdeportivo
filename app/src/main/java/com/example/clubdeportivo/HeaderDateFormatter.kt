package com.example.clubdeportivo

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object HeaderDateFormatter {
    private val localeAr: Locale = Locale.forLanguageTag("es-AR")

    fun format(date: Date = Date()): String {
        val text = SimpleDateFormat("EEEE, d 'de' MMMM", localeAr).format(date)
        return text.replaceFirstChar { it.uppercase(localeAr) }
    }
}
