package com.example.clubdeportivo

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.SearchView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class PagoActividadActivity : AppCompatActivity() {

    private lateinit var db: DBHelper
    private lateinit var etBuscar: SearchView            // <--- usa el id real de tu buscador (ej: etBuscar)
    private lateinit var tvNombreUsuario: TextView     // tvNombreUsuario
    private lateinit var tvNombreActividad: TextView   // tvNombreActividad
    private lateinit var tvHoraInicio: TextView           // tvHorario
    private lateinit var tvPrecio: TextView            // tvPrecio

    private lateinit var tvIdUsuario: TextView
    // tvIdUsuario
    private lateinit var btnPagar: Button              // btnPagar
    private lateinit var rgMedioPago: RadioGroup       // rgMedioPago



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_inscribir_actividad)
        db = DBHelper(this)

        // Inicializar views
        tvNombreActividad = findViewById<TextView>(R.id.tvNombreActividad)
        tvNombreUsuario = findViewById(R.id.tvNombreUsuario)
        tvIdUsuario = findViewById(R.id.tvIdUsuario)
        tvHoraInicio = findViewById(R.id.tvHorario)
        tvPrecio = findViewById(R.id.tvPrecio)
        btnPagar = findViewById(R.id.btnPagar)
        rgMedioPago = findViewById(R.id.rgMedioPago)

        // Deshabilitados inicialmente
        btnPagar.isEnabled = false
        rgMedioPago.isEnabled = false

        // Recupera datos de la actividad del intent
        val idActividad = intent.getIntExtra("idActividad", -1)
        val nombreActividad = intent.getStringExtra("nombreActividad") ?: "nombre de la actividad"
        val horaInicio = intent.getStringExtra("horaInicio") ?: "Hora de inicio"
        val diaActividad = intent.getIntExtra("diaActividad", -1)
        val precio = intent.getDoubleExtra("precioActividad", 0.0)

        // Recupera el nombre de usuario del intent y lo muestra
        val usuario = intent.getStringExtra("usuario") ?: "Usuario"
        val tvBienvenida = findViewById<TextView>(R.id.tvBienvenida)
        tvBienvenida.text = "Bienvenido, $usuario"

        // Fecha encabezado
        val tvFecha = findViewById<TextView>(R.id.tvFecha)
        tvFecha.text = HeaderDateFormatter.format()

        // Valor int del dia convertido a texto para mostrar en pantalla.
        val diaTxt = ClubFormatters.nombreDia(diaActividad)
        // Asignar datos a views
        tvNombreActividad.text = "Actividad: $nombreActividad"
        tvHoraInicio.text = "$diaTxt - $horaInicio hs"
        tvPrecio.text = "Precio: $precio"

        // Buscador
        etBuscar = findViewById(R.id.etBuscar)
        etBuscar.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                val persona = db.obtenerPersonaPorDni(query)
                if (persona != null) {
                    tvNombreUsuario.text = "${persona.apellido}, ${persona.nombre}"
                    tvIdUsuario.text = "DNI: ${persona.dni}"
                    rgMedioPago.isEnabled = true
                } else {
                    toast("Ingres\u00e1 un DNI v\u00e1lido")
                }
                return true
            }

            override fun onQueryTextChange(newText: String?) = false
        })

        // RadioGroup
        rgMedioPago.setOnCheckedChangeListener { _, checkedId ->
            btnPagar.isEnabled = checkedId != -1
        }

        // Boton Pagar
        btnPagar.setOnClickListener {
            if (etBuscar.query.isEmpty()) {
                toast("Debe ingresar un DNI v\u00e1lido")
            } else{
                AlertDialog.Builder(this)
                    .setTitle(PaymentDialogText.confirmActivityTitle)
                    .setMessage(PaymentDialogText.actividad(precio, nombreActividad))
                    .setPositiveButton(PaymentDialogText.confirm) { _, _ ->
                        try {
                            pagarActividad(etBuscar.query.toString(), idActividad, precio)
                            intent = Intent(this, InicioActivity::class.java)
                            intent.putExtra("usuario", usuario)
                            startActivity(intent)
                        } catch (e: IllegalArgumentException) {
                            Toast.makeText(this, e.message ?: "No se pudo realizar la inscripci\u00f3n", Toast.LENGTH_LONG).show()
                        } catch (e: Exception) {
                            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                    .setNegativeButton(PaymentDialogText.cancel, null)
                    .show()
            }
        }

        // Bottom
        BottomNavHelper.setup(this, usuario, R.id.nav_home)
    }

    private fun toast(msg: String) =
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()

    fun pagarActividad(dni: String, idActividad: Int, precio: Double) {
        // 1) Buscar persona
        val cliente = db.obtenerPersonaPorDni(dni)

        if (cliente == null) {
            throw IllegalArgumentException("Debe ingresar un DNI v\u00e1lido")
        }

        if (cliente.esSocio) {
            toast("Los socios no necesitan pagar esta actividad")
            return
        }

        // 2) Validar medio de pago
        val selectedId = rgMedioPago.checkedRadioButtonId
        val formaPago = if (selectedId != -1) findViewById<RadioButton>(selectedId).text.toString() else null
        val validacion = PaymentValidator.validateManualPayment(precio, formaPago)
        if (!validacion.isValid) {
            Toast.makeText(this, validacion.error, Toast.LENGTH_SHORT).show()
            return
        }
        val medioPago = formaPago.orEmpty()

        // 3) Registrar pago
        val insertedId = db.registrarPagoActividadNoSocio(
            idCliente = cliente.id.toString(),
            horarioId = idActividad,
            monto = precio,
            medioPago = medioPago
        )
        if (insertedId > 0L) {
            toast("Pago registrado")
            finish()
        } else {
            toast("No se pudo registrar el pago")
        }
    }

}
