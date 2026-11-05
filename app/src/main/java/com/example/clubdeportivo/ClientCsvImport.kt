package com.example.clubdeportivo

import java.text.Normalizer
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.ResolverStyle

object ClientCsvImport {
    const val MAX_ROWS = 2000
    const val MAX_BYTES = 1_000_000

    enum class Column(val header: String, val label: String, val aliases: Set<String>) {
        DNI("dni", "DNI", setOf("dni", "documento", "nro_documento", "numero_documento")),
        NAME("nombre", "Nombre", setOf("nombre", "nombres")),
        LAST_NAME("apellido", "Apellido", setOf("apellido", "apellidos")),
        BIRTH_DATE(
            "fecha_nacimiento",
            "Fecha de nacimiento",
            setOf("fecha_nacimiento", "fecha_de_nacimiento", "fecha_nac", "nacimiento")
        ),
        PHONE("telefono", "Teléfono", setOf("telefono", "tel", "celular")),
        ADDRESS("direccion", "Domicilio", setOf("direccion", "domicilio")),
        EMAIL("email", "Email", setOf("email", "e_mail", "mail", "correo", "correo_electronico"))
    }

    data class Client(
        val dni: String,
        val name: String,
        val lastName: String,
        val birthDate: String,
        val phone: String,
        val address: String,
        val email: String
    )

    data class Row(
        val lineNumber: Int,
        val displayName: String,
        val dni: String,
        val email: String,
        val client: Client?,
        val errors: List<String>
    ) {
        val isValid: Boolean get() = client != null
    }

    data class Preview(
        val rows: List<Row> = emptyList(),
        val ignoredColumns: List<String> = emptyList(),
        val fileError: String? = null
    ) {
        val validClients: List<Client> get() = rows.mapNotNull { it.client }
        val invalidCount: Int get() = rows.count { !it.isValid }
    }

    private val localDate = DateTimeFormatter.ofPattern("d/M/uuuu").withResolverStyle(ResolverStyle.STRICT)
    private val emptyFileError = "El archivo no tiene filas de clientes"

    fun template(): String = Column.entries.joinToString(",", postfix = "\n") { it.header }

    fun preview(
        text: String,
        existingDnis: Set<String>,
        existingEmails: Set<String>,
        today: LocalDate = LocalDate.now()
    ): Preview {
        val parsed = CsvParser.parse(text)
        parsed.error?.let { return Preview(fileError = it) }
        val header = parsed.records.firstOrNull() ?: return Preview(fileError = emptyFileError)
        val dataRecords = parsed.records.drop(1)
        if (dataRecords.isEmpty()) return Preview(fileError = emptyFileError)
        if (dataRecords.size > MAX_ROWS) {
            return Preview(fileError = "El archivo supera el máximo de $MAX_ROWS filas")
        }

        val columnIndexes = mutableMapOf<Column, Int>()
        val ignoredColumns = mutableListOf<String>()
        header.fields.forEachIndexed { index, rawHeader ->
            val key = normalizeHeader(rawHeader)
            val column = Column.entries.firstOrNull { key in it.aliases }
            if (column != null && column !in columnIndexes) {
                columnIndexes[column] = index
            } else if (rawHeader.isNotBlank()) {
                ignoredColumns.add(rawHeader.trim())
            }
        }
        val missing = Column.entries.filter { it !in columnIndexes }
        if (missing.isNotEmpty()) {
            return Preview(
                ignoredColumns = ignoredColumns,
                fileError = "Faltan columnas obligatorias: ${missing.joinToString(", ") { it.label }}"
            )
        }

        val existingEmailKeys = existingEmails.map { it.trim().lowercase() }.toSet()
        val firstLineByDni = mutableMapOf<String, Int>()
        val firstLineByEmail = mutableMapOf<String, Int>()
        val rows = dataRecords.map { record ->
            validateRow(
                record = record,
                headerSize = header.fields.size,
                columnIndexes = columnIndexes,
                existingDnis = existingDnis,
                existingEmails = existingEmailKeys,
                firstLineByDni = firstLineByDni,
                firstLineByEmail = firstLineByEmail,
                today = today
            )
        }
        return Preview(rows = rows, ignoredColumns = ignoredColumns)
    }

