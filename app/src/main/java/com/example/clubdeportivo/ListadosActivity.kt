package com.example.clubdeportivo

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.widget.Button
import android.widget.SearchView
import android.widget.TextView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class ListadosActivity : AppCompatActivity() {
    private lateinit var db: DBHelper
    private lateinit var hoyISO: String
    private lateinit var rvNoSocios: RecyclerView
    private lateinit var rvSocios: RecyclerView
    private lateinit var rvVenc: RecyclerView
    private lateinit var tvNombreLista: TextView
    private lateinit var tvFecha: TextView
    private lateinit var tvResumenVencimientos: TextView
    private lateinit var tvEstadoLista: TextView
    private lateinit var panelFiltrosVencimientos: LinearLayout
    private lateinit var noSocioAdapter: NoSocioAdapter
    private lateinit var socioAdapter: SocioAdapter
    private lateinit var vencimientoAdapter: VencimientoAdapter
    private lateinit var verMasLauncher: androidx.activity.result.ActivityResultLauncher<Intent>
    private var noSociosActuales: List<DBHelper.NoSocioCard> = emptyList()
    private var sociosActuales: List<DBHelper.SocioCard> = emptyList()
    private var vencimientosActuales: List<DBHelper.VencimientoCard> = emptyList()
    private var filtroVencimiento = VencimientoFilters.Tipo.TODOS
    private var busquedaActual = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        // DB Helper
        verMasLauncher = registerForActivityResult(
            androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == RESULT_OK) {
                refreshVisibleList()
            }
        }
        db = DBHelper(this)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_listados)

        // Views
        rvNoSocios = findViewById(R.id.rvNoSocios)
        rvSocios   = findViewById(R.id.rvSocios)
        rvVenc     = findViewById(R.id.rvVencimientos)
        tvNombreLista = findViewById(R.id.tvNombreLista)
        tvFecha = findViewById(R.id.tvFecha)
        tvResumenVencimientos = findViewById(R.id.tvResumenVencimientos)
        tvEstadoLista = findViewById(R.id.tvEstadoLista)
        panelFiltrosVencimientos = findViewById(R.id.panelFiltrosVencimientos)


        // Fecha actual usada para vencimientos y refrescos.
        hoyISO = LocalDate.now().format(DateTimeFormatter.ISO_DATE)

        // Recupera el nombre de usuario del intent y lo muestra
        val usuario = SessionExtras.nombreUsuario(intent.getStringExtra(SessionExtras.USUARIO))
        val tvBienvenida = findViewById<TextView>(R.id.tvBienvenida)
        tvBienvenida.text = "Bienvenido, $usuario"

        // Fecha encabezado
        tvFecha.text = HeaderDateFormatter.format()


        rvNoSocios.layoutManager = LinearLayoutManager(this)
        rvSocios.layoutManager   = LinearLayoutManager(this)
        rvVenc.layoutManager     = LinearLayoutManager(this)

        //  crear instancias
        noSocioAdapter = NoSocioAdapter(usuario)
        socioAdapter   = SocioAdapter(usuario)
        vencimientoAdapter    = VencimientoAdapter(usuario)

        // Asignar adapters
        rvNoSocios.adapter = noSocioAdapter
        rvSocios.adapter   = socioAdapter
        rvVenc.adapter     = vencimientoAdapter
        rvNoSocios.setHasFixedSize(true)
        rvSocios.setHasFixedSize(true)
        rvVenc.setHasFixedSize(true)

        // Listados
        renderNoSocios(db.obtenerNoSocios())
        renderSocios(db.obtenerSocios())
        renderVencimientos(db.obtenerVencimientos(hoyISO))
        renderResumenVencimientos()

        // Botones listas
        val botonVencimiento: Button = findViewById(R.id.btnListVencimientos)
        val botonSocios: Button = findViewById(R.id.btnListSocios)
        val botonNoSocios: Button = findViewById(R.id.btnListNoSocios)
        val botonExportar: Button = findViewById(R.id.btnExportarListados)
        val botonFiltroTodos: Button = findViewById(R.id.btnFiltroVencTodos)
        val botonFiltroAlDia: Button = findViewById(R.id.btnFiltroVencAlDia)
        val botonFiltroPorVencer: Button = findViewById(R.id.btnFiltroVencPorVencer)
        val botonFiltroVencidos: Button = findViewById(R.id.btnFiltroVencVencidos)

        // Lista por defecto
        botonNoSocios.setTextColor(Color.WHITE);
        botonSocios.setTextColor(Color.BLACK);
        botonVencimiento.setTextColor(Color.BLACK)
        tvNombreLista.text = "Listado No Socios"

        // Buscador
        val svBuscar = findViewById<SearchView>(R.id.svBuscar)

        svBuscar.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                filtrarSegunListaActual(query.orEmpty())
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                filtrarSegunListaActual(newText.orEmpty())
                return true
            }
        })
        // onClick
        botonVencimiento.setOnClickListener {
            mostrar(rvVenc)
            panelFiltrosVencimientos.visibility = View.VISIBLE
            botonNoSocios.setTextColor(Color.BLACK);
            botonSocios.setTextColor(Color.BLACK);
            botonVencimiento.setTextColor(Color.WHITE)
            tvNombreLista.text = "Listado Vencimientos"
            aplicarFiltroVencimientos(filtroVencimiento)
            renderResumenVencimientos()
        }
        botonSocios.setOnClickListener {
            mostrar(rvSocios)
            panelFiltrosVencimientos.visibility = View.GONE
            botonNoSocios.setTextColor(Color.BLACK);
            botonSocios.setTextColor(Color.WHITE);
            botonVencimiento.setTextColor(Color.BLACK)
            tvNombreLista.text = "Listado Socios"
        }
        botonNoSocios.setOnClickListener {
            mostrar(rvNoSocios)
            panelFiltrosVencimientos.visibility = View.GONE
            botonNoSocios.setTextColor(Color.WHITE);
            botonSocios.setTextColor(Color.BLACK);
            botonVencimiento.setTextColor(Color.BLACK)
            tvNombreLista.text = "Listado No Socios"
        }

        botonFiltroTodos.setOnClickListener { aplicarFiltroVencimientos(VencimientoFilters.Tipo.TODOS) }
        botonFiltroAlDia.setOnClickListener { aplicarFiltroVencimientos(VencimientoFilters.Tipo.AL_DIA) }
        botonFiltroPorVencer.setOnClickListener { aplicarFiltroVencimientos(VencimientoFilters.Tipo.POR_VENCER) }
        botonFiltroVencidos.setOnClickListener { aplicarFiltroVencimientos(VencimientoFilters.Tipo.VENCIDO) }
        botonExportar.setOnClickListener { exportarListadoVisible() }

        // Bottom
        BottomNavHelper.setup(this, usuario, R.id.nav_listas)
    }
    fun mostrar(rv: RecyclerView) {
        rvNoSocios.visibility = View.GONE
        rvSocios.visibility   = View.GONE
        rvVenc.visibility     = View.GONE
        rv.visibility         = View.VISIBLE
    }
    private fun renderResumenVencimientos() {
        val resumen = db.obtenerResumenVencimientos(hoyISO)
        tvResumenVencimientos.text = "Al dia: ${resumen.alDia} | Por vencer: ${resumen.porVencer} | Vencidos: ${resumen.vencidos}"
    }
    private fun renderNoSocios(lista: List<DBHelper.NoSocioCard>) {
        noSociosActuales = lista
        noSocioAdapter.submitList(lista)
        actualizarEstadoLista(lista.size, "no socios")
    }
    private fun renderSocios(lista: List<DBHelper.SocioCard>) {
        sociosActuales = lista
        socioAdapter.submitList(lista)
        actualizarEstadoLista(lista.size, "socios")
    }
    private fun renderVencimientos(lista: List<DBHelper.VencimientoCard>) {
        vencimientosActuales = lista
        aplicarFiltroVencimientos(filtroVencimiento)
    }
    private fun aplicarFiltroVencimientos(tipo: VencimientoFilters.Tipo) {
        filtroVencimiento = tipo
        val filtrados = VencimientoFilters.filtrar(vencimientosActuales, tipo)
        vencimientoAdapter.submitList(filtrados)
        val etiqueta = when (tipo) {
            VencimientoFilters.Tipo.TODOS -> "vencimientos"
            VencimientoFilters.Tipo.AL_DIA -> "vencimientos al dia"
            VencimientoFilters.Tipo.POR_VENCER -> "vencimientos por vencer"
            VencimientoFilters.Tipo.VENCIDO -> "vencimientos vencidos"
        }
        actualizarEstadoLista(filtrados.size, etiqueta)
    }

    private fun actualizarEstadoLista(cantidad: Int, tipo: String) {
        tvEstadoLista.text = EmptyStateText.listado(cantidad, tipo, busquedaActual)
    }
    private fun exportarListadoVisible() {
        val (nombreArchivo, csv) = when {
            rvSocios.visibility == View.VISIBLE ->
                "socios_$hoyISO.csv" to CsvExporter.socios(
                    ListadoExportFilter.socios(sociosActuales, busquedaActual)
                )

            rvVenc.visibility == View.VISIBLE ->
                "vencimientos_$hoyISO.csv" to CsvExporter.vencimientos(
                    ListadoExportFilter.vencimientos(vencimientosActuales, busquedaActual, filtroVencimiento)
                )

            else ->
                "no_socios_$hoyISO.csv" to CsvExporter.noSocios(
                    ListadoExportFilter.noSocios(noSociosActuales, busquedaActual)
                )
        }
        val dir = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: filesDir
        File(dir, nombreArchivo).writeText(csv)
        Toast.makeText(this, "CSV guardado: $nombreArchivo", Toast.LENGTH_LONG).show()
    }

    private fun refreshVisibleList() {
        val rvSocios        = findViewById<RecyclerView>(R.id.rvSocios)
        val rvNoSocios      = findViewById<RecyclerView>(R.id.rvNoSocios)
        val rvVencimientos  = findViewById<RecyclerView>(R.id.rvVencimientos)
        when {
            rvSocios.visibility == View.VISIBLE ->
                renderSocios(db.obtenerSocios())

            rvNoSocios.visibility == View.VISIBLE ->
                renderNoSocios(db.obtenerNoSocios())

            rvVencimientos.visibility == View.VISIBLE -> {
                renderVencimientos(db.obtenerVencimientos(hoyISO))
                renderResumenVencimientos()
            }

            else ->
                renderNoSocios(db.obtenerNoSocios()) // fallback
        }
    }
    private fun filtrarSegunListaActual(texto: String) {
        busquedaActual = texto.trim()
        when {
            rvNoSocios.visibility == View.VISIBLE -> {
                noSocioAdapter.filtrarPorNombre(busquedaActual)
                actualizarEstadoLista(ListadoExportFilter.noSocios(noSociosActuales, busquedaActual).size, "no socios")
            }

            rvSocios.visibility == View.VISIBLE -> {
                socioAdapter.filtrarPorNombre(busquedaActual)
                actualizarEstadoLista(ListadoExportFilter.socios(sociosActuales, busquedaActual).size, "socios")
            }

            rvVenc.visibility == View.VISIBLE -> {
                vencimientoAdapter.filtrarPorNombre(busquedaActual)
                actualizarEstadoLista(
                    ListadoExportFilter.vencimientos(vencimientosActuales, busquedaActual, filtroVencimiento).size,
                    "vencimientos"
                )
            }
        }
    }
}
