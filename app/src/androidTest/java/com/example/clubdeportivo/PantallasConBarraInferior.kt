package com.example.clubdeportivo

import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.database.sqlite.SQLiteDatabase

// Pantallas con BottomNavigationView y los extras minimos para abrirlas.
object PantallasConBarraInferior {
    private const val DNI_CLIENTE = "30111222"

    fun intents(context: Context): List<Pair<String, Intent>> {
        asegurarClienteDeDetalle(context)
        fun intentDe(activity: Class<out Activity>): Intent =
            Intent(context, activity).putExtra(SessionExtras.USUARIO, "QA")

        return listOf(
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
    }

    // Ver mas vuelve a Listados si el cliente no existe, y production arranca sin datos.
    private fun asegurarClienteDeDetalle(context: Context) {
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
}
