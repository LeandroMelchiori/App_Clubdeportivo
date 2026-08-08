package com.example.clubdeportivo

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton

class VerMasActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ver_mas)

        // DB Helper
        val db = DBHelper(this)

        // Recupera el nombre de usuario del intent y lo muestra
        val usuario = intent.getStringExtra("usuario") ?: "Usuario"
        val tvBienvenida = findViewById<TextView>(R.id.tvBienvenida)
        tvBienvenida.text = "Bienvenido, $usuario"

        // Recupera dni del intent y busca a la persona en la BBDD
        val dniUsuario = intent.getStringExtra("dni") ?: ""
        if (dniUsuario.isEmpty()) {
            Intent(this, InicioActivity::class.java)
            Toast.makeText(this, "Error al cargar el cliente", Toast.LENGTH_LONG).show()
        }
        val cliente = db.obtenerPersonaPorDni(dniUsuario)
        if (cliente == null) {
            Toast.makeText(this, "Error al cargar el cliente", Toast.LENGTH_LONG).show()
            startActivity(Intent(this, ListadosActivity::class.java).putExtra(SessionExtras.USUARIO, usuario))
            finish()
            return
        }

        //Inicializar vistas
        val tvNombreCompleto = findViewById<TextView>(R.id.tvNombreUsuario)
        val tvDNI = findViewById<TextView>(R.id.tvDNI)
        val tvEmail = findViewById<TextView>(R.id.tvEmail)
        val tvDireccion = findViewById<TextView>(R.id.tvDireccion)
        val tvTelefono = findViewById<TextView>(R.id.tvTelefono)
        val tvFechaNacimiento = findViewById<TextView>(R.id.tvFechaNacimiento)
        val tvIdTipoSocio = findViewById<TextView>(R.id.tvIdTipoSocio)
        val tvEstadoCuenta = findViewById<TextView>(R.id.tvEstadoCuenta)
        val tvUltimoPago = findViewById<TextView>(R.id.tvUltimoPago)
        val tvProximoVencimiento = findViewById<TextView>(R.id.tvProximoVencimiento)
        val tvDeudaEstimada = findViewById<TextView>(R.id.tvDeudaEstimada)
        val tvHistorialCuenta = findViewById<TextView>(R.id.tvHistorialCuenta)

        // Reemplaza datos en las view
        tvNombreCompleto.text = PersonaDisplayFormatter.nombreCompleto(cliente.nombre, cliente.apellido)
        tvDNI.text = PersonaDisplayFormatter.etiqueta("DNI", cliente.dni)
        tvTelefono.text = PersonaDisplayFormatter.etiqueta("Tel\u00e9fono", cliente.telefono)
        tvDireccion.text = PersonaDisplayFormatter.etiqueta("Domicilio", cliente.direccion)
        tvFechaNacimiento.text = PersonaDisplayFormatter.etiqueta("Fecha de nacimiento", cliente.fecha_nac)
        tvEmail.text = PersonaDisplayFormatter.etiqueta("Email", cliente.email)
        tvIdTipoSocio.text = PersonaDisplayFormatter.tipoSocio(cliente.id, cliente.esSocio)

        val cuenta = db.obtenerCuentaCorriente(cliente.dni)
        tvEstadoCuenta.text = "Estado: ${cuenta?.estado ?: "Sin datos"} - ${cuenta?.detalleEstado ?: ""}"
        tvUltimoPago.text = "\u00daltimo pago: ${cuenta?.ultimoPagoCuota ?: cuenta?.ultimoPagoActividad ?: "Sin registros"}"
        tvProximoVencimiento.text = "Pr\u00f3ximo vencimiento: ${cuenta?.proximoVencimiento ?: "No aplica"}"
        tvDeudaEstimada.text = "Deuda estimada: $${cuenta?.deudaEstimada ?: 0.0}"
        tvHistorialCuenta.text = CuentaCorrienteFormatter.historial(cuenta?.movimientos.orEmpty())

        // Boton editar
        val btnEditar = findViewById<MaterialButton>(R.id.btnEditar)
        btnEditar.setOnClickListener {
            val intent = Intent(this, EditarUsuarioActivity::class.java)
            intent.putExtra("id", cliente.id)
            intent.putExtra("dni", cliente.dni)
            intent.putExtra("esSocio", cliente.esSocio)
            startActivity(intent)
        }

        // Boton Eliminar
        val btnEliminar: Button = findViewById(R.id.btnEliminar)
        btnEliminar.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Eliminar registro")
                .setMessage("¿Seguro que querés eliminar a esta persona? Esta acción no se puede deshacer.")
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Eliminar") { _, _ ->
                    val ok = db.eliminarPersonaPorId(cliente.id.toString()) // ← clave
                    if (ok) {
                        Toast.makeText(this, "Eliminado correctamente", Toast.LENGTH_SHORT).show()
                        val data = Intent().putExtra("dniEliminado", cliente.dni)
                        setResult(Activity.RESULT_OK, data)
                        intent = Intent(this, ListadosActivity::class.java)
                        intent.putExtra("usuario", usuario)
                        startActivity(intent)
                    } else {
                        Toast.makeText(this, "No se pudo eliminar", Toast.LENGTH_LONG).show()
                        finish()
                    }
                }
                .show()
        }

        // Bottom
        val bottom = findViewById<BottomNavigationView>(R.id.bottomNav)
        bottom.selectedItemId = R.id.nav_listas
        bottom.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_pagos -> {
                    val intent = Intent(this, ResumenMensualActivity::class.java)
                    intent.putExtra("usuario", usuario)
                    startActivity(intent)
                    true
                }

                R.id.nav_activity -> {
                    val intent = Intent(this, ActividadesActivity::class.java)
                    intent.putExtra("usuario", usuario)
                    startActivity(intent)
                    true
                }

                R.id.nav_settings -> {
                    val intent = Intent(this, ConfiguracionActivity::class.java)
                    intent.putExtra("usuario", usuario)
                    startActivity(intent)
                    true
                }

                R.id.nav_listas -> {
                    val intent = Intent(this, ListadosActivity::class.java)
                    intent.putExtra("usuario", usuario)
                    startActivity(intent)
                    true
                }

                R.id.nav_home -> {
                    val intent = Intent(this, InicioActivity::class.java)
                    intent.putExtra("usuario", usuario)
                    startActivity(intent)
                    true
                }

                else -> true
            }
        }
    }
}