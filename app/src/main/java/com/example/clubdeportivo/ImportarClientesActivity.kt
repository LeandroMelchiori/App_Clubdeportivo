package com.example.clubdeportivo

import android.database.SQLException
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.View
import android.widget.Button
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.card.MaterialCardView
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.time.LocalDate

class ImportarClientesActivity : AppCompatActivity() {
    private lateinit var dbHelper: DBHelper
    private lateinit var adapter: ImportacionClienteAdapter
    private lateinit var summaryCard: MaterialCardView
    private lateinit var fileNameText: TextView
    private lateinit var summaryText: TextView
    private lateinit var ignoredColumnsText: TextView
    private lateinit var statusText: TextView
    private lateinit var selectOtherFileButton: MaterialButton
    private lateinit var filterGroup: MaterialButtonToggleGroup
    private lateinit var allRowsButton: MaterialButton
    private lateinit var errorRowsButton: MaterialButton
    private lateinit var instructions: ScrollView
    private lateinit var noErrorsText: TextView
    private lateinit var rows: RecyclerView
    private lateinit var importButton: MaterialButton

    private var preview = ClientCsvImport.Preview()
    private var sourceUri: Uri? = null
    private var fileName: String? = null
    private var statusMessage: String? = null
    private var statusIsError = false
    private var showOnlyErrors = false

