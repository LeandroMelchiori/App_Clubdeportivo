package com.example.clubdeportivo

import java.time.LocalDate
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

    @Test
    fun clientSummary_listaBecaYDescuentoVigentes() {
        val hoy = LocalDate.parse("2026-11-06")
        val benefits = ClientBenefits(
            discount = Discount(1, "Grupo familiar", AdjustmentType.PERCENT, 15.0, ChargeScope.BOTH),
            scholarship = Scholarship(
                50,
                ChargeScope.FEE,
                "Deportista federado",
                LocalDate.parse("2026-12-31")
            )
        )

        assertEquals("Beneficios: ninguno", BenefitText.clientSummary(ClientBenefits.NONE, ClubCurrency.ARS, hoy))
        assertEquals(
            "Beneficios:\nBeca 50 % (Cuota) hasta 2026-12-31: Deportista federado\n" +
                "Descuento Grupo familiar: 15 % - Cuota y actividades",
            BenefitText.clientSummary(benefits, ClubCurrency.ARS, hoy)
        )
    }

    @Test
    fun clientSummary_avisaCuandoLaBecaVencio() {
        val benefits = ClientBenefits(
            scholarship = Scholarship(100, ChargeScope.BOTH, "Convenio", LocalDate.parse("2026-10-31"))
        )

        assertEquals(
            "Beneficios:\nBeca 100 % (Cuota y actividades) - vencida el 2026-10-31: Convenio",
            BenefitText.clientSummary(benefits, ClubCurrency.ARS, LocalDate.parse("2026-11-06"))
        )
    }
}
