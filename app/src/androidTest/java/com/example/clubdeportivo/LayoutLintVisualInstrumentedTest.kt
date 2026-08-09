package com.example.clubdeportivo

import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.runner.screenshot.Screenshot
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LayoutLintVisualInstrumentedTest {
    @Test
    fun actividades_muestraListaConAlturaDinamica() {
        val intent = Intent(ApplicationProvider.getApplicationContext(), ActividadesActivity::class.java)
            .putExtra(SessionExtras.USUARIO, "QA")
        ActivityScenario.launch<ActividadesActivity>(intent).use {
            onView(withId(R.id.contenedorActividades)).check(matches(isDisplayed()))
            Screenshot.capture().setName("lint_actividades_lista").process()
        }
    }

    @Test
    fun resumenMensual_muestraControlesDeMesTintados() {
        val intent = Intent(ApplicationProvider.getApplicationContext(), ResumenMensualActivity::class.java)
            .putExtra(SessionExtras.USUARIO, "QA")
        ActivityScenario.launch<ResumenMensualActivity>(intent).use {
            onView(withId(R.id.btnMesAnterior)).check(matches(isDisplayed()))
            onView(withId(R.id.btnMesSiguiente)).check(matches(isDisplayed()))
            Screenshot.capture().setName("lint_resumen_controles").process()
        }
    }

    @Test
    fun inicio_muestraActividadesSinErroresDeInflado() {
        val intent = Intent(ApplicationProvider.getApplicationContext(), InicioActivity::class.java)
            .putExtra(SessionExtras.USUARIO, "QA")
        ActivityScenario.launch<InicioActivity>(intent).use {
            onView(withId(R.id.contenedorActividadesHoy)).check(matches(isDisplayed()))
            Screenshot.capture().setName("lint_inicio_actividades").process()
        }
    }
}
