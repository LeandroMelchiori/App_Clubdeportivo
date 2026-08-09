package com.example.clubdeportivo

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton

class EditarActividadActivity : AppCompatActivity() {
    private lateinit var spActividad: Spinner
    private lateinit var spProfesor: Spinner
    private lateinit var spDia: Spinner
    private lateinit var spHoraInicio: Spinner
    private lateinit var spHoraFin: Spinner
    private lateinit var etPrecio: EditText
    private lateinit var btnGuardar: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        val db = DBHelper(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_editar_actividad)

        // --- refs UI ---
        spActividad  = findViewById(R.id.spActividad)
        spProfesor   = findViewById(R.id.spProfesor)
        spDia        = findViewById(R.id.spDia)
        spHoraInicio = findViewById(R.id.spHoraInicio)
        spHoraFin    = findViewById(R.id.spHoraFin)
        etPrecio     = findViewById(R.id.etValor)
        btnGuardar   = findViewById(R.id.btnIngresar)

        // --- extras que vienen del intent de la actividad ---
        val dhId         = intent.getIntExtra("dh_id", -1)
        val idActividad  = intent.getIntExtra("id_actividad", -1)
        val nombreAct    = intent.getStringExtra("nombre_act") ?: ""
        val profesor     = intent.getStringExtra("profesor") ?: ""
        val diaActual   = intent.getIntExtra("dia", 1)
        val horaInicioActual = intent.getIntExtra("hora_inicio", 8 * 60)
        val horaFinActual   = intent.getIntExtra("hora_fin", 9 * 60)
        val precioActual = intent.getDoubleExtra("precio", 0.0)

        // Recupera el nombre de usuario del intent y lo muestra
        val usuario = intent.getStringExtra("usuario") ?: "Usuario"
        val tvBienvenida = findViewById<TextView>(R.id.tvBienvenida)
        tvBienvenida.text = "Bienvenido, $usuario"

        // Fecha encabezado
        val tvFecha = findViewById<TextView>(R.id.tvFecha)
        tvFecha.text = HeaderDateFormatter.format()

        // Actividad/Profesor fijos
        spActividad.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, listOf(nombreAct ?: ""))
        spProfesor.adapter  = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, listOf(profesor ?: ""))
        spActividad.isEnabled = false; spProfesor.isEnabled = false
        spActividad.isClickable = false; spProfesor.isClickable = false

        // Dia y horas compartidos con el alta de horarios.
        spDia.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, HorarioFormOptions.diasCortos)
        spDia.setSelection(diaActual.coerceIn(0, 6))

        val horas = HorarioFormOptions.minutosCada(30)
        val labels = HorarioFormOptions.etiquetas(horas)
        spHoraInicio.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, labels)
        spHoraFin.adapter    = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, labels)
        spHoraInicio.setSelection(HorarioFormOptions.posicionMasCercana(horas, horaInicioActual))
        spHoraFin.setSelection(HorarioFormOptions.posicionMasCercana(horas, horaFinActual))

        // Precio
        etPrecio.setText(if (precioActual.isNaN()) "" else String.format("%.2f", precioActual))
        etPrecio.isEnabled = false
        etPrecio.isClickable = false

        // Botón Guardar
        btnGuardar.setOnClickListener {
            val nuevoDia = spDia.selectedItemPosition
            val nuevaIni = horas[spHoraInicio.selectedItemPosition]
            val nuevaFin = horas[spHoraFin.selectedItemPosition]
            AlertDialog.Builder(this)
                .setTitle("Confirmar edici\u00f3n de actividad")
                .setMessage("¿Confirmás editar la actividad $nombreAct?")
                .setPositiveButton("Sí") { _, _ ->
                    try {
                        db.actualizarHorarioPorId(
                            idDiaHorario = dhId,
                            dia = nuevoDia,
                            horaInicio = nuevaIni,
                            horaFin = nuevaFin
                        )
                        Toast.makeText(this, "Actividad actualizada con \u00e9xito", Toast.LENGTH_SHORT).show()
                        intent = Intent(this, ActividadesActivity::class.java)
                        intent.putExtra("usuario", usuario)
                        startActivity(intent)
                    } catch (e: IllegalArgumentException) {
                        Toast.makeText(this, e.message ?: "Error al actualizar", Toast.LENGTH_LONG).show()
                    } catch (e: Exception) {
                        Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
                .setNegativeButton("Cancelar", null)
                .show()

        }

        // Bottom
        BottomNavHelper.setup(this, usuario, R.id.nav_activity)
    }

}
