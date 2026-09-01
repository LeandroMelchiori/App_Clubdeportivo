package com.example.clubdeportivo

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.text.NumberFormat

class FormularioCatalogoActividadActivity : AppCompatActivity() {
    private lateinit var dbHelper: DBHelper
    private var activityId = NEW_ACTIVITY_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_formulario_catalogo)

        dbHelper = DBHelper(this)
        val nameInput = findViewById<EditText>(R.id.etNombreActividad)
        val priceInput = findViewById<EditText>(R.id.etPrecio)
        val configuration = dbHelper.obtenerConfiguracionClub()
        priceInput.hint = getString(R.string.price_with_currency, configuration.currency.code)

        activityId = intent.getLongExtra(EXTRA_ACTIVITY_ID, NEW_ACTIVITY_ID)
        if (activityId != NEW_ACTIVITY_ID) {
            findViewById<TextView>(R.id.tvTituloFormulario).setText(R.string.edit_activity_title)
            dbHelper.obtenerCatalogoActividad(activityId)?.let { activity ->
                nameInput.setText(activity.nombre)
                priceInput.setText(NumberFormat.getNumberInstance().format(activity.precio))
            } ?: run {
                toast("La actividad ya no existe")
                finish()
            }
        }

        findViewById<Button>(R.id.btnCancelar).setOnClickListener { finish() }
        findViewById<Button>(R.id.btnGuardar).setOnClickListener {
            clearErrors(nameInput, priceInput)
            val result = CatalogActivityValidator.validate(
                nameInput.text.toString(),
                priceInput.text.toString()
            )
            val value = result.value
            if (value == null) {
                val input = if (result.field == "name") nameInput else priceInput
                input.error = result.error
                input.requestFocus()
                return@setOnClickListener
            }

            val activity = DBHelper.CatalogoActividad(activityId, value.name, value.price)
            val saved = if (activityId == NEW_ACTIVITY_ID) {
                dbHelper.insertarCatalogoActividad(activity) != -1L
            } else {
                dbHelper.actualizarCatalogoActividad(activity) > 0
            }
            if (saved) {
                toast(if (activityId == NEW_ACTIVITY_ID) "Actividad guardada" else "Actividad actualizada")
                finish()
            } else {
                toast("No se pudo guardar la actividad")
            }
        }
    }

    override fun onDestroy() {
        dbHelper.close()
        super.onDestroy()
    }

    private fun clearErrors(vararg inputs: EditText) {
        inputs.forEach { it.error = null }
    }

    private fun toast(message: String) =
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

    companion object {
        const val EXTRA_ACTIVITY_ID = "ID_ACTIVIDAD"
        private const val NEW_ACTIVITY_ID = -1L
    }
}
