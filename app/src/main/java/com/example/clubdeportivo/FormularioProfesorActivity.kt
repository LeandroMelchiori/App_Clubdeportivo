package com.example.clubdeportivo

import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.time.LocalDate

class FormularioProfesorActivity : AppCompatActivity() {
    private lateinit var dbHelper: DBHelper
    private var originalDni: String? = null
    private var enrollmentDate: String = LocalDate.now().toString()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_formulario_profesor)

        dbHelper = DBHelper(this)
        val dniInput = findViewById<EditText>(R.id.etDni)
        val nameInput = findViewById<EditText>(R.id.etNombre)
        val lastNameInput = findViewById<EditText>(R.id.etApellido)
        val birthDateInput = findViewById<EditText>(R.id.etFechaNacimiento)
        val titleInput = findViewById<EditText>(R.id.etTitulo)
        val phoneInput = findViewById<EditText>(R.id.etTelefono)
        val emailInput = findViewById<EditText>(R.id.etEmail)
        val addressInput = findViewById<EditText>(R.id.etDireccion)
        val medicalCheck = findViewById<CheckBox>(R.id.cbFichaMedica)
        val activeCheck = findViewById<CheckBox>(R.id.cbActivo)

        originalDni = intent.getStringExtra(EXTRA_DNI)
        originalDni?.let { dni ->
            findViewById<TextView>(R.id.tvTituloFormulario).setText(R.string.edit_professor_title)
            dniInput.isEnabled = false
            dbHelper.obtenerProfesor(dni)?.let { professor ->
                enrollmentDate = professor.fechaInscripcion
                dniInput.setText(professor.dni)
                nameInput.setText(professor.nombre)
                lastNameInput.setText(professor.apellido)
                birthDateInput.setText(professor.fechaNac)
                titleInput.setText(professor.titulo.orEmpty())
                phoneInput.setText(professor.telefono)
                emailInput.setText(professor.email)
                addressInput.setText(professor.direccion)
                medicalCheck.isChecked = professor.fichaMedica
                activeCheck.isChecked = professor.activo
            } ?: run {
                toast("El profesor ya no existe")
                finish()
            }
        }

        findViewById<Button>(R.id.btnCancelar).setOnClickListener { finish() }
        findViewById<Button>(R.id.btnGuardar).setOnClickListener {
            val inputs = mapOf(
                "dni" to dniInput,
                "name" to nameInput,
                "lastName" to lastNameInput,
                "birthDate" to birthDateInput,
                "title" to titleInput,
                "phone" to phoneInput,
                "email" to emailInput,
                "address" to addressInput
            )
            inputs.values.forEach { it.error = null }
            val result = ProfessorValidator.validate(
                ProfessorValidator.Draft(
                    dni = dniInput.text.toString(),
                    name = nameInput.text.toString(),
                    lastName = lastNameInput.text.toString(),
                    birthDate = birthDateInput.text.toString(),
                    phone = phoneInput.text.toString(),
                    email = emailInput.text.toString(),
                    address = addressInput.text.toString(),
                    title = titleInput.text.toString()
                )
            )
            val value = result.value
            if (value == null) {
                inputs[result.field]?.apply {
                    error = result.error
                    requestFocus()
                }
                return@setOnClickListener
            }

            val professor = DBHelper.Profesor(
                dni = value.dni,
                nombre = value.name,
                apellido = value.lastName,
                fechaNac = value.birthDate,
                telefono = value.phone,
                direccion = value.address,
                fechaInscripcion = enrollmentDate,
                fichaMedica = medicalCheck.isChecked,
                email = value.email,
                activo = activeCheck.isChecked,
                titulo = value.title
            )
            val saved = if (originalDni == null) {
                dbHelper.insertarProfesor(professor) != -1L
            } else {
                dbHelper.actualizarProfesor(professor) > 0
            }
            if (saved) {
                toast(if (originalDni == null) "Profesor guardado" else "Profesor actualizado")
                finish()
            } else {
                toast("No se pudo guardar. Revisa que el DNI no exista")
            }
        }
    }

    override fun onDestroy() {
        dbHelper.close()
        super.onDestroy()
    }

    private fun toast(message: String) =
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

    companion object {
        const val EXTRA_DNI = "DNI"
    }
}
