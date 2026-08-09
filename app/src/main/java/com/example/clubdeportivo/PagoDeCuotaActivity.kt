package com.example.clubdeportivo

import android.content.Intent
import android.os.Bundle
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import java.time.LocalDate

class PagoDeCuotaActivity : AppCompatActivity() {
    private lateinit var db: DBHelper
    private lateinit var configuration: ClubConfiguration

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pago_cuota)

        db = DBHelper(this)
        configuration = db.obtenerConfiguracionClub()

        val personName = intent.getStringExtra("nombre") ?: "Sin nombre"
        val dni = intent.getStringExtra("dni") ?: ""
        val lastPayment = intent.getStringExtra("ultimoPago")
        val isMember = intent.getBooleanExtra("esSocio", false)
        val user = SessionExtras.nombreUsuario(intent.getStringExtra(SessionExtras.USUARIO))
        val operation = if (isMember) {
            getString(R.string.operation_monthly_fee)
        } else {
            getString(R.string.operation_join_member)
        }

        findViewById<TextView>(R.id.tvBienvenida).text = getString(R.string.welcome_user, user)
        findViewById<TextView>(R.id.tvFecha).text = HeaderDateFormatter.format()
        findViewById<TextView>(R.id.tvNombre).text = personName
        findViewById<TextView>(R.id.tvDni).text = dni
        findViewById<TextView>(R.id.tvTipoOperacion).text = operation
        findViewById<TextView>(R.id.tvPrecio).text = getString(
            R.string.value_amount,
            MoneyFormatter.format(configuration.monthlyFee, configuration.currency)
        )

        val paymentMethods = findViewById<RadioGroup>(R.id.rgMediosdePago)
        PaymentMethodUi.bind(paymentMethods, configuration)

        findViewById<MaterialButton>(R.id.btnPagar).apply {
            contentDescription = AccessibilityText.pay
            isEnabled = configuration.enabledPaymentMethods().isNotEmpty()
            setOnClickListener {
                registerPayment(
                    dni = dni,
                    personName = personName,
                    lastPayment = lastPayment,
                    isMember = isMember,
                    user = user,
                    paymentMethods = paymentMethods
                )
            }
        }
        BottomNavHelper.setup(this, user, R.id.nav_pagos)
    }

    override fun onDestroy() {
        db.close()
        super.onDestroy()
    }

    private fun registerPayment(
        dni: String,
        personName: String,
        lastPayment: String?,
        isMember: Boolean,
        user: String,
        paymentMethods: RadioGroup
    ) {
        val method = PaymentMethodUi.selected(paymentMethods)
        val validation = PaymentValidator.validateManualPayment(
            configuration.monthlyFee,
            method?.displayName
        )
        if (!validation.isValid) {
            toast(validation.error ?: "Selecciona un medio de pago")
            return
        }
        val paymentMethod = method?.displayName.orEmpty()
        val today = LocalDate.now()
        if (isMember && PaymentDbRules.paymentAlreadyRegistered(lastPayment, today)) {
            toast(getString(R.string.current_period_already_paid))
            return
        }

        val message = if (isMember) {
            PaymentDialogText.cuota(
                configuration.monthlyFee,
                paymentMethod,
                configuration.currency
            )
        } else {
            PaymentDialogText.convertirNoSocio(
                configuration.monthlyFee,
                paymentMethod,
                personName,
                configuration.currency
            )
        }

        AlertDialog.Builder(this)
            .setTitle(PaymentDialogText.confirmTitle)
            .setMessage(message)
            .setPositiveButton(PaymentDialogText.confirm) { _, _ ->
                persistPayment(dni, paymentMethod, isMember, user, today)
            }
            .setNegativeButton(PaymentDialogText.cancel, null)
            .show()
    }

    private fun persistPayment(
        dni: String,
        paymentMethod: String,
        isMember: Boolean,
        user: String,
        paymentDate: LocalDate
    ) {
        try {
            if (isMember) {
                db.registrarPagoCuota(dni, paymentMethod, paymentDate.toString())
                toast(PaymentDialogText.quotaSuccess)
            } else {
                val memberId = db.hacerSocioDesdeNoSocio(
                    dni.toInt(),
                    paymentMethod,
                    paymentDate.toString()
                )
                toast(PaymentDialogText.socioCreado(memberId))
            }
            startActivity(
                Intent(this, ListadosActivity::class.java)
                    .putExtra(SessionExtras.USUARIO, user)
            )
            finish()
        } catch (error: IllegalArgumentException) {
            toast(error.message ?: "No se pudo registrar el pago")
        } catch (error: Exception) {
            toast("Error: ${error.message}")
        }
    }

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
}