    private val selectFile = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) loadFile(uri)
    }

    private val saveTemplate = registerForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) writeTemplate(uri)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_importar_clientes)

        dbHelper = DBHelper(this)
        bindViews()

        adapter = ImportacionClienteAdapter()
        rows.layoutManager = LinearLayoutManager(this)
        rows.adapter = adapter

        findViewById<Button>(R.id.btnVolver).setOnClickListener { finish() }
        findViewById<MaterialButton>(R.id.btnSeleccionarArchivo).setOnClickListener { chooseFile() }
        selectOtherFileButton.setOnClickListener { chooseFile() }
        findViewById<MaterialButton>(R.id.btnDescargarPlantilla).setOnClickListener {
            saveTemplate.launch(TEMPLATE_FILE_NAME)
        }
        filterGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                showOnlyErrors = checkedId == R.id.btnFiltroErrores
                renderRows()
            }
        }
        importButton.setOnClickListener { confirmImport() }

        showOnlyErrors = savedInstanceState?.getBoolean(STATE_ONLY_ERRORS) ?: false
        if (showOnlyErrors) filterGroup.check(R.id.btnFiltroErrores)
        // intent.data permite abrir la pantalla con un archivo ya elegido (lo usa la prueba visual).
        val restoredUri = savedInstanceState?.getString(STATE_URI)?.let(Uri::parse) ?: intent.data
        if (restoredUri != null) loadFile(restoredUri) else render()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        sourceUri?.let { outState.putString(STATE_URI, it.toString()) }
        outState.putBoolean(STATE_ONLY_ERRORS, showOnlyErrors)
    }

    override fun onDestroy() {
        dbHelper.close()
        super.onDestroy()
    }

    private fun bindViews() {
        summaryCard = findViewById(R.id.cardResumen)
        fileNameText = findViewById(R.id.tvArchivo)
        summaryText = findViewById(R.id.tvResumen)
        ignoredColumnsText = findViewById(R.id.tvColumnasIgnoradas)
        statusText = findViewById(R.id.tvEstadoImportacion)
        selectOtherFileButton = findViewById(R.id.btnElegirOtroArchivo)
        filterGroup = findViewById(R.id.toggleFiltroFilas)
        allRowsButton = findViewById(R.id.btnFiltroTodas)
        errorRowsButton = findViewById(R.id.btnFiltroErrores)
        instructions = findViewById(R.id.svInstrucciones)
        noErrorsText = findViewById(R.id.tvSinErrores)
        rows = findViewById(R.id.rvFilasImportacion)
        importButton = findViewById(R.id.btnImportar)
    }

    private fun chooseFile() = selectFile.launch(CSV_MIME_TYPES)

    private fun loadFile(uri: Uri) {
        fileName = displayName(uri)
        sourceUri = null
        preview = ClientCsvImport.Preview()
        val bytes = try {
            readLimited(uri)
        } catch (_: IOException) {
            showStatus(getString(R.string.import_clients_read_error), isError = true)
            return
        } catch (_: SecurityException) {
            showStatus(getString(R.string.import_clients_read_error), isError = true)
            return
        }
        if (bytes == null) {
            showStatus(getString(R.string.import_clients_too_large), isError = true)
            return
        }

        val identities = dbHelper.obtenerIdentidadesClientes()
        preview = ClientCsvImport.preview(CsvParser.decode(bytes), identities.dnis, identities.emails)
        sourceUri = uri
        statusMessage = preview.fileError
        statusIsError = preview.fileError != null
        render()
    }

    private fun readLimited(uri: Uri): ByteArray? {
        val input = contentResolver.openInputStream(uri) ?: throw IOException("Sin acceso al archivo")
        input.use { stream ->
            val output = ByteArrayOutputStream()
            val buffer = ByteArray(8 * 1024)
            while (true) {
                val read = stream.read(buffer)
                if (read == -1) break
                output.write(buffer, 0, read)
                if (output.size() > ClientCsvImport.MAX_BYTES) return null
            }
            return output.toByteArray()
        }
    }

    private fun displayName(uri: Uri): String {
        val queried = try {
            contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null }
        } catch (_: Exception) {
            null
        }
        return queried ?: uri.lastPathSegment ?: getString(R.string.import_clients_unnamed_file)
    }

    private fun showStatus(message: String, isError: Boolean) {
        statusMessage = message
        statusIsError = isError
        render()
    }

    private fun render() {
        val hasRows = preview.rows.isNotEmpty()
        summaryCard.visibility = if (fileName != null) View.VISIBLE else View.GONE
        fileNameText.text = fileName

        summaryText.visibility = if (hasRows) View.VISIBLE else View.GONE
        summaryText.text = ClientCsvImportText.summary(preview.validClients.size, preview.invalidCount)
        ignoredColumnsText.visibility =
            if (hasRows && preview.ignoredColumns.isNotEmpty()) View.VISIBLE else View.GONE
        ignoredColumnsText.text = ClientCsvImportText.ignoredColumns(preview.ignoredColumns)

        statusText.visibility = if (statusMessage != null) View.VISIBLE else View.GONE
        statusText.text = statusMessage
        statusText.setTextColor(getColor(if (statusIsError) R.color.error_on_dark else R.color.green_user))

        selectOtherFileButton.visibility = if (hasRows) View.VISIBLE else View.GONE
        instructions.visibility = if (hasRows) View.GONE else View.VISIBLE
        filterGroup.visibility = if (hasRows) View.VISIBLE else View.GONE
        allRowsButton.text = getString(R.string.import_clients_filter_all, preview.rows.size)
        errorRowsButton.text = getString(R.string.import_clients_filter_errors, preview.invalidCount)

        val validCount = preview.validClients.size
        importButton.visibility = if (hasRows) View.VISIBLE else View.GONE
        importButton.isEnabled = validCount > 0
        importButton.text = ClientCsvImportText.importButton(validCount)
        renderRows()
    }

    private fun renderRows() {
        val visibleRows = if (showOnlyErrors) preview.rows.filterNot { it.isValid } else preview.rows
        adapter.submitList(visibleRows)
        val hasRows = preview.rows.isNotEmpty()
        rows.visibility = if (hasRows && visibleRows.isNotEmpty()) View.VISIBLE else View.GONE
        noErrorsText.visibility = if (hasRows && visibleRows.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun confirmImport() {
        val clients = preview.validClients
        if (clients.isEmpty()) return
        AlertDialog.Builder(this)
            .setTitle(R.string.import_clients_confirm_title)
            .setMessage(ClientCsvImportText.confirmMessage(clients.size, preview.invalidCount))
            .setPositiveButton(R.string.import_clients_confirm_action) { _, _ -> importClients(clients) }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }

    private fun importClients(clients: List<ClientCsvImport.Client>) {
        val imported = try {
            dbHelper.importarClientes(clients, LocalDate.now().toString())
        } catch (_: SQLException) {
            showStatus(getString(R.string.import_clients_failed), isError = true)
            return
        }
        sourceUri = null
        preview = ClientCsvImport.Preview()
        setResult(RESULT_OK)
        showStatus(ClientCsvImportText.imported(imported), isError = false)
    }

    private fun writeTemplate(uri: Uri) {
        val saved = try {
            contentResolver.openOutputStream(uri)?.use { output ->
                output.write(ClientCsvImport.template().toByteArray(Charsets.UTF_8))
            } != null
        } catch (_: IOException) {
            false
        } catch (_: SecurityException) {
            false
        }
        val message = if (saved) R.string.import_clients_template_saved else R.string.import_clients_template_error
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private companion object {
        const val STATE_URI = "import_source_uri"
        const val STATE_ONLY_ERRORS = "import_only_errors"
        const val TEMPLATE_FILE_NAME = "plantilla_clientes.csv"
        val CSV_MIME_TYPES = arrayOf(
            "text/csv",
            "text/comma-separated-values",
            "application/csv",
            "application/vnd.ms-excel",
            "text/plain"
        )
    }
}
