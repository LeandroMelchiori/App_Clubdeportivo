package com.example.clubdeportivo

import android.content.ContentValues
import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.os.Bundle
import android.util.Log
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NuevoUsuarioActivity : AppCompatActivity() {
    private lateinit var db: SQLiteDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_nuevo_usuario)

        db = DBHelper(this).writableDatabase

        // Recupera el nombre de usuario del intent y lo muestra
        val usuario = intent.getStringExtra("usuario") ?: "Usuario"
        val tvBienvenida = findViewById<TextView>(R.id.tvBienvenida)
        tvBienvenida.text = "Bienvenido, $usuario"

        // Inputs
        val etNombre     = findViewById<EditText>(R.id.etNombre)
        val etApellido   = findViewById<EditText>(R.id.etApellido)
        val etFecha      = findViewById<EditText>(R.id.etFecha)
        val etDNI        = findViewById<EditText>(R.id.etDNI)
        val etDireccion  = findViewById<EditText>(R.id.etDireccion)
        val etTelefono   = findViewById<EditText>(R.id.etTelefono)
        val etEmail      = findViewById<EditText>(R.id.etEmail)

        // Boton registro
        findViewById<MaterialButton>(R.id.btnRegistrar)
            .setOnClickListener {
                val nombre    = etNombre.text.toString().trim()
                val apellido  = etApellido.text.toString().trim()
                val dni       = etDNI.text.toString().trim()
                val fecha  = etFecha.text.toString().trim()
                val direccion = etDireccion.text.toString().trim()
                val telefono  = etTelefono.text.toString().trim()
                val email     = etEmail.text.toString().trim()

                // Validaciones campos vacios
                listOf(etNombre, etApellido, etFecha, etDNI, etDireccion, etTelefono, etEmail).forEach { it.error = null }

                when (UsuarioFormValidation.firstMissing(nombre, apellido, fecha, dni, direccion, telefono, email)) {
                    UsuarioFormValidation.Field.NOMBRE -> etNombre
                    UsuarioFormValidation.Field.APELLIDO -> etApellido
                    UsuarioFormValidation.Field.FECHA_NACIMIENTO -> etFecha
                    UsuarioFormValidation.Field.DNI -> etDNI
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
                    etDNI.error = UsuarioFormText.invalidDni
                    etDNI.requestFocus()
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

                val fechaISO = UsuarioValidator.normalizarFechaNacimiento(fecha)
                if (fechaISO == null) {
                    etFecha.error = UsuarioFormText.invalidBirthDate
                    etFecha.requestFocus()
                    return@setOnClickListener
                }

                // Fecha hoy
                val hoyISO = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                // Tabla
                val tabla = "clientes"

                // Valores a insertar
                val values = ContentValues().apply {
                    put("dni", dni.trim())
                    put("nombre", nombre.trim())
                    put("apellido", apellido.trim())
                    put("fecha_nac", fechaISO)
                    put("direccion", direccion)
                    put("telefono", telefono.trim())
                    put("email", email.trim())
                    put("fecha_inscripcion", hoyISO)
                    put("activo", 1)
                    put("ficha_medica", 1)
                }

                // Chequeo DNI duplicado
                if (existeDni(db, tabla, dni)) {
                    Toast.makeText(this, "El DNI ya está registrado", Toast.LENGTH_LONG).show()
                    return@setOnClickListener
                }

                AlertDialog.Builder(this)
                    .setTitle(UsuarioFormText.confirmCreateTitle)
                    .setMessage(UsuarioFormText.confirmCreateMessage)
                    .setPositiveButton("Sí") { _, _ ->
                        try {
                            val rowId = db.insertOrThrow(tabla, null, values)  // usa insertOrThrow para ver el error real
                            startActivity(Intent(this, InicioActivity::class.java).putExtra(SessionExtras.USUARIO, usuario))
                            Toast.makeText(this, "Registro exitoso (ID $rowId)", Toast.LENGTH_LONG).show()

                            // Limpieza de campos
                            etNombre.text.clear()
                            etApellido.text.clear()
                            etFecha.text.clear()
                            etDNI.text.clear()
                            etDireccion.text.clear()
                            etTelefono.text.clear()
                            etEmail.text.clear()

                        } catch (e: android.database.sqlite.SQLiteConstraintException) {
                            Log.e("DB", "Constraint al insertar: ${e.message}")
                            Toast.makeText(this, "No se pudo registrar: ${e.message}", Toast.LENGTH_LONG).show()
                        } catch (e: Exception) {
                            Log.e("DB", "Error al insertar", e)
                            Toast.makeText(this, "Error al registrar: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                        }
                    }
                    .setNegativeButton("Cancelar", null)
                    .show()
            }

        // Bottom
        BottomNavHelper.setup(this, usuario, R.id.nav_home)
    }


    // Metodo para chequear si el DNI ya está registrado
    private fun existeDni(db: SQLiteDatabase, tabla: String, dni: String): Boolean {
        db.rawQuery("SELECT COUNT(1) FROM $tabla WHERE dni = ?", arrayOf(dni)).use { c ->
            return c.moveToFirst() && c.getInt(0) > 0
        }
    }

}
