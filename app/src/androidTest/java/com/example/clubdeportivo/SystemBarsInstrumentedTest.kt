package com.example.clubdeportivo

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.view.View
import android.view.ViewGroup
import android.widget.ScrollView
import androidx.core.view.WindowInsetsCompat
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.screenshot.Screenshot
import com.google.android.material.bottomnavigation.BottomNavigationView
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SystemBarsInstrumentedTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun pantallasConBarraInferior_noDibujanContenidoBajoLaBarraDeEstado() {
        val invadidas = PantallasConBarraInferior.intents(context).mapNotNull { (nombre, intent) ->
            contenidoBajoLaBarraDeEstado(nombre, intent)
        }
        assertTrue(
            "Contenido bajo la barra de estado:\n${invadidas.joinToString("\n")}",
            invadidas.isEmpty()
        )
    }

    // Desplaza el contenido hasta el final, lo captura y compara el borde superior de cada
    // contenedor (que recorta a sus hijos) con el final de la barra de estado.
    private fun contenidoBajoLaBarraDeEstado(nombre: String, intent: Intent): String? {
        ActivityScenario.launch<Activity>(intent).use { scenario ->
            onView(withId(R.id.bottomNav)).check(matches(isDisplayed()))
            scenario.onActivity { activity ->
                scrollViewDe(activity.findViewById(R.id.tvBienvenida))
                    ?.let { it.scrollTo(0, it.getChildAt(0).height) }
            }
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()
            SystemClock.sleep(300)
            Screenshot.capture().setName("${BuildConfig.FLAVOR}_barra_estado_$nombre").process()

            var invasion: String? = null
            scenario.onActivity { activity ->
                val root = activity.findViewById<ViewGroup>(android.R.id.content)
                    .getChildAt(0) as ViewGroup
                val finBarraEstado = WindowInsetsCompat.toWindowInsetsCompat(root.rootWindowInsets, root)
                    .getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
                    .top
                val inicioContenido = root.hijos()
                    .filterNot { it is BottomNavigationView }
                    .minOf { it.topEnPantalla() }
                if (inicioContenido < finBarraEstado) {
                    invasion = "$nombre: contenido desde y=$inicioContenido px, " +
                        "barra de estado hasta y=$finBarraEstado px"
                }
            }
            return invasion
        }
    }

    private fun scrollViewDe(view: View): ScrollView? = when (view) {
        is ScrollView -> view
        else -> (view.parent as? View)?.let(::scrollViewDe)
    }

    private fun ViewGroup.hijos(): List<View> = (0 until childCount).map(::getChildAt)

    private fun View.topEnPantalla(): Int = IntArray(2).also(::getLocationOnScreen)[1]
}
