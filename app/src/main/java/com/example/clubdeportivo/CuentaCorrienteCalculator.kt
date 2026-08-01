package com.example.clubdeportivo

import java.time.LocalDate
import java.time.temporal.ChronoUnit

object CuentaCorrienteCalculator {
    private const val CUOTA_MENSUAL_ESTIMADA = 30000.0

    fun evaluarSocio(proximoVencimientoIso: String?, hoy: LocalDate = LocalDate.now()): EstadoCuenta {
        if (proximoVencimientoIso.isNullOrBlank()) {
            return EstadoCuenta("Sin pagos", "Sin vencimiento", CUOTA_MENSUAL_ESTIMADA)
        }

        val vencimiento = LocalDate.parse(proximoVencimientoIso)
        val dias = ChronoUnit.DAYS.between(hoy, vencimiento)
        return when {
            dias < 0 -> EstadoCuenta("Vencido", "Vencio hace ${-dias} dias", CUOTA_MENSUAL_ESTIMADA)
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
