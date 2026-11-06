package com.example.clubdeportivo

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged

class BeneficiosClienteActivity : AppCompatActivity() {
    private lateinit var dbHelper: DBHelper
    private lateinit var discountSelector: Spinner
    private lateinit var hasScholarship: CheckBox
    private lateinit var scholarshipFields: LinearLayout
    private lateinit var percent: EditText
    private lateinit var scope: Spinner
    private lateinit var reason: EditText
    private lateinit var until: EditText
    private var discountOptions: List<Discount?> = emptyList()
    private var clientId = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_beneficios_cliente)
        SystemBarInsets.applyTo(this)

        dbHelper = DBHelper(this)
        val client = dbHelper.obtenerPersonaPorDni(intent.getStringExtra(EXTRA_DNI).orEmpty())
        val id = client?.id
        if (client == null || id == null) {
            Toast.makeText(this, PaymentDbRules.activeClientMissing, Toast.LENGTH_LONG).show()
            finish()
            return
        }
        clientId = id
        bindViews()
        findViewById<TextView>(R.id.tvClienteBeneficios).text = getString(
            R.string.client_benefits_subtitle,
            PersonaDisplayFormatter.nombreCompleto(client.nombre, client.apellido),
            client.dni
        )

        val benefits = dbHelper.obtenerBeneficiosCliente(clientId)
        setupDiscounts(benefits.discount)
        renderScholarship(benefits.scholarship)
        findViewById<Button>(R.id.btnCancelar).setOnClickListener { finish() }
        findViewById<Button>(R.id.btnGuardar).setOnClickListener { save() }
    }

    override fun onDestroy() {
        dbHelper.close()
        super.onDestroy()
    }

    private fun bindViews() {
        discountSelector = findViewById(R.id.spDescuentoCliente)
        hasScholarship = findViewById(R.id.cbTieneBeca)
        scholarshipFields = findViewById(R.id.llBeca)
        percent = findViewById(R.id.etPorcentajeBeca)
        scope = findViewById(R.id.spAlcanceBeca)
        reason = findViewById(R.id.etMotivoBeca)
        until = findViewById(R.id.etBecaHasta)
        scope.adapter = spinnerAdapter(ChargeScope.entries.map { it.label })
        hasScholarship.setOnCheckedChangeListener { _, checked ->
            scholarshipFields.visibility = if (checked) View.VISIBLE else View.GONE
        }
        listOf(percent, reason, until).forEach { input ->
            input.doAfterTextChanged { input.error = null }
        }
    }

    private fun setupDiscounts(assigned: Discount?) {
        val currency = dbHelper.obtenerConfiguracionClub().currency
        val available = dbHelper.obtenerDescuentos(soloActivos = true)
        val options = if (assigned != null && available.none { it.id == assigned.id }) {
            available + assigned
        } else {
            available
        }
        discountOptions = listOf(null) + options
        val labels = discountOptions.map { discount ->
            if (discount == null) {
                getString(R.string.client_discount_none)
            } else {
                "${discount.name} (${BenefitText.discountDetail(discount, currency)})"
            }
        }
        discountSelector.adapter = spinnerAdapter(labels)
        discountSelector.setSelection(discountOptions.indexOfFirst { it?.id == assigned?.id }.coerceAtLeast(0))
        findViewById<TextView>(R.id.tvSinDescuentos).visibility =
            if (options.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun renderScholarship(scholarship: Scholarship?) {
        hasScholarship.isChecked = scholarship != null
        scholarshipFields.visibility = if (scholarship != null) View.VISIBLE else View.GONE
        scope.setSelection(ChargeScope.entries.indexOf(scholarship?.scope ?: ChargeScope.BOTH))
        if (scholarship != null) {
            percent.setText(scholarship.percent.toString())
            reason.setText(scholarship.reason)
            until.setText(scholarship.until?.toString().orEmpty())
        }
    }

    private fun save() {
        val discount = discountOptions.getOrNull(discountSelector.selectedItemPosition)
        val scholarship = if (hasScholarship.isChecked) {
            val result = ScholarshipValidator.validate(
                percentInput = percent.text.toString(),
                scope = ChargeScope.entries.getOrElse(scope.selectedItemPosition) { ChargeScope.BOTH },
                reasonInput = reason.text.toString(),
                untilInput = until.text.toString()
            )
            if (result.scholarship == null) {
                val input = when (result.field) {
                    ScholarshipValidator.Field.PERCENT -> percent
                    ScholarshipValidator.Field.REASON -> reason
                    else -> until
                }
                input.error = result.error
                input.requestFocus()
                return
            }
            result.scholarship
        } else {
            null
        }

        val saved = dbHelper.guardarBeneficiosCliente(clientId, discount?.id, scholarship)
        Toast.makeText(
            this,
            if (saved) R.string.client_benefits_saved else R.string.client_benefits_save_error,
            Toast.LENGTH_SHORT
        ).show()
        if (saved) finish()
    }

    private fun spinnerAdapter(labels: List<String>) =
        ArrayAdapter(this, android.R.layout.simple_spinner_item, labels).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

    companion object {
        const val EXTRA_DNI = "dni"
    }
}
