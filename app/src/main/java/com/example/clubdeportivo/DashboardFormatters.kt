package com.example.clubdeportivo

object DashboardFormatters {
    fun montoPesos(valor: Double): String = "$" + String.format("%,.0f", valor).replace(',', '.')
}
