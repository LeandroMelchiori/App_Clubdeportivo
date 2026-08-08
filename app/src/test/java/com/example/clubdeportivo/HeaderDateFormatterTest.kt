package com.example.clubdeportivo

import java.util.Calendar
import java.util.GregorianCalendar
import org.junit.Assert.assertEquals
import org.junit.Test

class HeaderDateFormatterTest {
    @Test
    fun format_muestraFechaEnEspanolArgentina() {
        val date = GregorianCalendar(2026, Calendar.AUGUST, 8).time

        assertEquals("S\u00e1bado, 8 de agosto", HeaderDateFormatter.format(date))
    }
}
