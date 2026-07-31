package com.example.clubdeportivo

import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton

class LoginActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Variables
        val etUsuario = findViewById<EditText>(R.id.etUsuario)
        val etContrasena = findViewById<EditText>(R.id.etContrasena)
        val btnLogin = findViewById<MaterialButton>(R.id.btnLogin)

        // Logica inicio de sesion
        btnLogin.setOnClickListener {
            val usuario = etUsuario.text.toString()
            val contrasena = etContrasena.text.toString()
            // Validacion campos en blanco
            if (usuario.isEmpty() || contrasena.isEmpty()) {
                Toast.makeText(this, "Por favor ingrese usuario y contrasena", Toast.LENGTH_SHORT)
                    .show() }
            // Validacion usuario correcto
            else if (LoginCredentials.sonValidas(usuario, contrasena)) {
                val intent = Intent(this, InicioActivity::class.java)
                intent.putExtra("usuario", usuario)
                startActivity(intent)
                Toast.makeText(this, "Sesion iniciada...", Toast.LENGTH_LONG).show()
            }
            // Usuario o contrasena incorrectos
            else {
                Toast.makeText(this, "Usuario o contrasena incorrectos", Toast.LENGTH_SHORT).show()
            }
        }
    }
}