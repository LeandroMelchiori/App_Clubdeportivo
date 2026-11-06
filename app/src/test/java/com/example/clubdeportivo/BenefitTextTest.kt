package com.example.clubdeportivo

import org.junit.Assert.assertEquals
import org.junit.Test

class BenefitTextTest {
    @Test
    fun discountDetail_muestraPorcentajeAlcanceYEstado() {
        val discount = Discount(1, "Grupo familiar", AdjustmentType.PERCENT, 12.5, ChargeScope.FEE)

        assertEquals("12,5 % - Cuota", BenefitText.discountDetail(discount, ClubCurrency.ARS))
        assertEquals(
            "12,5 % - Cuota - Inactivo",
            BenefitText.discountDetail(discount.copy(active = false), ClubCurrency.ARS)
        )
    }

    @Test
    fun discountDetail_muestraMontoFijoConMoneda() {
        val discount = Discount(2, "Jubilados", AdjustmentType.FIXED, 5000.0, ChargeScope.BOTH)

        assertEquals(
            "ARS 5.000,00 - Cuota y actividades",
            BenefitText.discountDetail(discount, ClubCurrency.ARS)
        )
    }
}