    private fun validateRow(
        record: CsvParser.Record,
        headerSize: Int,
        columnIndexes: Map<Column, Int>,
        existingDnis: Set<String>,
        existingEmails: Set<String>,
        firstLineByDni: MutableMap<String, Int>,
        firstLineByEmail: MutableMap<String, Int>,
        today: LocalDate
    ): Row {
        fun value(column: Column) = record.fields.getOrNull(columnIndexes.getValue(column)).orEmpty().trim()

        val errors = mutableListOf<String>()
        if (record.fields.size > headerSize) {
            errors.add("La fila tiene más columnas que el encabezado; revisa si hay comas sin comillas")
        }

        val dni = value(Column.DNI).filterNot { it == '.' || it == '-' || it.isWhitespace() }
        when {
            dni.isEmpty() -> errors.add("Falta el DNI")
            !UsuarioValidator.dniValido(dni) -> errors.add("DNI inválido: debe tener 8 o 9 dígitos")
            dni in existingDnis -> errors.add("El DNI ya está registrado")
            dni in firstLineByDni -> errors.add("DNI repetido en la fila ${firstLineByDni.getValue(dni)}")
            else -> firstLineByDni[dni] = record.lineNumber
        }

        val name = value(Column.NAME)
        val lastName = value(Column.LAST_NAME)
        requireText(name, "el nombre", 80, errors)
        requireText(lastName, "el apellido", 80, errors)

        val rawBirthDate = value(Column.BIRTH_DATE)
        val birthDate = parseBirthDate(rawBirthDate)
        when {
            rawBirthDate.isEmpty() -> errors.add("Falta la fecha de nacimiento")
            birthDate == null || birthDate.year < 1900 ->
                errors.add("Fecha de nacimiento inválida: usa DD/MM/AAAA")
            birthDate.isAfter(today) -> errors.add("La fecha de nacimiento no puede ser futura")
        }

        val phone = value(Column.PHONE).filterNot { it in " -().".toSet() }
        when {
            phone.isEmpty() -> errors.add("Falta el teléfono")
            !UsuarioValidator.telefonoValido(phone) ->
                errors.add("Teléfono inválido: debe tener entre 9 y 12 dígitos")
        }

        val address = value(Column.ADDRESS)
        requireText(address, "el domicilio", 160, errors)

        val email = value(Column.EMAIL).lowercase()
        when {
            email.isEmpty() -> errors.add("Falta el email")
            !UsuarioValidator.emailValido(email) -> errors.add("Email inválido")
            email in existingEmails -> errors.add("El email ya está registrado")
            email in firstLineByEmail -> errors.add("Email repetido en la fila ${firstLineByEmail.getValue(email)}")
            else -> firstLineByEmail[email] = record.lineNumber
        }

        val displayName = listOf(lastName, name).filter { it.isNotEmpty() }.joinToString(", ")
            .ifEmpty { "Sin nombre" }
        val client = if (errors.isEmpty() && birthDate != null) {
            Client(dni, name, lastName, birthDate.toString(), phone, address, email)
        } else {
            null
        }
        return Row(record.lineNumber, displayName, dni, email, client, errors)
    }

    private fun requireText(value: String, label: String, maxLength: Int, errors: MutableList<String>) {
        when {
            value.isEmpty() -> errors.add("Falta $label")
            value.length > maxLength -> errors.add(
                "${label.replaceFirstChar { it.uppercase() }} supera $maxLength caracteres"
            )
        }
    }

    private fun parseBirthDate(input: String): LocalDate? =
        runCatching { LocalDate.parse(input, localDate) }.getOrNull()
            ?: runCatching { LocalDate.parse(input) }.getOrNull()

    private fun normalizeHeader(input: String): String =
        Normalizer.normalize(input.trim().lowercase(), Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")
            .replace(Regex("[^a-z0-9]+"), "_")
            .trim('_')
}

object ClientCsvImportText {
    fun summary(validCount: Int, invalidCount: Int): String {
        val valid = if (validCount == 1) "1 fila lista para importar" else "$validCount filas listas para importar"
        val invalid = when (invalidCount) {
            0 -> "sin errores"
            1 -> "1 fila con errores"
            else -> "$invalidCount filas con errores"
        }
        return "$valid · $invalid"
    }

    fun importButton(validCount: Int): String = when (validCount) {
        0 -> "No hay clientes para importar"
        1 -> "Importar 1 cliente"
        else -> "Importar $validCount clientes"
    }

    fun confirmMessage(validCount: Int, invalidCount: Int): String {
        val clients = if (validCount == 1) "1 cliente nuevo" else "$validCount clientes nuevos"
        val skipped = when (invalidCount) {
            0 -> ""
            1 -> "\n\nLa fila con errores no se importa."
            else -> "\n\nLas $invalidCount filas con errores no se importan."
        }
        return "Se van a registrar $clients como no socios.$skipped\n\n" +
            "Luego se pueden editar o dar de baja desde Listados."
    }

    fun imported(count: Int): String =
        if (count == 1) "Se importó 1 cliente" else "Se importaron $count clientes"

    fun ignoredColumns(columns: List<String>): String =
        "Columnas ignoradas: ${columns.joinToString(", ")}"

    fun rowLabel(lineNumber: Int): String = "Fila $lineNumber"

    fun rowDetail(dni: String, email: String): String =
        listOf(dni.takeIf { it.isNotEmpty() }?.let { "DNI $it" }, email.takeIf { it.isNotEmpty() })
            .filterNotNull()
            .joinToString(" · ")
            .ifEmpty { "Sin DNI ni email" }

    fun rowErrors(errors: List<String>): String = errors.joinToString("\n") { "• $it" }
}
