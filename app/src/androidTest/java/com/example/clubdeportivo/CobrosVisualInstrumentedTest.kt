package com.example.clubdeportivo

import android.content.Context
import android.content.Intent
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
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.runner.screenshot.Screenshot
import org.hamcrest.Matchers.`is`
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

    private fun captureScreen(name: String) {
        Screenshot.capture().setName("${BuildConfig.FLAVOR}_$name").process()
    }
}
