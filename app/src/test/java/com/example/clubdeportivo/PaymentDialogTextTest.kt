package com.example.clubdeportivo

import org.junit.Assert.assertEquals
import org.junit.Test

class PaymentDialogTextTest {
    @Test
    fun cuota_armaMensajeConMontoYMedio() {
        assertEquals(
            "\u00bfConfirm\u00e1s registrar el pago de $30000 por \"Efectivo\"?",
            PaymentDialogText.cuota(30000.0, "Efectivo")
        )
    }

    @Test
    fun convertirNoSocio_armaMensajeClaro() {
        assertEquals(
            "\u00bfConfirm\u00e1s registrar el pago de $15000.50 por \"Transferencia\" y convertir a Perez, Ana en socio?",
            PaymentDialogText.convertirNoSocio(15000.5, "Transferencia", "Perez, Ana")
        )
    }

    @Test
    fun actividad_armaMensajeSinEspaciosRaros() {
        assertEquals(
            "\u00bfConfirm\u00e1s registrar el pago de $5000 por la actividad Yoga?",
            PaymentDialogText.actividad(5000.0, "Yoga")
        )
    }
    @Test
    fun socioCreado_muestraIdCuandoExiste() {
        assertEquals("\u00a1Pago exitoso! Ahora es socio (id 12)", PaymentDialogText.socioCreado(12))
    }

}
