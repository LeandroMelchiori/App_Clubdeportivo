package com.example.clubdeportivo

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onData
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.Visibility
import androidx.test.espresso.matcher.ViewMatchers.hasErrorText
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.runner.screenshot.Screenshot
import java.time.LocalDate
import org.hamcrest.Matchers.containsString
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.startsWith
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CobrosVisualInstrumentedTest {
    private lateinit var context: Context
    private lateinit var originalConfiguration: ClubConfiguration

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        originalConfiguration = DBHelper(context).use { it.obtenerConfiguracionClub() }
    }

    @After
    fun tearDown() {
        DBHelper(context).use { it.guardarConfiguracionClub(originalConfiguration) }
    }

    @Test
    fun configuracion_guardaRecargoYReglasDeBeneficios() {
        DBHelper(context).use { it.guardarConfiguracionClub(ClubConfiguration.DEFAULT) }
        val scenario = ActivityScenario.launch<ConfiguracionActivity>(
            Intent(context, ConfiguracionActivity::class.java).putExtra("usuario", "QA")
        )
        try {
            onView(withId(R.id.etTopeMesesRecargo))
                .check(matches(withEffectiveVisibility(Visibility.GONE)))
            onView(withId(R.id.spTipoRecargo)).perform(scrollTo(), click())
            onData(`is`(AdjustmentType.FIXED.label)).perform(click())
            onView(withId(R.id.etValorRecargo)).perform(scrollTo(), replaceText("1500"), closeSoftKeyboard())
            onView(withId(R.id.spFrecuenciaRecargo)).perform(scrollTo(), click())
            onData(`is`(LateFeeFrequency.MONTHLY.label)).perform(click())
            onView(withId(R.id.etTopeMesesRecargo)).perform(scrollTo(), replaceText("4"), closeSoftKeyboard())
            onView(withId(R.id.spAcumulacionBeneficios)).perform(scrollTo(), click())
            onData(`is`(BenefitStacking.COMBINED.label)).perform(click())
            onView(withId(R.id.cbVencidoPierdeDescuento)).perform(scrollTo(), click())
            onView(withId(R.id.etValorRecargo)).perform(scrollTo())
            captureScreen("configuracion_recargo")

            onView(withId(R.id.btnGuardarConfiguracion)).perform(scrollTo(), click())
            onView(withText(R.string.club_config_saved)).check(matches(isDisplayed()))
            onView(withId(R.id.btnGuardarConfiguracion)).perform(scrollTo())
            captureScreen("configuracion_beneficios")

            val rules = DBHelper(context).use { it.obtenerConfiguracionClub().billingRules }
            assertEquals(
                BillingRules(
                    lateFeeType = AdjustmentType.FIXED,
                    lateFeeValue = 1500.0,
                    lateFeeFrequency = LateFeeFrequency.MONTHLY,
                    lateFeeMaxMonths = 4,
                    benefitStacking = BenefitStacking.COMBINED,
                    scholarshipWaivesLateFee = true,
                    lateLosesDiscount = true
                ),
                rules
            )
        } finally {
            scenario.close()
        }
    }

    @Test
    fun descuentos_creaEditaYMuestraElCatalogo() {
        DBHelper(context).use { helper ->
            helper.writableDatabase.execSQL("DELETE FROM descuentos WHERE nombre LIKE 'Visual %'")
        }
        val configuracion = ActivityScenario.launch<ConfiguracionActivity>(
            Intent(context, ConfiguracionActivity::class.java).putExtra("usuario", "QA")
        )
        try {
            onView(withId(R.id.btnGestionDescuentos)).perform(scrollTo(), click())
            onView(withId(R.id.tvTitulo)).check(matches(withText(R.string.discounts_title)))
            onView(withId(R.id.fabNuevoDescuento)).perform(click())

            onView(withId(R.id.tvTituloFormulario)).check(matches(withText(R.string.new_discount_title)))
            onView(withId(R.id.btnGuardar)).perform(click())
            onView(withId(R.id.etNombreDescuento))
                .check(matches(hasErrorText("Ingresa el nombre del descuento")))
            onView(withId(R.id.etNombreDescuento)).perform(replaceText("Visual familiar"), closeSoftKeyboard())
            onView(withId(R.id.etValorDescuento)).perform(replaceText("150"), closeSoftKeyboard())
            onView(withId(R.id.btnGuardar)).perform(click())
            onView(withId(R.id.etValorDescuento))
                .check(matches(hasErrorText("El porcentaje no puede superar 100")))
            onView(withId(R.id.etValorDescuento)).perform(replaceText("15"), closeSoftKeyboard())
            onView(withId(R.id.spAlcanceDescuento)).perform(click())
            onData(`is`(ChargeScope.FEE.label)).perform(click())
            captureScreen("descuento_formulario")
            onView(withId(R.id.btnGuardar)).perform(click())

            onView(withText("Visual familiar")).check(matches(isDisplayed()))
            onView(withText("15 % - Cuota")).check(matches(isDisplayed()))
            captureScreen("descuentos_catalogo")

            val saved = DBHelper(context).use { helper ->
                helper.obtenerDescuentos().single { it.name == "Visual familiar" }
            }
            assertEquals(AdjustmentType.PERCENT, saved.type)
            assertEquals(15.0, saved.value, 0.0)
            assertEquals(ChargeScope.FEE, saved.scope)
        } finally {
            configuracion.close()
            DBHelper(context).use { helper ->
                helper.writableDatabase.execSQL("DELETE FROM descuentos WHERE nombre LIKE 'Visual %'")
            }
        }
    }

    @Test
    fun beneficios_asignaDescuentoYBecaDesdeVerMas() {
        val memberId = ensureMember(BENEFITS_DNI, "Marta", "Becada")
        val discountId = ensureVisualDiscount()
        DBHelper(context).use { it.guardarBeneficiosCliente(memberId, null, null) }
        val until = LocalDate.now().plusMonths(2)
        val scenario = ActivityScenario.launch<VerMasActivity>(
            Intent(context, VerMasActivity::class.java)
                .putExtra("usuario", "QA")
                .putExtra("dni", BENEFITS_DNI)
        )
        try {
            onView(withId(R.id.tvBeneficios)).perform(scrollTo())
                .check(matches(withText("Beneficios: ninguno")))
            onView(withId(R.id.btnBeneficios)).perform(scrollTo(), click())

            onView(withId(R.id.tvClienteBeneficios))
                .check(matches(withText("Becada, Marta - DNI $BENEFITS_DNI")))
            onView(withId(R.id.spDescuentoCliente)).perform(click())
            onData(startsWith("Visual familiar")).perform(click())
            onView(withId(R.id.cbTieneBeca)).perform(click())
            onView(withId(R.id.btnGuardar)).perform(scrollTo(), click())
            onView(withId(R.id.etPorcentajeBeca))
                .check(matches(hasErrorText("Ingresa un porcentaje entre 1 y 100")))
            onView(withId(R.id.etPorcentajeBeca)).perform(scrollTo(), replaceText("50"), closeSoftKeyboard())
            onView(withId(R.id.spAlcanceBeca)).perform(scrollTo(), click())
            onData(`is`(ChargeScope.FEE.label)).perform(click())
            onView(withId(R.id.etMotivoBeca))
                .perform(scrollTo(), replaceText("Deportista federado"), closeSoftKeyboard())
            onView(withId(R.id.etBecaHasta)).perform(scrollTo(), replaceText(until.toString()), closeSoftKeyboard())
            captureScreen("beneficios_cliente")
            onView(withId(R.id.btnGuardar)).perform(scrollTo(), click())

            onView(withId(R.id.tvBeneficios)).perform(scrollTo())
                .check(matches(withText(containsString("Beca 50 % (Cuota) hasta $until"))))
                .check(matches(withText(containsString("Descuento Visual familiar"))))
            onView(withId(R.id.btnBeneficios)).perform(scrollTo())
            captureScreen("ver_mas_beneficios")

            val benefits = DBHelper(context).use { it.obtenerBeneficiosCliente(memberId) }
            assertEquals(discountId, benefits.discount?.id)
            assertEquals(Scholarship(50, ChargeScope.FEE, "Deportista federado", until), benefits.scholarship)
        } finally {
            scenario.close()
            DBHelper(context).use { helper ->
                helper.guardarBeneficiosCliente(memberId, null, null)
                helper.writableDatabase.execSQL("DELETE FROM descuentos WHERE nombre LIKE 'Visual %'")
            }
        }
    }

    private fun ensureMember(dni: String, nombre: String, apellido: String): Int =
        DBHelper(context).use { helper ->
            val values = ContentValues().apply {
                put("nombre", nombre)
                put("apellido", apellido)
                put("dni", dni)
                put("fecha_nac", "1985-04-12")
                put("telefono", "3415550101")
                put("direccion", "San Martin 900")
                put("fecha_inscripcion", "2026-01-10")
                put("ficha_medica", 1)
                put("email", "$dni@example.com")
                put("esSocio", 1)
                put("activo", 1)
                put("carnet", 1)
            }
            helper.writableDatabase.insertWithOnConflict(
                "clientes",
                null,
                values,
                SQLiteDatabase.CONFLICT_IGNORE
            )
            requireNotNull(helper.obtenerPersonaPorDni(dni)?.id)
        }

    private fun ensureVisualDiscount(): Long = DBHelper(context).use { helper ->
        helper.obtenerDescuentos().firstOrNull { it.name == "Visual familiar" }?.id
            ?: helper.insertarDescuento(
                Discount(-1L, "Visual familiar", AdjustmentType.PERCENT, 15.0, ChargeScope.BOTH)
            )
    }

    private fun captureScreen(name: String) {
        Screenshot.capture().setName("${BuildConfig.FLAVOR}_$name").process()
    }

    private companion object {
        const val BENEFITS_DNI = "40999002"
    }
}
