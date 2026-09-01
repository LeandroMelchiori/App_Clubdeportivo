package com.example.clubdeportivo

import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.io.File
import java.util.Calendar

class ResumenMensualActivity : AppCompatActivity() {

    private lateinit var db: DBHelper
    private var mesActual: Int = 0
    private var anioActual: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_resumen_mensual)

        db = DBHelper(this)
        val configuration = db.obtenerConfiguracionClub()

        // --------- Usuario ----------
        val usuario = intent.getStringExtra("usuario") ?: "Usuario"
        val tvBienvenida = findViewById<TextView>(R.id.tvBienvenida)
        tvBienvenida.text = "Bienvenido, $usuario"


        // --------- Fecha de hoy ----------
        val calendar = Calendar.getInstance()
        mesActual = calendar.get(Calendar.MONTH) + 1      // 1..12
        anioActual = calendar.get(Calendar.YEAR)

        val tvFecha = findViewById<TextView>(R.id.tvFecha)
        tvFecha.text = HeaderDateFormatter.format(calendar.time)

        // --------- Referencias a los TextView del resumen ----------
        val tvMes = findViewById<TextView>(R.id.tvMes)
        val tvNoSocios = findViewById<TextView>(R.id.tvNoSocios)
        val tvSocios = findViewById<TextView>(R.id.tvSocios)
        val tvTotalClientes = findViewById<TextView>(R.id.tvTotalClientes)
        val tvMontoCuotas = findViewById<TextView>(R.id.tvMontoCuotas)
        val tvMontoActividades = findViewById<TextView>(R.id.tvMontoActividades)
        val tvIngresosTotales = findViewById<TextView>(R.id.tvIngresosTotales)
        val btnMesAnterior = findViewById<ImageButton>(R.id.btnMesAnterior)
        val btnMesSiguiente = findViewById<ImageButton>(R.id.btnMesSiguiente)
        val btnDescargar = findViewById<TextView>(R.id.btnDescargar)

        fun nombreMes(mes: Int): String {
            val meses = arrayOf(
                "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
                "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"
            )
            return meses[mes - 1]
        }

        // Cargar el mes actual al entrar
        fun cargarMes() {
            val resumen = db.obtenerResumenPagosMes(anioActual, mesActual)

            tvMes.text = " ${nombreMes(mesActual)} $anioActual"
            tvNoSocios.text = "No Socios : ${resumen.cantNoSocios}"
            tvSocios.text = "Socios: ${resumen.cantSocios}"
            tvTotalClientes.text = "Total clientes: ${resumen.totalClientes}"
            tvMontoCuotas.text = getString(
                R.string.summary_quota_amount,
                MoneyFormatter.format(resumen.montoCuotas, configuration.currency)
            )
            tvMontoActividades.text = getString(
                R.string.summary_activity_amount,
                MoneyFormatter.format(resumen.montoActividades, configuration.currency)
            )
            tvIngresosTotales.text = getString(
                R.string.summary_total_income,
                MoneyFormatter.format(resumen.ingresosTotales, configuration.currency)
            )
        }
        cargarMes()

        val calHoy = Calendar.getInstance()
        val mesHoy = calHoy.get(Calendar.MONTH) + 1
        val anioHoy = calHoy.get(Calendar.YEAR)

        btnMesAnterior.setOnClickListener {
            mesActual--
            if (mesActual < 1) {
                mesActual = 12
                anioActual--
            }
            cargarMes()
        }

        btnDescargar.setOnClickListener {
            val resumen = db.obtenerResumenPagosMes(anioActual, mesActual)
            val csv = CsvExporter.resumenMensual(
                nombreMes(mesActual),
                resumen,
                configuration.currency
            )
            val dir = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: filesDir
            val file = File(dir, "resumen_${anioActual}_${String.format("%02d", mesActual)}.csv")
            file.writeText(csv)
            Toast.makeText(this, "CSV guardado: ${file.name}", Toast.LENGTH_LONG).show()
        }

        btnMesSiguiente.setOnClickListener {
            // Solo dejamos avanzar hasta el mes actual del año actual
            val esMismoAnio = (anioActual == anioHoy)
            val puedeAvanzar =
                (anioActual < anioHoy) || (esMismoAnio && mesActual < mesHoy)

            if (puedeAvanzar) {
                mesActual++
                if (mesActual > 12) {
                    mesActual = 1
                    anioActual++
                }
                cargarMes()
            }
        }


        // --------- Bottom nav ----------
        BottomNavHelper.setup(this, usuario, R.id.nav_pagos)
    }
}
