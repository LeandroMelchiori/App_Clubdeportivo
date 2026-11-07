package com.example.clubdeportivo

import android.content.Intent
import android.os.Bundle
import android.view.View
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
    private lateinit var charge: ChargeBreakdown

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pago_cuota)
        SystemBarInsets.applyTopTo(this)

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
        charge = try {
            db.calcularCobroCuota(dni)
        } catch (_: IllegalArgumentException) {
            ChargeBreakdown(base = configuration.monthlyFee)
        }

        val paymentMethods = findViewById<RadioGroup>(R.id.rgMediosdePago)
        PaymentMethodUi.bind(paymentMethods, configuration)
        renderCharge(paymentMethods)

        findViewById<MaterialButton>(R.id.btnPagar).apply {
            contentDescription = AccessibilityText.pay
            if (charge.isFree) setText(R.string.register_no_charge)
            isEnabled = charge.isFree || configuration.enabledPaymentMethods().isNotEmpty()
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

    private fun renderCharge(paymentMethods: RadioGroup) {
        val price = findViewById<TextView>(R.id.tvPrecio)
        val breakdown = findViewById<TextView>(R.id.tvDesglose)
        if (charge.hasAdjustments) {
            price.visibility = View.GONE
            breakdown.visibility = View.VISIBLE
            breakdown.text = ChargeBreakdownFormatter.text(charge, ChargeKind.FEE, configuration.currency)
        } else {
            price.text = getString(
                R.string.value_amount,
                MoneyFormatter.format(charge.total, configuration.currency)
            )
        }
        if (charge.isFree) {
            findViewById<TextView>(R.id.tvMetodoPago).visibility = View.GONE
            paymentMethods.visibility = View.GONE
        }
    }

    private fun registerPayment(
        dni: String,
        personName: String,
        lastPayment: String?,
        isMember: Boolean,
        user: String,
        paymentMethods: RadioGroup
    ) {
        val method = if (charge.isFree) null else PaymentMethodUi.selected(paymentMethods)
        val validation = PaymentValidator.validateCharge(charge.total, method?.displayName)
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

        val message = when {
            charge.isFree && isMember -> PaymentDialogText.cuotaSinCargo()
            charge.isFree -> PaymentDialogText.convertirNoSocioSinCargo(personName)
            isMember -> PaymentDialogText.cuota(charge.total, paymentMethod, configuration.currency)
            else -> PaymentDialogText.convertirNoSocio(
                charge.total,
                paymentMethod,
                personName,
                configuration.currency
            )
        }

        AlertDialog.Builder(this)
            .setTitle(if (charge.isFree) PaymentDialogText.noChargeTitle else PaymentDialogText.confirmTitle)
            .setMessage(message)
            .setPositiveButton(PaymentDialogText.confirm) { _, _ ->
                persistPayment(dni, method?.displayName, isMember, user, today)
            }
            .setNegativeButton(PaymentDialogText.cancel, null)
            .show()
    }

    private fun persistPayment(
        dni: String,
        paymentMethod: String?,
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
