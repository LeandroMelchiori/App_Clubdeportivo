package com.example.clubdeportivo

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import java.time.LocalDate

class VerMasActivity : AppCompatActivity() {
    private lateinit var db: DBHelper
    private lateinit var configuration: ClubConfiguration
    private var cliente: DBHelper.PersonaDTO? = null
    private var movimientosCuenta: List<DBHelper.MovimientoCuenta> = emptyList()
    private var filtroHistorial = CuentaCorrienteFormatter.Filtro.TODOS

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ver_mas)
        SystemBarInsets.applyTopTo(this)

        // DB Helper
        db = DBHelper(this)
        configuration = db.obtenerConfiguracionClub()

        // Recupera el nombre de usuario del intent y lo muestra
        val usuario = intent.getStringExtra("usuario") ?: "Usuario"
        val tvBienvenida = findViewById<TextView>(R.id.tvBienvenida)
        tvBienvenida.text = "Bienvenido, $usuario"
        findViewById<TextView>(R.id.tvFecha).text = HeaderDateFormatter.format()

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
        this.cliente = cliente

        //Inicializar vistas
        val tvNombreCompleto = findViewById<TextView>(R.id.tvNombreUsuario)
        val tvDNI = findViewById<TextView>(R.id.tvDNI)
        val tvEmail = findViewById<TextView>(R.id.tvEmail)
        val tvDireccion = findViewById<TextView>(R.id.tvDireccion)
        val tvTelefono = findViewById<TextView>(R.id.tvTelefono)
        val tvFechaNacimiento = findViewById<TextView>(R.id.tvFechaNacimiento)
        val tvIdTipoSocio = findViewById<TextView>(R.id.tvIdTipoSocio)

        // Reemplaza datos en las view
        tvNombreCompleto.text = PersonaDisplayFormatter.nombreCompleto(cliente.nombre, cliente.apellido)
        tvDNI.text = PersonaDisplayFormatter.etiqueta("DNI", cliente.dni)
        tvTelefono.text = PersonaDisplayFormatter.etiqueta("Teléfono", cliente.telefono)
        tvDireccion.text = PersonaDisplayFormatter.etiqueta("Domicilio", cliente.direccion)
        tvFechaNacimiento.text = PersonaDisplayFormatter.etiqueta("Fecha de nacimiento", cliente.fecha_nac)
        tvEmail.text = PersonaDisplayFormatter.etiqueta("Email", cliente.email)
        tvIdTipoSocio.text = PersonaDisplayFormatter.tipoSocio(cliente.id, cliente.esSocio)

        findViewById<Button>(R.id.btnHistorialTodos).setOnClickListener { mostrarHistorial(CuentaCorrienteFormatter.Filtro.TODOS) }
        findViewById<Button>(R.id.btnHistorialCuotas).setOnClickListener { mostrarHistorial(CuentaCorrienteFormatter.Filtro.CUOTAS) }
        findViewById<Button>(R.id.btnHistorialActividades).setOnClickListener { mostrarHistorial(CuentaCorrienteFormatter.Filtro.ACTIVIDADES) }

        // Boton beneficios
        findViewById<Button>(R.id.btnBeneficios).setOnClickListener {
            startActivity(
                Intent(this, BeneficiosClienteActivity::class.java)
                    .putExtra(BeneficiosClienteActivity.EXTRA_DNI, cliente.dni)
            )
        }

        // Boton editar
        val btnEditar = findViewById<Button>(R.id.btnEditar)
        btnEditar.contentDescription = AccessibilityText.editPerson
        btnEditar.setOnClickListener {
            val intent = Intent(this, EditarUsuarioActivity::class.java)
            intent.putExtra("id", cliente.id)
            intent.putExtra("dni", cliente.dni)
            intent.putExtra("esSocio", cliente.esSocio)
            intent.putExtra(SessionExtras.USUARIO, usuario)
            startActivity(intent)
        }

        // Boton eliminar
        val btnEliminar: Button = findViewById(R.id.btnEliminar)
        btnEliminar.contentDescription = AccessibilityText.deletePerson
        btnEliminar.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle(DeletePersonDialogText.title)
                .setMessage(DeletePersonDialogText.message(tvNombreCompleto.text.toString(), cliente.dni))
                .setNegativeButton(DeletePersonDialogText.cancel, null)
                .setPositiveButton(DeletePersonDialogText.confirm) { _, _ ->
                    val ok = db.eliminarPersonaPorId(cliente.id.toString())
                    if (ok) {
                        Toast.makeText(this, "Eliminado correctamente", Toast.LENGTH_SHORT).show()
                        val data = Intent().putExtra("dniEliminado", cliente.dni)
                        setResult(Activity.RESULT_OK, data)
                        intent = Intent(this, ListadosActivity::class.java)
                        intent.putExtra(SessionExtras.USUARIO, usuario)
                        startActivity(intent)
                    } else {
                        Toast.makeText(this, "No se pudo eliminar", Toast.LENGTH_LONG).show()
                        finish()
                    }
                }
                .show()
        }

        // Bottom
        BottomNavHelper.setup(this, usuario, R.id.nav_listas)
    }

    override fun onResume() {
        super.onResume()
        cliente?.let(::mostrarCuenta)
    }

    override fun onDestroy() {
        db.close()
        super.onDestroy()
    }

    private fun mostrarCuenta(cliente: DBHelper.PersonaDTO) {
        val cuenta = db.obtenerCuentaCorriente(cliente.dni)
        findViewById<TextView>(R.id.tvEstadoCuenta).text =
            "Estado: ${cuenta?.estado ?: "Sin datos"} - ${cuenta?.detalleEstado ?: ""}"
        findViewById<TextView>(R.id.tvUltimoPago).text =
            "Último pago: ${cuenta?.ultimoPagoCuota ?: cuenta?.ultimoPagoActividad ?: "Sin registros"}"
        findViewById<TextView>(R.id.tvProximoVencimiento).text =
            "Próximo vencimiento: ${cuenta?.proximoVencimiento ?: "No aplica"}"
        findViewById<TextView>(R.id.tvDeudaEstimada).text = getString(
            R.string.estimated_debt,
            MoneyFormatter.format(cuenta?.deudaEstimada ?: 0.0, configuration.currency)
        )
        findViewById<TextView>(R.id.tvBeneficios).text = BenefitText.clientSummary(
            cliente.id?.let(db::obtenerBeneficiosCliente) ?: ClientBenefits.NONE,
            configuration.currency,
            LocalDate.now()
        )
        movimientosCuenta = cuenta?.movimientos.orEmpty()
        mostrarHistorial(filtroHistorial)
    }

    private fun mostrarHistorial(filtro: CuentaCorrienteFormatter.Filtro) {
        filtroHistorial = filtro
        findViewById<TextView>(R.id.tvHistorialCuenta).text = CuentaCorrienteFormatter.historial(
            movimientosCuenta,
            configuration.currency,
            filtro
        )
    }
}
