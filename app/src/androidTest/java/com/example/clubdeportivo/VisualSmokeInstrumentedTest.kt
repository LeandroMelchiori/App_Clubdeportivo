package com.example.clubdeportivo

import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.action.ViewActions.click
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
            onView(withText("CONTRASE\u00d1A:")).check(matches(isDisplayed()))
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
            onView(withId(R.id.tvEstadoActividadesHoy)).check(matches(isDisplayed()))
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
            onView(withId(R.id.btnListVencimientos)).perform(click())
            onView(withId(R.id.panelFiltrosVencimientos)).check(matches(isDisplayed()))
            onView(withId(R.id.btnFiltroVencAlDia)).check(matches(isDisplayed()))
            onView(withId(R.id.btnFiltroVencPorVencer)).check(matches(isDisplayed()))
            onView(withId(R.id.btnListSocios)).check(matches(isDisplayed()))
            onView(withId(R.id.btnListNoSocios)).check(matches(isDisplayed()))
            onView(withId(R.id.svBuscar)).check(matches(isDisplayed()))
            onView(withId(R.id.tvResumenVencimientos)).check(matches(isDisplayed()))
            onView(withId(R.id.tvEstadoLista)).check(matches(isDisplayed()))
            onView(withId(R.id.btnExportarListados)).check(matches(isDisplayed()))
            onView(withId(R.id.rvNoSocios)).check(matches(isDisplayed()))
            captureScreen("listados")
        } finally {
            scenario.close()
        }
    }


    @Test
    fun verMas_muestraUsuarioSesion() {
        val intent = Intent(ApplicationProvider.getApplicationContext(), VerMasActivity::class.java)
            .putExtra("usuario", "QA")
            .putExtra("dni", "30111222")
        val scenario = ActivityScenario.launch<VerMasActivity>(intent)
        try {
            onView(withText("Bienvenido, QA")).check(matches(isDisplayed()))
            onView(withId(R.id.tvDNI)).check(matches(isDisplayed()))
            onView(withId(R.id.tvHistorialCuenta)).check(matches(isDisplayed()))
            captureScreen("ver_mas_usuario")
        } finally {
            scenario.close()
        }
    }


    @Test
    fun pagoCuota_muestraDatosYMediosDePago() {
        val intent = Intent(ApplicationProvider.getApplicationContext(), PagoDeCuotaActivity::class.java)
            .putExtra("usuario", "QA")
            .putExtra("nombre", "Perez, Juan")
            .putExtra("dni", "40111111")
            .putExtra("ultimoPago", "2026-07-01")
            .putExtra("tipoOperacion", "Cuota mensual")
            .putExtra("precio", "30000")
            .putExtra("esSocio", true)
        val scenario = ActivityScenario.launch<PagoDeCuotaActivity>(intent)
        try {
            onView(withId(R.id.rgMediosdePago)).check(matches(isDisplayed()))
            onView(withId(R.id.btnPagar)).check(matches(isDisplayed()))
            captureScreen("pago_cuota")
        } finally {
            scenario.close()
        }
    }


    @Test
    fun resumenMensual_muestraExportacion() {
        val intent = Intent(ApplicationProvider.getApplicationContext(), ResumenMensualActivity::class.java)
            .putExtra("usuario", "QA")
        val scenario = ActivityScenario.launch<ResumenMensualActivity>(intent)
        try {
            onView(withId(R.id.tvIngresosTotales)).check(matches(isDisplayed()))
            onView(withId(R.id.btnDescargar)).check(matches(isDisplayed()))
            captureScreen("resumen_mensual")
        } finally {
            scenario.close()
        }
    }

    @Test
    fun nuevoHorario_muestraFormularioPropio() {
        val intent = Intent(ApplicationProvider.getApplicationContext(), NuevoHorarioActividadActivity::class.java)
            .putExtra("usuario", "QA")
        val scenario = ActivityScenario.launch<NuevoHorarioActividadActivity>(intent)
        try {
            onView(withId(R.id.spActividad)).check(matches(isDisplayed()))
            onView(withId(R.id.spProfesor)).check(matches(isDisplayed()))
            onView(withId(R.id.spDia)).check(matches(isDisplayed()))
            onView(withId(R.id.btnIngresar)).check(matches(isDisplayed()))
            captureScreen("nuevo_horario")
        } finally {
            scenario.close()
        }
    }


    @Test
    fun configuracion_muestraAccionesPrincipales() {
        val intent = Intent(ApplicationProvider.getApplicationContext(), ConfiguracionActivity::class.java)
            .putExtra("usuario", "QA")
        val scenario = ActivityScenario.launch<ConfiguracionActivity>(intent)
        try {
            onView(withId(R.id.btnCerrarSesion)).check(matches(isDisplayed()))
            onView(withId(R.id.bottomNav)).check(matches(isDisplayed()))
            onView(withId(R.id.btnCerrarSesion)).perform(click())
            onView(withText(SessionDialogText.logoutMessage)).check(matches(isDisplayed()))
            captureScreen("configuracion")
        } finally {
            scenario.close()
        }
    }


    @Test
    fun editarUsuario_muestraFormularioPrincipal() {
        val intent = Intent(ApplicationProvider.getApplicationContext(), EditarUsuarioActivity::class.java)
            .putExtra("usuario", "QA")
            .putExtra("dni", "30111222")
            .putExtra("id", 2)
            .putExtra("esSocio", false)
        val scenario = ActivityScenario.launch<EditarUsuarioActivity>(intent)
        try {
            onView(withId(R.id.etNombre)).check(matches(isDisplayed()))
            onView(withId(R.id.etDni)).check(matches(isDisplayed()))
            onView(withId(R.id.btnConfirmar)).check(matches(isDisplayed()))
            captureScreen("editar_usuario")
        } finally {
            scenario.close()
        }
    }



    @Test
    fun editarActividad_muestraFormularioPrincipal() {
        val intent = Intent(ApplicationProvider.getApplicationContext(), EditarActividadActivity::class.java)
            .putExtra("usuario", "QA")
            .putExtra("dh_id", 1)
            .putExtra("id_actividad", 1)
            .putExtra("nombre_act", "Funcional")
            .putExtra("profesor", "Ana Perez")
            .putExtra("dia", 1)
            .putExtra("hora_inicio", 8 * 60)
            .putExtra("hora_fin", 9 * 60)
            .putExtra("precio", 5000.0)
        val scenario = ActivityScenario.launch<EditarActividadActivity>(intent)
        try {
            onView(withId(R.id.spActividad)).check(matches(isDisplayed()))
            onView(withId(R.id.spDia)).check(matches(isDisplayed()))
            onView(withId(R.id.spHoraInicio)).check(matches(isDisplayed()))
            onView(withId(R.id.btnIngresar)).check(matches(isDisplayed()))
            captureScreen("editar_actividad")
        } finally {
            scenario.close()
        }
    }


    @Test
    fun actividades_muestraControlesPrincipales() {
        val intent = Intent(ApplicationProvider.getApplicationContext(), ActividadesActivity::class.java)
            .putExtra("usuario", "QA")
        val scenario = ActivityScenario.launch<ActividadesActivity>(intent)
        try {
            onView(withId(R.id.tvBienvenida)).check(matches(isDisplayed()))
            onView(withId(R.id.btnAgregar)).check(matches(isDisplayed()))
            onView(withId(R.id.etBuscar)).check(matches(isDisplayed()))
            onView(withId(R.id.contenedorActividades)).check(matches(isDisplayed()))
            onView(withId(R.id.bottomNav)).check(matches(isDisplayed()))
            captureScreen("actividades")
        } finally {
            scenario.close()
        }
    }

    private fun captureScreen(name: String) {
        Screenshot.capture().setName(name).process()
    }
}
