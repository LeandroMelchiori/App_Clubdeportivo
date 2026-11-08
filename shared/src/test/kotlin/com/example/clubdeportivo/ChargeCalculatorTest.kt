package com.example.clubdeportivo

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChargeCalculatorTest {
    private val cuota = 30000.0
    private val vencimiento = LocalDate.parse("2026-09-10")
    private val familiar = Discount(1, "Grupo familiar", AdjustmentType.PERCENT, 15.0, ChargeScope.BOTH)
    private val jubilados = Discount(2, "Jubilados", AdjustmentType.FIXED, 5000.0, ChargeScope.FEE)
    private val becaMitad = Scholarship(50, ChargeScope.BOTH, "Deportista federado")
    private val recargoDiez = BillingRules(lateFeeType = AdjustmentType.PERCENT, lateFeeValue = 10.0)

    @Test
    fun sinBeneficiosNiRecargo_cobraLaBase() {
        val result = fee(ClientBenefits.NONE, paymentDate = vencimiento)

        assertEquals(cuota, result.total, 0.0)
        assertFalse(result.hasAdjustments)
    }

    @Test
    fun descuentoPorcentual_seAplicaSobreLaCuota() {
        val result = fee(ClientBenefits(discount = familiar))

        assertEquals("Grupo familiar", result.discountName)
        assertEquals(4500.0, result.discountAmount, 0.0)
        assertEquals(25500.0, result.total, 0.0)
    }

    @Test
    fun descuentoFijo_noSuperaLaBase() {
        val grande = jubilados.copy(value = 50000.0)

        val result = fee(ClientBenefits(discount = grande))

        assertEquals(cuota, result.discountAmount, 0.0)
        assertEquals(0.0, result.total, 0.0)
        assertTrue(result.isFree)
    }

    @Test
    fun descuentoInactivoOFueraDeAlcance_noSeAplica() {
        val inactivo = fee(ClientBenefits(discount = familiar.copy(active = false)))
        val soloActividades = fee(ClientBenefits(discount = familiar.copy(scope = ChargeScope.ACTIVITIES)))
        val cuotaEnActividad = ChargeCalculator.calculate(
            8000.0,
            ChargeKind.ACTIVITY,
            ClientBenefits(discount = jubilados),
            BillingRules.DEFAULT,
            ClubCurrency.ARS,
            vencimiento
        )

        assertEquals(cuota, inactivo.total, 0.0)
        assertEquals(cuota, soloActividades.total, 0.0)
        assertEquals(8000.0, cuotaEnActividad.total, 0.0)
        assertNull(cuotaEnActividad.discountName)
    }

    @Test
    fun becaTotal_dejaLaCuotaSinCargo() {
        val result = fee(ClientBenefits(scholarship = becaMitad.copy(percent = 100)))

        assertEquals(100, result.scholarshipPercent)
        assertEquals(0.0, result.total, 0.0)
        assertTrue(result.isFree)
    }

    @Test
    fun becaConFechaHasta_dejaDeAplicarseDespuesDeEsaFecha() {
        val beca = becaMitad.copy(until = LocalDate.parse("2026-09-30"))

        val ultimoDia = fee(ClientBenefits(scholarship = beca), paymentDate = LocalDate.parse("2026-09-30"))
        val vencida = fee(ClientBenefits(scholarship = beca), paymentDate = LocalDate.parse("2026-10-01"))

        assertEquals(15000.0, ultimoDia.total, 0.0)
        assertEquals(cuota, vencida.total, 0.0)
        assertNull(vencida.scholarshipPercent)
    }

    @Test
    fun soloElMayor_aplicaElBeneficioQueMasDescuenta() {
        val becaChica = becaMitad.copy(percent = 10)

        val ganaDescuento = fee(ClientBenefits(familiar, becaChica))
        val ganaBeca = fee(ClientBenefits(familiar, becaMitad))

        assertEquals("Grupo familiar", ganaDescuento.discountName)
        assertNull(ganaDescuento.scholarshipPercent)
        assertEquals(25500.0, ganaDescuento.total, 0.0)
        assertNull(ganaBeca.discountName)
        assertEquals(50, ganaBeca.scholarshipPercent)
        assertEquals(15000.0, ganaBeca.total, 0.0)
    }

    @Test
    fun sumar_aplicaPorcentajesPrimeroYDespuesMontosFijos() {
        val rules = BillingRules(benefitStacking = BenefitStacking.COMBINED)

        val porcentajes = fee(ClientBenefits(familiar, becaMitad), rules = rules)
        val fijo = fee(ClientBenefits(jubilados, becaMitad), rules = rules)
        val excedido = fee(ClientBenefits(jubilados.copy(value = 20000.0), becaMitad), rules = rules)

        assertEquals(15000.0, porcentajes.scholarshipAmount, 0.0)
        assertEquals(4500.0, porcentajes.discountAmount, 0.0)
        assertEquals(10500.0, porcentajes.total, 0.0)
        assertEquals(10000.0, fijo.total, 0.0)
        assertEquals(15000.0, excedido.discountAmount, 0.0)
        assertEquals(0.0, excedido.total, 0.0)
    }

    @Test
    fun recargo_noSeCobraHastaElVencimientoConGracia() {
        val result = fee(ClientBenefits.NONE, rules = recargoDiez, paymentDate = vencimiento)

        assertEquals(0, result.lateFeePeriods)
        assertEquals(cuota, result.total, 0.0)
    }

    @Test
    fun recargoUnico_seCalculaSobreElImporteBonificado() {
        val result = fee(
            ClientBenefits(discount = familiar),
            rules = recargoDiez,
            paymentDate = LocalDate.parse("2026-11-20")
        )

        assertEquals(1, result.lateFeePeriods)
        assertEquals(2550.0, result.lateFee, 0.0)
        assertEquals(28050.0, result.total, 0.0)
    }

    @Test
    fun recargoMensual_cuentaMesesOFraccionHastaElTope() {
        val rules = recargoDiez.copy(
            lateFeeType = AdjustmentType.FIXED,
            lateFeeValue = 1000.0,
            lateFeeFrequency = LateFeeFrequency.MONTHLY,
            lateFeeMaxMonths = 3
        )

        val unDia = fee(ClientBenefits.NONE, rules = rules, paymentDate = LocalDate.parse("2026-09-11"))
        val unMesJusto = fee(ClientBenefits.NONE, rules = rules, paymentDate = LocalDate.parse("2026-10-10"))
        val unMesYUnDia = fee(ClientBenefits.NONE, rules = rules, paymentDate = LocalDate.parse("2026-10-11"))
        val conTope = fee(ClientBenefits.NONE, rules = rules, paymentDate = LocalDate.parse("2027-03-01"))

        assertEquals(1, unDia.lateFeePeriods)
        assertEquals(1, unMesJusto.lateFeePeriods)
        assertEquals(2, unMesYUnDia.lateFeePeriods)
        assertEquals(2000.0, unMesYUnDia.lateFee, 0.0)
        assertEquals(3, conTope.lateFeePeriods)
        assertEquals(33000.0, conTope.total, 0.0)
    }

    @Test
    fun recargoEnCero_quedaDesactivado() {
        val result = fee(
            ClientBenefits.NONE,
            rules = BillingRules.DEFAULT,
            paymentDate = LocalDate.parse("2026-12-01")
        )

        assertEquals(0, result.lateFeePeriods)
        assertEquals(cuota, result.total, 0.0)
    }

    @Test
    fun becaVigente_eximeDelRecargoSegunLaConfiguracion() {
        val tarde = LocalDate.parse("2026-10-01")

        val exento = fee(ClientBenefits(scholarship = becaMitad), rules = recargoDiez, paymentDate = tarde)
        val sinExencion = fee(
            ClientBenefits(scholarship = becaMitad),
            rules = recargoDiez.copy(scholarshipWaivesLateFee = false),
            paymentDate = tarde
        )

        assertTrue(exento.lateFeeWaived)
        assertEquals(15000.0, exento.total, 0.0)
        assertFalse(sinExencion.lateFeeWaived)
        assertEquals(1500.0, sinExencion.lateFee, 0.0)
        assertEquals(16500.0, sinExencion.total, 0.0)
    }

    @Test
    fun pagoVencido_pierdeElDescuentoSoloSiLaReglaLoIndica() {
        val tarde = LocalDate.parse("2026-10-01")

        val conserva = fee(ClientBenefits(discount = familiar), rules = recargoDiez, paymentDate = tarde)
        val pierde = fee(
            ClientBenefits(discount = familiar),
            rules = recargoDiez.copy(lateLosesDiscount = true),
            paymentDate = tarde
        )

        assertEquals("Grupo familiar", conserva.discountName)
        assertEquals(28050.0, conserva.total, 0.0)
        assertNull(pierde.discountName)
        assertEquals(33000.0, pierde.total, 0.0)
    }

    @Test
    fun actividades_noTienenRecargo() {
        val result = ChargeCalculator.calculate(
            8000.0,
            ChargeKind.ACTIVITY,
            ClientBenefits(scholarship = becaMitad.copy(scope = ChargeScope.ACTIVITIES)),
            recargoDiez,
            ClubCurrency.ARS,
            paymentDate = LocalDate.parse("2026-12-01"),
            dueDate = vencimiento
        )

        assertEquals(0, result.lateFeePeriods)
        assertEquals(4000.0, result.total, 0.0)
    }

    @Test
    fun redondea_segunLosDecimalesDeLaMoneda() {
        val pesos = fee(ClientBenefits(discount = familiar), base = 10001.0)
        val chilenos = fee(ClientBenefits(discount = familiar), base = 10001.0, currency = ClubCurrency.CLP)

        assertEquals(1500.15, pesos.discountAmount, 0.0)
        assertEquals(8500.85, pesos.total, 0.0)
        assertEquals(1500.0, chilenos.discountAmount, 0.0)
        assertEquals(8501.0, chilenos.total, 0.0)
    }

    @Test
    fun monthsStarted_cuentaFraccionesComoMesCompleto() {
        assertEquals(0, ChargeCalculator.monthsStarted(vencimiento, vencimiento))
        assertEquals(1, ChargeCalculator.monthsStarted(vencimiento, LocalDate.parse("2026-09-11")))
        assertEquals(2, ChargeCalculator.monthsStarted(vencimiento, LocalDate.parse("2026-10-11")))
    }

    private fun fee(
        benefits: ClientBenefits,
        rules: BillingRules = BillingRules.DEFAULT,
        paymentDate: LocalDate = LocalDate.parse("2026-09-05"),
        base: Double = cuota,
        currency: ClubCurrency = ClubCurrency.ARS
    ): ChargeBreakdown = ChargeCalculator.calculate(
        base,
        ChargeKind.FEE,
        benefits,
        rules,
        currency,
        paymentDate,
        vencimiento
    )
}
