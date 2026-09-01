package com.example.clubdeportivo

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import java.util.Locale

class ConfiguracionActivity : AppCompatActivity() {
    private val currencies = ClubCurrency.entries
    private lateinit var dbHelper: DBHelper
    private lateinit var clubLogo: ImageView
    private lateinit var clubName: EditText
    private lateinit var clubAddress: EditText
    private lateinit var clubPhone: EditText
    private lateinit var clubEmail: EditText
    private lateinit var currency: Spinner
    private lateinit var monthlyFee: EditText
    private lateinit var dueDay: EditText
    private lateinit var graceDays: EditText
    private lateinit var acceptsCash: CheckBox
    private lateinit var acceptsTransfer: CheckBox
    private lateinit var acceptsCard: CheckBox
    private lateinit var paymentMethodError: TextView
    private lateinit var status: TextView
    private var logoUri: String? = null

    private val selectLogo = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            persistLogoPermission(uri)
            logoUri = uri.toString()
            renderLogo(uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_configuracion)

        dbHelper = DBHelper(this)
        bindViews()
        setupHeader()
        setupCurrencySelector()
        renderConfiguration(dbHelper.obtenerConfiguracionClub())

        findViewById<MaterialButton>(R.id.btnSeleccionarLogo).setOnClickListener {
            selectLogo.launch(arrayOf("image/png", "image/jpeg", "image/webp"))
        }
        findViewById<MaterialButton>(R.id.btnGuardarConfiguracion).setOnClickListener {
            saveConfiguration()
        }
        findViewById<MaterialButton>(R.id.btnGestionProfesores).setOnClickListener {
            startActivity(Intent(this, ProfesoresActivity::class.java))
        }
        findViewById<MaterialButton>(R.id.btnGestionActividades).setOnClickListener {
            startActivity(Intent(this, CatalogoActividadesActivity::class.java))
        }
        setupLogout()
        BottomNavHelper.setup(
            this,
            SessionExtras.nombreUsuario(intent.getStringExtra(SessionExtras.USUARIO)),
            R.id.nav_settings
        )
    }

    override fun onDestroy() {
        dbHelper.close()
        super.onDestroy()
    }

    private fun bindViews() {
        clubLogo = findViewById(R.id.ivClubLogo)
        clubName = findViewById(R.id.etClubNombre)
        clubAddress = findViewById(R.id.etClubDireccion)
        clubPhone = findViewById(R.id.etClubTelefono)
        clubEmail = findViewById(R.id.etClubEmail)
        currency = findViewById(R.id.spClubMoneda)
        monthlyFee = findViewById(R.id.etCuotaMensual)
        dueDay = findViewById(R.id.etDiaVencimiento)
        graceDays = findViewById(R.id.etDiasGracia)
        acceptsCash = findViewById(R.id.cbEfectivo)
        acceptsTransfer = findViewById(R.id.cbTransferencia)
        acceptsCard = findViewById(R.id.cbTarjeta)
        paymentMethodError = findViewById(R.id.tvErrorMediosPago)
        status = findViewById(R.id.tvEstadoConfiguracion)
    }

    private fun setupHeader() {
        val user = SessionExtras.nombreUsuario(intent.getStringExtra(SessionExtras.USUARIO))
        findViewById<TextView>(R.id.tvBienvenida).text = getString(R.string.welcome_user, user)
        findViewById<TextView>(R.id.tvFecha).text = HeaderDateFormatter.format()
    }

    private fun setupCurrencySelector() {
        currency.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            currencies.map { it.label }
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
    }

    private fun renderConfiguration(configuration: ClubConfiguration) {
        clubName.setText(configuration.name)
        clubAddress.setText(configuration.address)
        clubPhone.setText(configuration.phone)
        clubEmail.setText(configuration.email)
        currency.setSelection(currencies.indexOf(configuration.currency).coerceAtLeast(0))
        monthlyFee.setText(formatNumber(configuration.monthlyFee))
        dueDay.setText(String.format(Locale.ROOT, "%d", configuration.dueDay))
        graceDays.setText(String.format(Locale.ROOT, "%d", configuration.graceDays))
        acceptsCash.isChecked = configuration.acceptsCash
        acceptsTransfer.isChecked = configuration.acceptsTransfer
        acceptsCard.isChecked = configuration.acceptsCard
        logoUri = configuration.logoUri
        configuration.logoUri?.let { renderLogo(Uri.parse(it)) }
    }

    private fun saveConfiguration() {
        clearErrors()
        val selectedCurrency = currencies.getOrElse(currency.selectedItemPosition) { ClubCurrency.ARS }
        val result = ClubConfigurationValidator.validate(
            ClubConfigurationValidator.Draft(
                name = clubName.text.toString(),
                address = clubAddress.text.toString(),
                phone = clubPhone.text.toString(),
                email = clubEmail.text.toString(),
                currencyCode = selectedCurrency.code,
                monthlyFee = monthlyFee.text.toString(),
                dueDay = dueDay.text.toString(),
                graceDays = graceDays.text.toString(),
                acceptsCash = acceptsCash.isChecked,
                acceptsTransfer = acceptsTransfer.isChecked,
                acceptsCard = acceptsCard.isChecked,
                logoUri = logoUri
            )
        )

        val configuration = result.configuration
        if (configuration == null) {
            showValidationError(result)
            return
        }

        if (dbHelper.guardarConfiguracionClub(configuration)) {
            renderConfiguration(configuration)
            status.setTextColor(getColor(R.color.green_user))
            status.setText(R.string.club_config_saved)
            status.visibility = View.VISIBLE
        } else {
            status.setTextColor(getColor(R.color.red))
            status.setText(R.string.club_config_save_error)
            status.visibility = View.VISIBLE
        }
    }

    private fun clearErrors() {
        listOf(
            clubName,
            clubAddress,
            clubPhone,
            clubEmail,
            monthlyFee,
            dueDay,
            graceDays
        ).forEach { it.error = null }
        paymentMethodError.visibility = View.GONE
        status.visibility = View.GONE
    }

    private fun showValidationError(result: ClubConfigurationValidator.Result) {
        val input = when (result.field) {
            ClubConfigurationValidator.Field.NAME -> clubName
            ClubConfigurationValidator.Field.ADDRESS -> clubAddress
            ClubConfigurationValidator.Field.PHONE -> clubPhone
            ClubConfigurationValidator.Field.EMAIL -> clubEmail
            ClubConfigurationValidator.Field.MONTHLY_FEE -> monthlyFee
            ClubConfigurationValidator.Field.DUE_DAY -> dueDay
            ClubConfigurationValidator.Field.GRACE_DAYS -> graceDays
            else -> null
        }
        if (result.field == ClubConfigurationValidator.Field.PAYMENT_METHODS) {
            paymentMethodError.text = result.error
            paymentMethodError.visibility = View.VISIBLE
        } else if (input != null) {
            input.error = result.error
            input.requestFocus()
        } else {
            status.text = result.error
            status.setTextColor(getColor(R.color.red))
            status.visibility = View.VISIBLE
        }
    }

    private fun persistLogoPermission(uri: Uri) {
        try {
            contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } catch (_: SecurityException) {
            // Some document providers grant access only for the current session.
        }
    }

    private fun renderLogo(uri: Uri) {
        try {
            clubLogo.setImageURI(null)
            clubLogo.setImageURI(uri)
            if (clubLogo.drawable == null) clubLogo.setImageResource(R.mipmap.ic_launcher)
        } catch (_: Exception) {
            clubLogo.setImageResource(R.mipmap.ic_launcher)
        }
    }

    private fun setupLogout() {
        findViewById<MaterialButton>(R.id.btnCerrarSesion).setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle(SessionDialogText.logoutTitle)
                .setMessage(SessionDialogText.logoutMessage)
                .setPositiveButton(SessionDialogText.confirm) { _, _ ->
                    startActivity(Intent(this, LoginActivity::class.java))
                    finish()
                }
                .setNegativeButton(SessionDialogText.cancel, null)
                .show()
        }
    }

    private fun formatNumber(value: Double): String =
        if (value % 1.0 == 0.0) value.toLong().toString() else value.toString()
}
