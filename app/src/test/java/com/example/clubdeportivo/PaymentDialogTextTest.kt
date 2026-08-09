package com.example.clubdeportivo

import org.junit.Assert.assertEquals
import org.junit.Test

class PaymentDialogTextTest {
    @Test
    fun cuota_armaMensajeConMonedaYMedio() {
        assertEquals(
            "Confirmas registrar el pago de USD 30.000,00 por \"Efectivo\"?",
            PaymentDialogText.cuota(30000.0, "Efectivo", ClubCurrency.USD)
        )
    }

    @Test
    fun convertirNoSocio_armaMensajeClaro() {
        assertEquals(
            "Confirmas registrar el pago de ARS 15.000,50 por \"Transferencia\" y convertir a Perez, Ana en socio?",
            PaymentDialogText.convertirNoSocio(
                15000.5,
                "Transferencia",
                "Perez, Ana",
                ClubCurrency.ARS
            )
        )
    }

    @Test
    fun actividad_incluyeMedioDePago() {
        assertEquals(
            "Confirmas registrar el pago de BRL 5.000,00 por la actividad Yoga mediante Tarjeta?",
            PaymentDialogText.actividad(5000.0, "Yoga", "Tarjeta", ClubCurrency.BRL)
        )
    }

    @Test
    fun socioCreado_muestraIdCuandoExiste() {
        assertEquals("Pago registrado. Ahora es socio (id 12)", PaymentDialogText.socioCreado(12))
    }
}
