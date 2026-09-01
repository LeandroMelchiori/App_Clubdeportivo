package com.example.clubdeportivo

import java.time.LocalDate
import java.time.temporal.ChronoUnit

object CuentaCorrienteCalculator {
    fun evaluarSocio(
        proximoVencimientoIso: String?,
        monthlyFee: Double,
        hoy: LocalDate = LocalDate.now()
    ): EstadoCuenta {
        require(monthlyFee > 0.0 && monthlyFee.isFinite()) { "La cuota mensual debe ser valida" }
        if (proximoVencimientoIso.isNullOrBlank()) {
            return EstadoCuenta("Sin pagos", "Sin vencimiento", monthlyFee)
        }

        val vencimiento = LocalDate.parse(proximoVencimientoIso)
        val dias = ChronoUnit.DAYS.between(hoy, vencimiento)
        return when {
            dias < 0 -> EstadoCuenta("Vencido", "Vencio hace ${-dias} dias", monthlyFee)
            dias <= 7 -> EstadoCuenta("Por vencer", "Vence en $dias dias", 0.0)
            else -> EstadoCuenta("Al dia", "Vence en $dias dias", 0.0)
        }
    }

    fun evaluarNoSocio(tienePagosActividad: Boolean): EstadoCuenta =
        if (tienePagosActividad) {
            EstadoCuenta("Actividad registrada", "Sin cuota mensual", 0.0)
        } else {
            EstadoCuenta("Sin pagos", "Sin cuota mensual", 0.0)
        }
}

data class EstadoCuenta(
    val estado: String,
    val detalle: String,
    val deudaEstimada: Double
)
