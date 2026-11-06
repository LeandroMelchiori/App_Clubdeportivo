package com.example.clubdeportivo

import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.database.sqlite.SQLiteDatabase
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
        ensureDetailClient()
        val invadidas = pantallasConBarraInferior().mapNotNull { (nombre, intent) ->
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

    private fun pantallasConBarraInferior(): List<Pair<String, Intent>> = listOf(
        "inicio" to intentDe(InicioActivity::class.java),
        "actividades" to intentDe(ActividadesActivity::class.java),
        "listados" to intentDe(ListadosActivity::class.java),
        "resumen_mensual" to intentDe(ResumenMensualActivity::class.java),
        "configuracion" to intentDe(ConfiguracionActivity::class.java),
        "editar_admin" to intentDe(EditarAdminActivity::class.java),
        "ver_mas" to intentDe(VerMasActivity::class.java).putExtra("dni", DNI_CLIENTE),
        "nuevo_usuario" to intentDe(NuevoUsuarioActivity::class.java),
        "editar_usuario" to intentDe(EditarUsuarioActivity::class.java)
            .putExtra("dni", DNI_CLIENTE)
            .putExtra("id", 2)
            .putExtra("esSocio", false),
        "pago_cuota" to intentDe(PagoDeCuotaActivity::class.java)
            .putExtra("nombre", "Perez, Juan")
            .putExtra("dni", "40111111")
            .putExtra("ultimoPago", "2026-07-01")
            .putExtra("tipoOperacion", "Cuota mensual")
            .putExtra("precio", "30000")
            .putExtra("esSocio", true),
        "pago_actividad" to intentDe(PagoActividadActivity::class.java)
            .putExtra("idActividad", 1)
            .putExtra("nombreActividad", "Funcional")
            .putExtra("horaInicio", "08:00")
            .putExtra("diaActividad", 1)
            .putExtra("precioActividad", 5000.0),
        "nuevo_horario" to intentDe(NuevoHorarioActividadActivity::class.java),
        "editar_actividad" to intentDe(EditarActividadActivity::class.java)
            .putExtra("dh_id", 1)
            .putExtra("id_actividad", 1)
            .putExtra("nombre_act", "Funcional")
            .putExtra("profesor", "Ana Perez")
            .putExtra("dia", 1)
            .putExtra("hora_inicio", 8 * 60)
            .putExtra("hora_fin", 9 * 60)
            .putExtra("precio", 5000.0)
    )

    private fun intentDe(activity: Class<out Activity>): Intent =
        Intent(context, activity).putExtra(SessionExtras.USUARIO, "QA")

    private fun ensureDetailClient() {
        DBHelper(context).use { helper ->
            val values = ContentValues().apply {
                put("nombre", "Usuario")
                put("apellido", "Visual")
                put("dni", DNI_CLIENTE)
                put("fecha_nac", "1990-01-01")
                put("telefono", "3415550000")
                put("direccion", "Domicilio de prueba")
                put("fecha_inscripcion", "2026-01-01")
                put("ficha_medica", 1)
                put("email", "visual@example.com")
                put("esSocio", 0)
                put("activo", 1)
                put("carnet", 0)
            }
            helper.writableDatabase.insertWithOnConflict(
                "clientes",
                null,
                values,
                SQLiteDatabase.CONFLICT_IGNORE
            )
        }
    }

    private companion object {
        const val DNI_CLIENTE = "30111222"
    }
}
