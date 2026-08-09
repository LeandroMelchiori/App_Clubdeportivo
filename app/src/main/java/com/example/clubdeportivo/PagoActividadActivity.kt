package com.example.clubdeportivo

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.RadioGroup
import android.widget.SearchView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class PagoActividadActivity : AppCompatActivity() {
    private lateinit var db: DBHelper
    private lateinit var configuration: ClubConfiguration
    private lateinit var search: SearchView
    private lateinit var personName: TextView
    private lateinit var personId: TextView
    private lateinit var payButton: Button
    private lateinit var paymentMethods: RadioGroup

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_inscribir_actividad)

        db = DBHelper(this)
        configuration = db.obtenerConfiguracionClub()
        search = findViewById(R.id.etBuscar)
        personName = findViewById(R.id.tvNombreUsuario)
        personId = findViewById(R.id.tvIdUsuario)
        payButton = findViewById(R.id.btnPagar)
        paymentMethods = findViewById(R.id.rgMedioPago)

        val scheduleId = intent.getIntExtra("idActividad", -1)
        val activityName = intent.getStringExtra("nombreActividad") ?: "Actividad"
        val startTime = intent.getStringExtra("horaInicio") ?: "--:--"
        val activityDay = intent.getIntExtra("diaActividad", -1)
        val price = db.obtenerPrecioHorario(scheduleId) ?: 0.0
        val user = SessionExtras.nombreUsuario(intent.getStringExtra(SessionExtras.USUARIO))

        findViewById<TextView>(R.id.tvBienvenida).text = getString(R.string.welcome_user, user)
        findViewById<TextView>(R.id.tvFecha).text = HeaderDateFormatter.format()
        findViewById<TextView>(R.id.tvNombreActividad).text = "Actividad: $activityName"
        findViewById<TextView>(R.id.tvHorario).text =
            "${ClubFormatters.nombreDia(activityDay)} - $startTime hs"
        findViewById<TextView>(R.id.tvPrecio).text = getString(
            R.string.price_amount,
            MoneyFormatter.format(price, configuration.currency)
        )

        payButton.contentDescription = AccessibilityText.pay
        payButton.isEnabled = false
        PaymentMethodUi.bind(paymentMethods, configuration, enabled = false)
        setupPersonSearch()
        paymentMethods.setOnCheckedChangeListener { _, checkedId ->
            payButton.isEnabled = checkedId != -1 && search.query.isNotEmpty()
        }
        payButton.setOnClickListener {
            confirmPayment(scheduleId, activityName, price, user)
        }
        BottomNavHelper.setup(this, user, R.id.nav_home)
    }

    override fun onDestroy() {
        db.close()
        super.onDestroy()
    }

    private fun setupPersonSearch() {
        search.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                val person = db.obtenerPersonaPorDni(query)
                if (person == null) {
                    personName.text = "-----"
                    personId.text = "----"
                    PaymentMethodUi.setEnabled(paymentMethods, configuration, false)
                    payButton.isEnabled = false
                    toast("Ingresa un DNI valido")
                    return true
                }

                personName.text = "${person.apellido}, ${person.nombre}"
                personId.text = "DNI: ${person.dni}"
                PaymentMethodUi.setEnabled(paymentMethods, configuration, true)
                payButton.isEnabled = paymentMethods.checkedRadioButtonId != -1
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                payButton.isEnabled = false
                return false
            }
        })
    }

    private fun confirmPayment(
        scheduleId: Int,
        activityName: String,
        price: Double,
        user: String
    ) {
        val method = PaymentMethodUi.selected(paymentMethods)
        val validation = PaymentValidator.validateManualPayment(price, method?.displayName)
        if (!validation.isValid) {
            toast(validation.error ?: "Selecciona un medio de pago")
            return
        }
        val paymentMethod = method?.displayName.orEmpty()

        AlertDialog.Builder(this)
            .setTitle(PaymentDialogText.confirmActivityTitle)
            .setMessage(
                PaymentDialogText.actividad(
                    price,
                    activityName,
                    paymentMethod,
                    configuration.currency
                )
            )
            .setPositiveButton(PaymentDialogText.confirm) { _, _ ->
                try {
                    if (payActivity(search.query.toString(), scheduleId, paymentMethod)) {
                        startActivity(
                            Intent(this, InicioActivity::class.java)
                                .putExtra(SessionExtras.USUARIO, user)
                        )
                        finish()
                    }
                } catch (error: IllegalArgumentException) {
                    toast(error.message ?: "No se pudo registrar el pago")
                } catch (error: Exception) {
                    toast("Error: ${error.message}")
                }
            }
            .setNegativeButton(PaymentDialogText.cancel, null)
            .show()
    }

    private fun payActivity(dni: String, scheduleId: Int, paymentMethod: String): Boolean {
        val client = db.obtenerPersonaPorDni(dni)
            ?: throw IllegalArgumentException("Debe ingresar un DNI valido")
        if (client.esSocio) {
            toast("Los socios no necesitan pagar esta actividad")
            return false
        }

        val insertedId = db.registrarPagoActividadNoSocio(
            idCliente = client.id.toString(),
            horarioId = scheduleId,
            medioPago = paymentMethod
        )
        if (insertedId <= 0L) {
            toast("No se pudo registrar el pago")
            return false
        }
        toast("Pago registrado")
        return true
    }

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
}
