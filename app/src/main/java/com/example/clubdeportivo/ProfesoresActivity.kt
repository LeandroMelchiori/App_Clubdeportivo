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

class ProfesoresActivity : AppCompatActivity() {

    private lateinit var dbHelper: DBHelper
    private lateinit var adapter: ProfesorAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profesores)

        dbHelper = DBHelper(this)

        findViewById<Button>(R.id.btnVolver).setOnClickListener {
            finish()
        }

        val rvProfesores = findViewById<RecyclerView>(R.id.rvProfesores)
        rvProfesores.layoutManager = LinearLayoutManager(this)

        adapter = ProfesorAdapter(
            list = dbHelper.obtenerProfesores(),
            onEdit = { profesor ->
                val intent = Intent(this, FormularioProfesorActivity::class.java)
                intent.putExtra(FormularioProfesorActivity.EXTRA_DNI, profesor.dni)
                startActivity(intent)
            },
            onDelete = { profesor ->
                confirmarBaja(profesor)
            }
        )
        rvProfesores.adapter = adapter

        findViewById<FloatingActionButton>(R.id.fabNuevoProfesor).setOnClickListener {
            startActivity(Intent(this, FormularioProfesorActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        adapter.updateList(dbHelper.obtenerProfesores())
    }

    override fun onDestroy() {
        dbHelper.close()
        super.onDestroy()
    }

    private fun confirmarBaja(profesor: DBHelper.Profesor) {
        AlertDialog.Builder(this)
            .setTitle("Dar de baja profesor")
            .setMessage("¿Estás seguro de que deseas dar de baja a ${profesor.nombre} ${profesor.apellido}?")
            .setPositiveButton("Sí") { _, _ ->
                if (dbHelper.darDeBajaProfesor(profesor.dni)) {
                    Toast.makeText(this, "Profesor dado de baja", Toast.LENGTH_SHORT).show()
                    adapter.updateList(dbHelper.obtenerProfesores())
                } else {
                    Toast.makeText(this, "Error al dar de baja", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("No", null)
            .show()
    }
}
