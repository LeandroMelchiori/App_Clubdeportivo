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
        btnLogin.contentDescription = AccessibilityText.login

        // Logica inicio de sesion
        btnLogin.setOnClickListener {
            val usuario = etUsuario.text.toString().trim()
            val contrasena = etContrasena.text.toString().trim()
            // Validacion campos en blanco
            if (usuario.isEmpty() || contrasena.isEmpty()) {
                Toast.makeText(this, LoginMessages.emptyFields, Toast.LENGTH_SHORT)
                    .show() }
            // Validacion usuario correcto
            else if (LoginCredentials.sonValidas(usuario, contrasena)) {
                val intent = Intent(this, InicioActivity::class.java)
                intent.putExtra("usuario", usuario)
                startActivity(intent)
                Toast.makeText(this, LoginMessages.success, Toast.LENGTH_LONG).show()
            }
            // Usuario o contrasena incorrectos
            else {
                Toast.makeText(this, LoginMessages.invalidCredentials, Toast.LENGTH_SHORT).show()
            }
        }
    }
}