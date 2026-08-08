package com.example.clubdeportivo

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton

class ConfiguracionActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_configuracion)

        // Recupera el nombre de usuario del intent y lo muestra
        val usuario = intent.getStringExtra("usuario") ?: "Usuario"
        val tvBienvenida = findViewById<TextView>(R.id.tvBienvenida)
        tvBienvenida.text = "Bienvenido, $usuario"

        // Fecha encabezado
        val tvFecha = findViewById<TextView>(R.id.tvFecha)
        tvFecha.text = HeaderDateFormatter.format()

        // Boton editar admin
        val btnEditar = findViewById<MaterialButton>(R.id.btnEditar)
        btnEditar.isEnabled = false
        btnEditar.setOnClickListener {
            startActivity(Intent(this, EditarAdminActivity::class.java))
        }

        // Boton nuevo admin
        val btnNuevo = findViewById<MaterialButton>(R.id.btnNuevo)
        btnNuevo.isEnabled = false
        btnNuevo.setOnClickListener {
           // startActivity(Intent(this, NuevoAdminActivity::class.java))
        }

        // Boton cerrar sesion
        val btnSalir = findViewById<MaterialButton>(R.id.btnCerrarSesion)
        btnSalir.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Cerrar sesion")
                .setMessage("¿Estas seguro que quieres cerrar sesion?")
                .setPositiveButton("Si") { _, _ ->
                    startActivity(
                        Intent(
                            this,
                            LoginActivity::class.java
                        )
                    )
                }
                .setNegativeButton("No", null)
                .show()
        }

        // Bottom
        BottomNavHelper.setup(this, usuario, R.id.nav_settings)
    }
}