package com.example.clubdeportivo

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton

class CatalogoActividadesActivity : AppCompatActivity() {

    private lateinit var dbHelper: DBHelper
    private lateinit var adapter: CatalogoActividadAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_catalogo_actividades)

        dbHelper = DBHelper(this)

        findViewById<Button>(R.id.btnVolver).setOnClickListener {
            finish()
        }

        val rvCatalogo = findViewById<RecyclerView>(R.id.rvCatalogo)
        rvCatalogo.layoutManager = LinearLayoutManager(this)

        adapter = CatalogoActividadAdapter(
            currency = dbHelper.obtenerConfiguracionClub().currency,
            list = dbHelper.obtenerCatalogoActividades(),
            onEdit = { actividad ->
                val intent = Intent(this, FormularioCatalogoActividadActivity::class.java)
                intent.putExtra(FormularioCatalogoActividadActivity.EXTRA_ACTIVITY_ID, actividad.id)
                startActivity(intent)
            },
            onDelete = { actividad ->
                confirmarBorrado(actividad)
            }
        )
        rvCatalogo.adapter = adapter

        findViewById<FloatingActionButton>(R.id.fabNuevaActividad).setOnClickListener {
            startActivity(Intent(this, FormularioCatalogoActividadActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        adapter.updateList(dbHelper.obtenerCatalogoActividades())
    }

    override fun onDestroy() {
        dbHelper.close()
        super.onDestroy()
    }

    private fun confirmarBorrado(actividad: DBHelper.CatalogoActividad) {
        AlertDialog.Builder(this)
            .setTitle("Eliminar Actividad")
            .setMessage("Se eliminara '${actividad.nombre}' solo si no tiene horarios asociados.")
            .setPositiveButton("Sí") { _, _ ->
                if (dbHelper.eliminarCatalogoActividad(actividad.id)) {
                    Toast.makeText(this, "Actividad eliminada", Toast.LENGTH_SHORT).show()
                    adapter.updateList(dbHelper.obtenerCatalogoActividades())
                } else {
                    Toast.makeText(this, "Error al eliminar, asegúrate que no esté en uso", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("No", null)
            .show()
    }
}
