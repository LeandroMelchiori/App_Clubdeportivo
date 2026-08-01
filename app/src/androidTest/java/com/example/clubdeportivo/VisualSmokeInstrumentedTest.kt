package com.example.clubdeportivo

import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.runner.screenshot.Screenshot
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class VisualSmokeInstrumentedTest {
    @Test
    fun login_muestraCamposPrincipales() {
        val scenario = ActivityScenario.launch(LoginActivity::class.java)
        try {
            onView(withText("CLUB DEPORTIVO")).check(matches(isDisplayed()))
            onView(withId(R.id.etUsuario)).check(matches(isDisplayed()))
            onView(withId(R.id.etContrasena)).check(matches(isDisplayed()))
            onView(withId(R.id.btnLogin)).check(matches(isDisplayed()))
            captureScreen("login")
        } finally {
            scenario.close()
        }
    }

    @Test
    fun inicio_muestraDashboardYNavegacion() {
        val intent = Intent(ApplicationProvider.getApplicationContext(), InicioActivity::class.java)
            .putExtra("usuario", "QA")
        val scenario = ActivityScenario.launch<InicioActivity>(intent)
        try {
            onView(withId(R.id.tvBienvenida)).check(matches(isDisplayed()))
            onView(withText("Nuevo Usuario")).check(matches(isDisplayed()))
            onView(withId(R.id.panelMetricas)).check(matches(isDisplayed()))
            onView(withId(R.id.tvIngresosMes)).check(matches(isDisplayed()))
            onView(withId(R.id.contenedorActividadesHoy)).check(matches(isDisplayed()))
            onView(withId(R.id.bottomNav)).check(matches(isDisplayed()))
            captureScreen("inicio")
        } finally {
            scenario.close()
        }
    }

    @Test
    fun listados_muestraControlesYListaInicial() {
        val intent = Intent(ApplicationProvider.getApplicationContext(), ListadosActivity::class.java)
            .putExtra("usuario", "QA")
        val scenario = ActivityScenario.launch<ListadosActivity>(intent)
        try {
            onView(withId(R.id.btnListVencimientos)).check(matches(isDisplayed()))
            onView(withId(R.id.btnListSocios)).check(matches(isDisplayed()))
            onView(withId(R.id.btnListNoSocios)).check(matches(isDisplayed()))
            onView(withId(R.id.svBuscar)).check(matches(isDisplayed()))
            onView(withId(R.id.tvResumenVencimientos)).check(matches(isDisplayed()))
            onView(withId(R.id.rvNoSocios)).check(matches(isDisplayed()))
            captureScreen("listados")
        } finally {
            scenario.close()
        }
    }

    private fun captureScreen(name: String) {
        Screenshot.capture().setName(name).process()
    }
}
