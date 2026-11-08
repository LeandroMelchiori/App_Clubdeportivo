package com.example.clubdeportivo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DiscountValidatorTest {
    @Test
    fun validate_normalizaUnDescuentoValido() {
        val result = validate(name = "  Grupo familiar ", value = "12,5")

        assertNull(result.error)
        assertEquals(
            Discount(7, "Grupo familiar", AdjustmentType.PERCENT, 12.5, ChargeScope.BOTH, true),
            result.discount
        )
    }

    @Test
    fun validate_exigeNombre() {
        assertEquals(DiscountValidator.Field.NAME, validate(name = " ").field)
        assertEquals(DiscountValidator.Field.NAME, validate(name = "x".repeat(61)).field)
    }

    @Test
    fun validate_exigeValorPositivo() {
        assertEquals(DiscountValidator.Field.VALUE, validate(value = "").field)
        assertEquals(DiscountValidator.Field.VALUE, validate(value = "0").field)
        assertEquals("Ingresa un valor mayor a cero", validate(value = "abc").error)
    }

    @Test
    fun validate_limitaElPorcentajeANoMasDeCien() {
        val porcentaje = validate(value = "101")
        val montoFijo = validate(type = AdjustmentType.FIXED, value = "5.000")

        assertEquals("El porcentaje no puede superar 100", porcentaje.error)
        assertEquals(5000.0, montoFijo.discount?.value ?: 0.0, 0.0)
    }

    private fun validate(
        name: String = "Grupo familiar",
        type: AdjustmentType = AdjustmentType.PERCENT,
        value: String = "15"
    ) = DiscountValidator.validate(7, name, type, value, ChargeScope.BOTH, active = true)
}
