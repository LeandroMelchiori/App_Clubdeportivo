package com.example.clubdeportivo

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import java.math.BigDecimal

class FormularioDescuentoActivity : AppCompatActivity() {
    private lateinit var dbHelper: DBHelper
    private lateinit var name: EditText
    private lateinit var type: Spinner
    private lateinit var value: EditText
    private lateinit var scope: Spinner
    private lateinit var active: CheckBox
    private var discountId = NEW_DISCOUNT_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_formulario_descuento)
        SystemBarInsets.applyTo(this)

        dbHelper = DBHelper(this)
        name = findViewById(R.id.etNombreDescuento)
        type = findViewById(R.id.spTipoDescuento)
        value = findViewById(R.id.etValorDescuento)
        scope = findViewById(R.id.spAlcanceDescuento)
        active = findViewById(R.id.cbDescuentoActivo)
        name.doAfterTextChanged { name.error = null }
        value.doAfterTextChanged { value.error = null }
        type.adapter = spinnerAdapter(AdjustmentType.entries.map { it.label })
        scope.adapter = spinnerAdapter(ChargeScope.entries.map { it.label })
        scope.setSelection(ChargeScope.entries.indexOf(ChargeScope.BOTH))

        discountId = intent.getLongExtra(EXTRA_DISCOUNT_ID, NEW_DISCOUNT_ID)
        if (discountId != NEW_DISCOUNT_ID) {
            findViewById<TextView>(R.id.tvTituloFormulario).setText(R.string.edit_discount_title)
            val discount = dbHelper.obtenerDescuento(discountId)
            if (discount == null) {
                Toast.makeText(this, R.string.discount_missing, Toast.LENGTH_SHORT).show()
                finish()
                return
            }
            render(discount)
        }

        findViewById<Button>(R.id.btnCancelar).setOnClickListener { finish() }
        findViewById<Button>(R.id.btnGuardar).setOnClickListener { save() }
    }

    override fun onDestroy() {
        dbHelper.close()
        super.onDestroy()
    }

    private fun render(discount: Discount) {
        name.setText(discount.name)
        type.setSelection(AdjustmentType.entries.indexOf(discount.type))
        value.setText(BigDecimal.valueOf(discount.value).stripTrailingZeros().toPlainString())
        scope.setSelection(ChargeScope.entries.indexOf(discount.scope))
        active.isChecked = discount.active
    }

    private fun save() {
        name.error = null
        value.error = null
        val result = DiscountValidator.validate(
            id = discountId,
            nameInput = name.text.toString(),
            type = AdjustmentType.entries.getOrElse(type.selectedItemPosition) { AdjustmentType.PERCENT },
            valueInput = value.text.toString(),
            scope = ChargeScope.entries.getOrElse(scope.selectedItemPosition) { ChargeScope.BOTH },
            active = active.isChecked
        )
        val discount = result.discount
        if (discount == null) {
            val input = if (result.field == DiscountValidator.Field.NAME) name else value
            input.error = result.error
            input.requestFocus()
            return
        }

        val saved = if (discountId == NEW_DISCOUNT_ID) {
            dbHelper.insertarDescuento(discount) != -1L
        } else {
            dbHelper.actualizarDescuento(discount) > 0
        }
        Toast.makeText(
            this,
            if (saved) R.string.discount_saved else R.string.discount_save_error,
            Toast.LENGTH_SHORT
        ).show()
        if (saved) finish()
    }

    private fun spinnerAdapter(labels: List<String>) =
        ArrayAdapter(this, android.R.layout.simple_spinner_item, labels).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

    companion object {
        const val EXTRA_DISCOUNT_ID = "ID_DESCUENTO"
        private const val NEW_DISCOUNT_ID = -1L
    }
}
