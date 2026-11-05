package com.example.clubdeportivo

import android.content.ContentValues
import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.matcher.ViewMatchers.Visibility
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.espresso.matcher.ViewMatchers.withContentDescription
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.runner.screenshot.Screenshot
import java.io.File
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
            onView(withText("INGRESAR")).check(matches(isDisplayed()))
            if (BuildConfig.DEMO_MODE) {
                onView(withId(R.id.tvEnvironmentBadge)).check(matches(isDisplayed()))
                onView(withText("MODO DEMO")).check(matches(isDisplayed()))
            } else {
                onView(withId(R.id.tvEnvironmentBadge))
                    .check(matches(withEffectiveVisibility(Visibility.GONE)))
            }
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
            onView(withId(R.id.rvNoSocios)).check(matches(isDisplayed()))
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
            onView(withId(R.id.rvVencimientos)).check(matches(isDisplayed()))
            captureScreen("listados")
        } finally {
            scenario.close()
        }
    }


    @Test
    fun verMas_muestraUsuarioSesion() {
        ensureDetailClient()
        val intent = Intent(ApplicationProvider.getApplicationContext(), VerMasActivity::class.java)
            .putExtra("usuario", "QA")
            .putExtra("dni", "30111222")
        val scenario = ActivityScenario.launch<VerMasActivity>(intent)
        try {
            onView(withText("Bienvenido, QA")).check(matches(isDisplayed()))
            onView(withId(R.id.tvFecha)).check(matches(withText(HeaderDateFormatter.format())))
            onView(withId(R.id.tvDNI)).check(matches(isDisplayed()))
            onView(withId(R.id.tvHistorialCuenta)).perform(scrollTo()).check(matches(isDisplayed()))
            onView(withId(R.id.btnHistorialTodos)).perform(scrollTo()).check(matches(isDisplayed()))
            onView(withId(R.id.btnHistorialCuotas)).perform(scrollTo()).check(matches(isDisplayed()))
            onView(withId(R.id.btnHistorialActividades)).perform(scrollTo()).check(matches(isDisplayed()))
            onView(withContentDescription(AccessibilityText.deletePerson)).perform(scrollTo())
                .check(matches(isDisplayed()))
            onView(withId(R.id.btnEliminar)).perform(click())
            onView(withText(DeletePersonDialogText.title)).check(matches(isDisplayed()))
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
            onView(withText("Método de pago:")).check(matches(isDisplayed()))
            onView(withId(R.id.rgMediosdePago)).check(matches(isDisplayed()))
            onView(withId(R.id.btnPagar)).check(matches(isDisplayed()))
            captureScreen("pago_cuota")
        } finally {
            scenario.close()
        }
    }



    @Test
    fun pagoActividad_muestraDatosYMediosDePago() {
        val intent = Intent(ApplicationProvider.getApplicationContext(), PagoActividadActivity::class.java)
            .putExtra("usuario", "QA")
            .putExtra("idActividad", 1)
            .putExtra("nombreActividad", "Funcional")
            .putExtra("horaInicio", "08:00")
            .putExtra("diaActividad", 1)
            .putExtra("precioActividad", 5000.0)
        val scenario = ActivityScenario.launch<PagoActividadActivity>(intent)
        try {
            onView(withText("Método de pago:")).check(matches(isDisplayed()))
            onView(withId(R.id.rgMedioPago)).check(matches(isDisplayed()))
            onView(withId(R.id.btnPagar)).check(matches(isDisplayed()))
            captureScreen("pago_actividad")
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
            onView(withId(R.id.btnIngresar)).perform(scrollTo()).check(matches(isDisplayed()))
            captureScreen("nuevo_horario")
        } finally {
            scenario.close()
        }
    }


    @Test
    fun configuracion_muestraYGuardaDatosDelClub() {
        val intent = Intent(ApplicationProvider.getApplicationContext(), ConfiguracionActivity::class.java)
            .putExtra("usuario", "QA")
        val scenario = ActivityScenario.launch<ConfiguracionActivity>(intent)
        try {
            onView(withId(R.id.ivClubLogo)).check(matches(isDisplayed()))
            onView(withId(R.id.etClubNombre)).check(matches(isDisplayed()))
            onView(withId(R.id.spClubMoneda)).perform(scrollTo()).check(matches(isDisplayed()))
            onView(withId(R.id.bottomNav)).check(matches(isDisplayed()))
            captureScreen("configuracion_identidad")
            onView(withId(R.id.btnGuardarConfiguracion)).perform(scrollTo(), click())
            onView(withText(R.string.club_config_saved)).check(matches(isDisplayed()))
            captureScreen("configuracion_formulario")
            onView(withId(R.id.btnGestionProfesores)).perform(scrollTo())
                .check(matches(isDisplayed()))
            onView(withId(R.id.btnGestionActividades)).perform(scrollTo())
                .check(matches(isDisplayed()))
            onView(withId(R.id.btnImportarClientes)).perform(scrollTo())
                .check(matches(isDisplayed()))
            onView(withId(R.id.btnCerrarSesion)).perform(scrollTo(), click())
            onView(withText(SessionDialogText.logoutMessage)).check(matches(isDisplayed()))
            captureScreen("configuracion_logout")
        } finally {
            scenario.close()
        }
    }



    @Test
    fun nuevoUsuario_muestraFormularioPrincipal() {
        val intent = Intent(ApplicationProvider.getApplicationContext(), NuevoUsuarioActivity::class.java)
            .putExtra("usuario", "QA")
        val scenario = ActivityScenario.launch<NuevoUsuarioActivity>(intent)
        try {
            onView(withText("Fecha de nacimiento:")).check(matches(isDisplayed()))
            onView(withId(R.id.etNombre)).check(matches(isDisplayed()))
            onView(withId(R.id.etDNI)).check(matches(isDisplayed()))
            onView(withId(R.id.btnRegistrar)).perform(scrollTo()).check(matches(isDisplayed()))
            captureScreen("nuevo_usuario")
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
            onView(withText("Fecha de nacimiento:")).check(matches(isDisplayed()))
            onView(withId(R.id.etNombre)).check(matches(isDisplayed()))
            onView(withId(R.id.etDni)).check(matches(isDisplayed()))
            onView(withId(R.id.btnConfirmar)).perform(scrollTo()).check(matches(isDisplayed()))
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
            onView(withId(R.id.btnIngresar)).perform(scrollTo()).check(matches(isDisplayed()))
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
            onView(withContentDescription(AccessibilityText.addActivity)).check(matches(isDisplayed()))
            onView(withId(R.id.etBuscar)).check(matches(isDisplayed()))
            onView(withId(R.id.tvEstadoActividades)).check(matches(isDisplayed()))
            onView(withId(R.id.contenedorActividades)).check(matches(isDisplayed()))
            onView(withId(R.id.bottomNav)).check(matches(isDisplayed()))
            captureScreen("actividades")
        } finally {
            scenario.close()
        }
    }

    @Test
    fun profesores_muestraListadoYAccionDeAlta() {
        ensureCatalogManagementData()
        val scenario = ActivityScenario.launch(ProfesoresActivity::class.java)
        try {
            onView(withId(R.id.tvTitulo)).check(matches(withText("Profesores")))
            onView(withId(R.id.rvProfesores)).check(matches(isDisplayed()))
            onView(withId(R.id.fabNuevoProfesor)).check(matches(isDisplayed()))
            captureScreen("profesores")
        } finally {
            scenario.close()
        }
    }

    @Test
    fun catalogoActividades_muestraListadoYAccionDeAlta() {
        ensureCatalogManagementData()
        val scenario = ActivityScenario.launch(CatalogoActividadesActivity::class.java)
        try {
            onView(withId(R.id.tvTitulo)).check(matches(withText("Catalogo de actividades")))
            onView(withId(R.id.rvCatalogo)).check(matches(isDisplayed()))
            onView(withId(R.id.fabNuevaActividad)).check(matches(isDisplayed()))
            captureScreen("catalogo_actividades")
        } finally {
            scenario.close()
        }
    }

    @Test
    fun formularioProfesor_muestraDatosProfesionalesYAcciones() {
        val scenario = ActivityScenario.launch(FormularioProfesorActivity::class.java)
        try {
            onView(withId(R.id.tvTituloFormulario)).check(matches(withText("Nuevo profesor")))
            onView(withId(R.id.etDni)).check(matches(isDisplayed()))
            onView(withId(R.id.etNombre)).check(matches(isDisplayed()))
            onView(withId(R.id.etFechaNacimiento)).perform(scrollTo())
                .check(matches(isDisplayed()))
            captureScreen("formulario_profesor_datos")
            onView(withId(R.id.btnGuardar)).perform(scrollTo())
                .check(matches(isDisplayed()))
            onView(withId(R.id.btnCancelar)).check(matches(isDisplayed()))
            captureScreen("formulario_profesor_acciones")
        } finally {
            scenario.close()
        }
    }

    @Test
    fun formularioCatalogo_muestraDatosYAcciones() {
        val scenario = ActivityScenario.launch(FormularioCatalogoActividadActivity::class.java)
        try {
            onView(withId(R.id.tvTituloFormulario)).check(matches(withText("Nueva actividad")))
            onView(withId(R.id.etNombreActividad)).check(matches(isDisplayed()))
            onView(withId(R.id.etPrecio)).check(matches(isDisplayed()))
            onView(withId(R.id.btnGuardar)).check(matches(isDisplayed()))
            onView(withId(R.id.btnCancelar)).check(matches(isDisplayed()))
            captureScreen("formulario_catalogo")
        } finally {
            scenario.close()
        }
    }

    @Test
    fun importarClientes_muestraFormatoYAcciones() {
        val scenario = ActivityScenario.launch(ImportarClientesActivity::class.java)
        try {
            onView(withId(R.id.tvTitulo)).check(matches(withText("Importar clientes")))
            onView(withId(R.id.tvFormatoArchivo)).check(matches(isDisplayed()))
            onView(withId(R.id.btnSeleccionarArchivo)).perform(scrollTo())
                .check(matches(isDisplayed()))
            onView(withId(R.id.btnDescargarPlantilla)).check(matches(isDisplayed()))
            onView(withId(R.id.cardResumen)).check(matches(withEffectiveVisibility(Visibility.GONE)))
            captureScreen("importar_clientes")
        } finally {
            scenario.close()
        }
    }

    @Test
    fun importarClientes_previsualizaFilasValidasErroresYConfirmacion() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val file = File(context.cacheDir, "clientes_visual.csv").apply {
            writeText(
                "dni;nombre;apellido;fecha_nacimiento;telefono;direccion;email\n" +
                    "39000001;Lucia;Fernandez;12/03/1994;3415552001;Mitre 100;lucia.visual@example.com\n" +
                    "39000002;Tomas;Rios;28/09/1988;3415552002;Oroño 450;tomas.visual@example.com\n" +
                    "12;Sofia;;31/02/1990;123;Belgrano 20;sofia-sin-arroba\n"
            )
        }
        val intent = Intent(context, ImportarClientesActivity::class.java).setData(Uri.fromFile(file))
        val scenario = ActivityScenario.launch<ImportarClientesActivity>(intent)
        try {
            onView(withId(R.id.tvArchivo)).check(matches(withText("clientes_visual.csv")))
            onView(withId(R.id.tvResumen))
                .check(matches(withText("2 filas listas para importar · 1 fila con errores")))
            onView(withId(R.id.rvFilasImportacion)).check(matches(isDisplayed()))
            onView(withId(R.id.btnImportar)).check(matches(withText("Importar 2 clientes")))
            captureScreen("importar_clientes_previsualizacion")

            onView(withId(R.id.btnFiltroErrores)).perform(click())
            onView(withText("Fila 4")).check(matches(isDisplayed()))
            captureScreen("importar_clientes_errores")

            onView(withId(R.id.btnImportar)).perform(click())
            onView(withText(R.string.import_clients_confirm_title)).check(matches(isDisplayed()))
            captureScreen("importar_clientes_confirmacion")
            onView(withText(R.string.action_cancel)).perform(click())
        } finally {
            scenario.close()
            file.delete()
        }
    }

    private fun ensureCatalogManagementData() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        DBHelper(context).use { helper ->
            if (helper.obtenerProfesor("30123456") == null) {
                helper.insertarProfesor(
                    DBHelper.Profesor(
                        dni = "30123456",
                        nombre = "Laura",
                        apellido = "Gomez",
                        fechaNac = "1991-05-20",
                        telefono = "3415551000",
                        direccion = "Calle 10",
                        fechaInscripcion = "2026-09-01",
                        fichaMedica = true,
                        email = "laura@example.com",
                        activo = true,
                        titulo = "Entrenadora"
                    )
                )
            }
            if (helper.obtenerCatalogoActividades().none { it.nombre == "Pilates visual" }) {
                helper.insertarCatalogoActividad(
                    DBHelper.CatalogoActividad(-1L, "Pilates visual", 8500.0)
                )
            }
        }
    }

    private fun ensureDetailClient() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        DBHelper(context).use { helper ->
            val values = ContentValues().apply {
                put("nombre", "Usuario")
                put("apellido", "Visual")
                put("dni", "30111222")
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

    private fun captureScreen(name: String) {
        Screenshot.capture().setName("${BuildConfig.FLAVOR}_$name").process()
    }
}
