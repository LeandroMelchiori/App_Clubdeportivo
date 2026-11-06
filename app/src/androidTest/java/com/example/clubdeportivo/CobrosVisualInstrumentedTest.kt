package com.example.clubdeportivo

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.view.View
import android.widget.SearchView
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onData
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.Visibility
import androidx.test.espresso.matcher.ViewMatchers.hasErrorText
import androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom
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
import org.hamcrest.Matcher
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

    @Test
    fun pagoCuota_muestraDesgloseYRegistraDescuentoConRecargo() {
        val memberId = ensureMember(LATE_DNI, "Diego", "Moroso")
        val discountId = ensureVisualDiscount()
        DBHelper(context).use { helper ->
            helper.guardarConfiguracionClub(
                originalConfiguration.copy(billingRules = BillingRules(lateFeeValue = 10.0))
            )
            helper.guardarBeneficiosCliente(memberId, discountId, null)
        }
        resetFees(memberId, LocalDate.now().minusMonths(2))
        val scenario = launchFeePayment(LATE_DNI, "Moroso, Diego", isMember = true)
        try {
            onView(withId(R.id.tvPrecio)).check(matches(withEffectiveVisibility(Visibility.GONE)))
            onView(withId(R.id.tvDesglose)).check(
                matches(
                    withText(
                        "Cuota base: ARS 30.000,00\n" +
                            "Descuento Visual familiar: -ARS 4.500,00\n" +
                            "Recargo por mora: ARS 2.550,00\n" +
                            "Total a cobrar: ARS 28.050,00"
                    )
                )
            )
            onView(withId(R.id.rbEfectivo)).perform(click())
            captureScreen("pago_cuota_desglose")
            onView(withId(R.id.btnPagar)).perform(scrollTo(), click())
            onView(withText(containsString("ARS 28.050,00"))).check(matches(isDisplayed()))
            captureScreen("pago_cuota_confirmacion")
            onView(withText(PaymentDialogText.confirm)).perform(click())

            assertLastFee(memberId, total = 28050.0, method = "Efectivo")
        } finally {
            scenario.close()
            DBHelper(context).use { helper ->
                helper.guardarBeneficiosCliente(memberId, null, null)
                helper.writableDatabase.execSQL("DELETE FROM descuentos WHERE nombre LIKE 'Visual %'")
            }
        }
    }

    @Test
    fun pagoCuota_conBecaTotalRegistraSinCargo() {
        val memberId = ensureMember(FREE_DNI, "Lucia", "Becada")
        DBHelper(context).use { helper ->
            helper.guardarBeneficiosCliente(
                memberId,
                null,
                Scholarship(100, ChargeScope.BOTH, "Convenio con la escuela")
            )
        }
        resetFees(memberId, LocalDate.now().minusDays(3))
        val scenario = launchFeePayment(FREE_DNI, "Becada, Lucia", isMember = true)
        try {
            onView(withId(R.id.tvDesglose)).check(
                matches(withText(containsString("Beca 100 %: -ARS 30.000,00")))
            )
            onView(withId(R.id.rgMediosdePago)).check(matches(withEffectiveVisibility(Visibility.GONE)))
            onView(withId(R.id.btnPagar)).check(matches(withText(R.string.register_no_charge)))
            captureScreen("pago_cuota_beca_total")
            onView(withId(R.id.btnPagar)).perform(scrollTo(), click())
            onView(withText(PaymentDialogText.cuotaSinCargo())).check(matches(isDisplayed()))
            onView(withText(PaymentDialogText.confirm)).perform(click())

            assertLastFee(memberId, total = 0.0, method = PaymentDbRules.noChargeMethod)
        } finally {
            scenario.close()
            DBHelper(context).use { it.guardarBeneficiosCliente(memberId, null, null) }
        }
    }

    @Test
    fun pagoActividad_aplicaBecaDeActividadesAlNoSocio() {
        val clientId = ensureMember(ACTIVITY_DNI, "Tomas", "Becado", isMember = false)
        val scheduleId = ensureVisualSchedule()
        DBHelper(context).use { helper ->
            helper.guardarBeneficiosCliente(
                clientId,
                null,
                Scholarship(50, ChargeScope.ACTIVITIES, "Convenio escolar")
            )
            helper.writableDatabase.execSQL(
                "DELETE FROM pagos_actividad WHERE idCliente = ?",
                arrayOf(clientId.toString())
            )
        }
        val scenario = ActivityScenario.launch<PagoActividadActivity>(
            Intent(context, PagoActividadActivity::class.java)
                .putExtra("usuario", "QA")
                .putExtra("idActividad", scheduleId)
                .putExtra("nombreActividad", "Visual pilates")
                .putExtra("horaInicio", "07:00")
                .putExtra("diaActividad", 6)
        )
        try {
            onView(withId(R.id.tvDesglose)).check(matches(withEffectiveVisibility(Visibility.GONE)))
            onView(withId(R.id.etBuscar)).perform(submitSearch(ACTIVITY_DNI))
            onView(withId(R.id.tvDesglose)).check(
                matches(
                    withText(
                        "Precio base: ARS 8.000,00\n" +
                            "Beca 50 %: -ARS 4.000,00\n" +
                            "Total a cobrar: ARS 4.000,00"
                    )
                )
            )
            onView(withId(R.id.rbTransferencia)).perform(click())
            captureScreen("pago_actividad_desglose")
            onView(withId(R.id.btnPagar)).perform(scrollTo(), click())
            onView(withText(containsString("ARS 4.000,00"))).check(matches(isDisplayed()))
            onView(withText(PaymentDialogText.confirm)).perform(click())

            DBHelper(context).use { helper ->
                helper.readableDatabase.rawQuery(
                    "SELECT monto, monto_base, forma_pago FROM pagos_actividad WHERE idCliente = ?",
                    arrayOf(clientId.toString())
                ).use { cursor ->
                    assertEquals(true, cursor.moveToFirst())
                    assertEquals(4000.0, cursor.getDouble(0), 0.0)
                    assertEquals(8000.0, cursor.getDouble(1), 0.0)
                    assertEquals("Transferencia", cursor.getString(2))
                }
            }
        } finally {
            scenario.close()
            DBHelper(context).use { it.guardarBeneficiosCliente(clientId, null, null) }
        }
    }

    private fun launchFeePayment(dni: String, name: String, isMember: Boolean) =
        ActivityScenario.launch<PagoDeCuotaActivity>(
            Intent(context, PagoDeCuotaActivity::class.java)
                .putExtra("usuario", "QA")
                .putExtra("nombre", name)
                .putExtra("dni", dni)
                .putExtra("esSocio", isMember)
        )

    private fun resetFees(memberId: Int, dueDate: LocalDate) {
        DBHelper(context).use { helper ->
            helper.writableDatabase.execSQL("DELETE FROM cuotas WHERE idCliente = ?", arrayOf(memberId))
            helper.writableDatabase.insert(
                "cuotas",
                null,
                ContentValues().apply {
                    put("idCliente", memberId)
                    put("monto", 30000.0)
                    put("monto_base", 30000.0)
                    put("fechaPago", dueDate.minusMonths(1).toString())
                    put("formaPago", "Efectivo")
                    put("estadoDelPago", 1)
                    put("fechaVencimiento", dueDate.toString())
                }
            )
        }
    }

    private fun assertLastFee(memberId: Int, total: Double, method: String) {
        DBHelper(context).use { helper ->
            helper.readableDatabase.rawQuery(
                """
                    SELECT monto, formaPago, fechaPago
                    FROM cuotas
                    WHERE idCliente = ?
                    ORDER BY fechaPago DESC, idCuota DESC
                    LIMIT 1
                """.trimIndent(),
                arrayOf(memberId.toString())
            ).use { cursor ->
                assertEquals(true, cursor.moveToFirst())
                assertEquals(total, cursor.getDouble(0), 0.0)
                assertEquals(method, cursor.getString(1))
                assertEquals(LocalDate.now().toString(), cursor.getString(2))
            }
        }
    }

    private fun ensureVisualSchedule(): Int = DBHelper(context).use { helper ->
        val activityId = helper.obtenerCatalogoActividades().firstOrNull { it.nombre == "Visual pilates" }?.id
            ?: helper.insertarCatalogoActividad(DBHelper.CatalogoActividad(-1L, "Visual pilates", 8000.0))
        if (helper.obtenerProfesor(VISUAL_PROFESSOR_DNI) == null) {
            helper.insertarProfesor(
                DBHelper.Profesor(
                    dni = VISUAL_PROFESSOR_DNI,
                    nombre = "Paula",
                    apellido = "Visual",
                    fechaNac = "1990-03-10",
                    telefono = "3415551999",
                    direccion = "Calle 20",
                    fechaInscripcion = "2026-01-01",
                    fichaMedica = true,
                    email = "paula.visual@example.com",
                    activo = true,
                    titulo = "Profesora"
                )
            )
        }
        helper.readableDatabase.rawQuery(
            """
                SELECT dh.id
                FROM dias_horarios dh
                JOIN actividad_profesor ap ON ap.id = dh.actividad_profesor_id
                WHERE ap.actividad_id = ? AND dh.activo = 1
                LIMIT 1
            """.trimIndent(),
            arrayOf(activityId.toString())
        ).use { cursor ->
            if (cursor.moveToFirst()) return@use cursor.getInt(0)
            helper.insertarHorario(activityId, VISUAL_PROFESSOR_DNI, 6, 7 * 60, 8 * 60).toInt()
        }
    }

    private fun submitSearch(query: String) = object : ViewAction {
        override fun getConstraints(): Matcher<View> = isAssignableFrom(SearchView::class.java)

        override fun getDescription(): String = "buscar $query"

        override fun perform(uiController: UiController, view: View) {
            (view as SearchView).setQuery(query, true)
            uiController.loopMainThreadUntilIdle()
        }
    }

    private fun ensureMember(
        dni: String,
        nombre: String,
        apellido: String,
        isMember: Boolean = true
    ): Int =
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
                put("esSocio", if (isMember) 1 else 0)
                put("activo", 1)
                put("carnet", if (isMember) 1 else 0)
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
        const val LATE_DNI = "40999003"
        const val FREE_DNI = "40999004"
        const val ACTIVITY_DNI = "30999005"
        const val VISUAL_PROFESSOR_DNI = "29888777"
    }
}
