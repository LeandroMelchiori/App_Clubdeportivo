package com.example.clubdeportivo

import android.app.Activity
import android.content.Context
import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.runner.screenshot.Screenshot
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HeaderDateInstrumentedTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun pantallasConBarraInferior_muestranLaFechaDeHoy() {
        val hoy = HeaderDateFormatter.format()
        val conOtraFecha = PantallasConBarraInferior.intents(context).mapNotNull { (nombre, intent) ->
            ActivityScenario.launch<Activity>(intent).use { scenario ->
                onView(withId(R.id.bottomNav)).check(matches(isDisplayed()))
                Screenshot.capture().setName("${BuildConfig.FLAVOR}_fecha_$nombre").process()
                var fecha = ""
                scenario.onActivity { activity ->
                    // Los formularios de clientes usan tvFecha para otro texto y tvFechaHoy en el encabezado.
                    val encabezado = activity.findViewById<TextView>(R.id.tvFechaHoy)
                        ?: activity.findViewById<TextView>(R.id.tvFecha)
                    fecha = encabezado.text.toString()
                }
                if (fecha == hoy) null else "$nombre: \"$fecha\""
            }
        }
        assertTrue(
            "Encabezados que no muestran \"$hoy\":\n${conOtraFecha.joinToString("\n")}",
            conOtraFecha.isEmpty()
        )
    }
}
