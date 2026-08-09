package com.example.clubdeportivo

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class EditarUsuarioActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val db = DBHelper(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_editar_usuario)

        // Recupera el nombre de usuario del intent y lo muestra
        val usuario = intent.getStringExtra("usuario") ?: "Usuario"
        val tvBienvenida = findViewById<TextView>(R.id.tvBienvenida)
        tvBienvenida.text = "Bienvenido, $usuario"

        // Fecha encabezado
        val tvFecha = findViewById<TextView>(R.id.tvFechaHoy)
        tvFecha.text = HeaderDateFormatter.format()

        // Recuperar datos del intent
        val id = intent.getIntExtra("id", -1)
        val dni = intent.getStringExtra("dni") ?: ""
        val esSocio = intent.getBooleanExtra("esSocio", false)

        // Inicializar views
        val etDni = findViewById<EditText>(R.id.etDni)
        val etNombre = findViewById<EditText>(R.id.etNombre)
        val etApellido = findViewById<EditText>(R.id.etApellido)
        val etTelefono = findViewById<EditText>(R.id.etTelefono)
        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etDireccion = findViewById<EditText>(R.id.etDireccion)
        val etFechaNac = findViewById<EditText>(R.id.etFechaNac)

        // Llenar views
        val persona = db.obtenerPersonaPorDni(dni)
        etNombre.setText(persona?.nombre.orEmpty())
        etApellido.setText(persona?.apellido.orEmpty())
        etTelefono.setText(persona?.telefono.orEmpty())
        etEmail.setText(persona?.email.orEmpty())
        etDireccion.setText(persona?.direccion.orEmpty())
        etFechaNac.setText(persona?.fecha_nac.orEmpty())
        etDni.setText(persona?.dni.orEmpty())

        // Campo dni deshabilitado
        etDni.isEnabled = false

        // Boton editar
        val btnConfirmar = findViewById<Button>(R.id.btnConfirmar)
        btnConfirmar.setOnClickListener {
            val nombre = etNombre.text.toString().trim()
            val apellido = etApellido.text.toString().trim()
            val dni = etDni.text.toString().trim()
            val fechaNac = etFechaNac.text.toString().trim()
            val telefono = etTelefono.text.toString().trim()
            val direccion = etDireccion.text.toString().trim()
            val email = etEmail.text.toString().trim()

            listOf(etNombre, etApellido, etFechaNac, etDni, etDireccion, etTelefono, etEmail).forEach { it.error = null }

            // Validar campos obligatorios y formatos compartidos con el alta.
            when (UsuarioFormValidation.firstMissing(nombre, apellido, fechaNac, dni, direccion, telefono, email)) {
                UsuarioFormValidation.Field.NOMBRE -> etNombre
                UsuarioFormValidation.Field.APELLIDO -> etApellido
                UsuarioFormValidation.Field.FECHA_NACIMIENTO -> etFechaNac
                UsuarioFormValidation.Field.DNI -> etDni
                UsuarioFormValidation.Field.DIRECCION -> etDireccion
                UsuarioFormValidation.Field.TELEFONO -> etTelefono
                UsuarioFormValidation.Field.EMAIL -> etEmail
                null -> null
            }?.let { campo ->
                campo.error = UsuarioFormText.requiredFields
                campo.requestFocus()
                Toast.makeText(this, UsuarioFormText.requiredFields, Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            if (!UsuarioValidator.dniValido(dni)) {
                etDni.error = UsuarioFormText.invalidDni
                etDni.requestFocus()
                return@setOnClickListener
            }
            if (!UsuarioValidator.telefonoValido(telefono)) {
                etTelefono.error = UsuarioFormText.invalidPhone
                etTelefono.requestFocus()
                return@setOnClickListener
            }
            if (!UsuarioValidator.emailValido(email)) {
                etEmail.error = UsuarioFormText.invalidEmail
                etEmail.requestFocus()
                return@setOnClickListener
            }
            if (!UsuarioValidator.fechaNacimientoValida(fechaNac)) {
                etFechaNac.error = UsuarioFormText.invalidBirthDate
                etFechaNac.requestFocus()
                return@setOnClickListener
            }

            AlertDialog.Builder(this)
                .setTitle(UsuarioFormText.confirmEditTitle)
                .setMessage(UsuarioFormText.confirmEditMessage(dni))
                .setPositiveButton("Sí") { _, _ ->
                    try {
                        db.actualizarClientePorId(
                            id          = id,
                            nombre      = etNombre.text.toString().trim(),
                            apellido    = etApellido.text.toString().trim(),
                            dni         = etDni.text.toString().trim(),
                            fechaNac    = etFechaNac.text.toString().trim(),
                            telefono    = etTelefono.text.toString().trim(),
                            direccion   = etDireccion.text.toString().trim(),
                            email       = etEmail.text.toString().trim()
                        )
                        val intent = Intent(this, VerMasActivity::class.java)
                        intent.putExtra("dni", dni)
                        intent.putExtra(SessionExtras.USUARIO, usuario)
                        startActivity(intent)
                        Toast.makeText(this, "Cliente actualizado con \u00e9xito", Toast.LENGTH_SHORT).show()
                    } catch (e: IllegalArgumentException) {
                        Toast.makeText(this, e.message ?: "Error al actualizar usuario", Toast.LENGTH_LONG).show()
                    } catch (e: Exception) {
                        Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
                .setNegativeButton("Cancelar", null)
                .show()
        }
        // Bottom
        BottomNavHelper.setup(this, usuario, R.id.nav_home)
    }
}
