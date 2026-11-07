package com.example.clubdeportivo

    import android.content.ContentValues
    import android.content.Context
    import android.database.Cursor
    import android.database.sqlite.SQLiteDatabase
    import android.database.sqlite.SQLiteOpenHelper
    import androidx.core.database.getIntOrNull
    import java.text.SimpleDateFormat
    import java.time.LocalDate
    import java.util.Date
    import java.util.Locale

    class DBHelper(context: Context) : SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {

    private companion object {
        const val DB_NAME = "app_clubDeportivo.db"
        const val DB_VERSION = 4

        // pagos_actividad.id_actividad guarda el id del horario (dias_horarios), no el de la actividad.
        const val JOIN_ACTIVIDAD_DEL_PAGO = """
            LEFT JOIN dias_horarios dh ON dh.id = p.id_actividad
            LEFT JOIN actividad_profesor ap ON ap.id = dh.actividad_profesor_id
            LEFT JOIN actividades a ON a.id_actividad = ap.actividad_id
        """
    }

    // ----------------------------------- Creacion DB -----------------------------------------
    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.execSQL("PRAGMA foreign_keys=ON")
        db.setForeignKeyConstraintsEnabled(true)
    }
    // Crear tablas
    override fun onCreate(db: SQLiteDatabase) {
        // Create TABLES
        db.execSQL("""
            CREATE TABLE actividades (
              id_actividad INTEGER PRIMARY KEY AUTOINCREMENT,
              nombre TEXT NOT NULL,
              precio NUMERIC NOT NULL
            );
        """.trimIndent())
        db.execSQL("""
            CREATE TABLE clientes (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                nombre TEXT NOT NULL,
                apellido TEXT NOT NULL,
                dni TEXT NOT NULL UNIQUE,
                fecha_nac TEXT NOT NULL,
                telefono TEXT NOT NULL,
                direccion TEXT NOT NULL,
                fecha_inscripcion TEXT NOT NULL,
                ficha_medica BOOLEAN NOT NULL DEFAULT 1,
                email TEXT NOT NULL UNIQUE,
                esSocio BOOLEAN NOT NULL DEFAULT 0,
                activo BOOLEAN NOT NULL DEFAULT 1,
                carnet BOOLEAN NOT NULL DEFAULT 0
                );
            """.trimIndent())
        db.execSQL("""
            CREATE TABLE profesores (
              dni TEXT PRIMARY KEY,
              nombre TEXT NOT NULL,
              apellido TEXT NOT NULL,
              fecha_nac TEXT NOT NULL,
              telefono TEXT NOT NULL,
              direccion TEXT NOT NULL,
              fecha_inscripcion TEXT NOT NULL,
              ficha_medica INTEGER NOT NULL,
              email TEXT NOT NULL,
              activo INTEGER NOT NULL DEFAULT 0,
              titulo TEXT
            );
            """.trimIndent())
        db.execSQL("""
            CREATE TABLE cuotas (
              idCuota INTEGER PRIMARY KEY AUTOINCREMENT,
              idCliente INTEGER NOT NULL,
              monto NUMERIC,
              fechaPago TEXT NOT NULL,
              formaPago TEXT,
              estadoDelPago INTEGER NOT NULL,
              fechaVencimiento TEXT NOT NULL,
              FOREIGN KEY (idCliente) REFERENCES clientes(id)
            );
            """.trimIndent())
        db.execSQL("""
            CREATE TABLE pagos_actividad (
              id_pago INTEGER PRIMARY KEY AUTOINCREMENT,
              idCliente TEXT NOT NULL,
              id_actividad INTEGER NOT NULL,
              fecha_pago TEXT NOT NULL,
              forma_pago TEXT NOT NULL,
              monto NUMERIC NOT NULL,
              FOREIGN KEY (idCliente) REFERENCES clientes(id),
              FOREIGN KEY (id_actividad) REFERENCES dias_horarios(id)
            );
            """.trimIndent())
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS actividad_profesor (
              id INTEGER PRIMARY KEY AUTOINCREMENT,
              actividad_id INTEGER NOT NULL,
              profesor_dni TEXT NOT NULL,
              activo INTEGER NOT NULL DEFAULT 1,
              motivo_baja TEXT,
              fecha_baja TEXT,
              UNIQUE(actividad_id, profesor_dni),
              FOREIGN KEY (actividad_id) REFERENCES actividades(id_actividad) ON DELETE CASCADE,
              FOREIGN KEY (profesor_dni) REFERENCES profesores(dni) ON DELETE CASCADE);
              """.trimIndent())
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS dias_horarios (
              id INTEGER PRIMARY KEY AUTOINCREMENT,
              actividad_profesor_id INTEGER NOT NULL,
              dia INTEGER NOT NULL,            -- 0..6 (Dom..Sáb)
              hora_inicio INTEGER NOT NULL,    -- minutos
              hora_fin INTEGER NOT NULL,       -- minutos
              activo INTEGER NOT NULL DEFAULT 1,
              motivo_baja TEXT,
              fecha_baja TEXT,
              FOREIGN KEY(actividad_profesor_id) REFERENCES actividad_profesor(id) ON DELETE CASCADE,
              UNIQUE(actividad_profesor_id, dia, hora_inicio, hora_fin));
            """.trimIndent())
        db.execSQL("""
            CREATE UNIQUE INDEX IF NOT EXISTS ux_dh_unico_activo
            ON dias_horarios(actividad_profesor_id, dia, hora_inicio, hora_fin)
            WHERE activo = 1;
            """.trimIndent())
        db.execSQL("""
            CREATE INDEX IF NOT EXISTS idx_ap_act_prof
            ON actividad_profesor(actividad_id, profesor_dni);
            """.trimIndent())
        db.execSQL("""
            CREATE INDEX IF NOT EXISTS idx_dh_apid
            ON dias_horarios(actividad_profesor_id);
            """.trimIndent())
        db.execSQL("""
            CREATE TRIGGER IF NOT EXISTS tr_ap_autoclean
            AFTER DELETE ON dias_horarios
            BEGIN
              DELETE FROM actividad_profesor
              WHERE id = OLD.actividad_profesor_id
                AND NOT EXISTS (
                  SELECT 1 FROM dias_horarios WHERE actividad_profesor_id = OLD.actividad_profesor_id
                );
            END;
            """.trimIndent())

        createClubConfigurationTable(db)
        migrateToVersion4(db)
        ensureDefaultClubConfiguration(db)
        InitialDataSeeder.seed(db)
        backfillPaymentBaseAmounts(db)
    }

    // Actualiza la base sin borrar datos existentes.
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        DatabaseMigrationPlanner.pendingSteps(oldVersion, newVersion).forEach { step ->
            when (step) {
                2 -> migrateToVersion2(db)
                3 -> migrateToVersion3(db)
                4 -> migrateToVersion4(db)
            }
        }
        ensureDefaultClubConfiguration(db)
    }

    private fun migrateToVersion2(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE UNIQUE INDEX IF NOT EXISTS idx_dh_active_unique
            ON dias_horarios(actividad_profesor_id, dia, hora_inicio, hora_fin)
            WHERE activo = 1;
        """.trimIndent())
        db.execSQL("""
            CREATE UNIQUE INDEX IF NOT EXISTS idx_ap_unique
            ON actividad_profesor(actividad_id, profesor_dni);
        """.trimIndent())
        db.execSQL("""
            CREATE INDEX IF NOT EXISTS idx_dh_ap
            ON dias_horarios(actividad_profesor_id);
        """.trimIndent())
        db.execSQL("""
            CREATE TRIGGER IF NOT EXISTS trg_delete_ap_if_no_dh
            AFTER DELETE ON dias_horarios
            BEGIN
              DELETE FROM actividad_profesor
              WHERE id = OLD.actividad_profesor_id
              AND NOT EXISTS (
                  SELECT 1 FROM dias_horarios WHERE actividad_profesor_id = OLD.actividad_profesor_id
              );
            END;
        """.trimIndent())
    }

    private fun migrateToVersion3(db: SQLiteDatabase) {
        createClubConfigurationTable(db)
    }

    private fun migrateToVersion4(db: SQLiteDatabase) {
        val rules = BillingRules.DEFAULT
        addColumnIfMissing(db, "club_configuration", "late_fee_type", "TEXT NOT NULL DEFAULT '${rules.lateFeeType.code}'")
        addColumnIfMissing(db, "club_configuration", "late_fee_value", "REAL NOT NULL DEFAULT ${rules.lateFeeValue}")
        addColumnIfMissing(db, "club_configuration", "late_fee_frequency", "TEXT NOT NULL DEFAULT '${rules.lateFeeFrequency.code}'")
        addColumnIfMissing(db, "club_configuration", "late_fee_max_months", "INTEGER NOT NULL DEFAULT ${rules.lateFeeMaxMonths}")
        addColumnIfMissing(db, "club_configuration", "benefit_stacking", "TEXT NOT NULL DEFAULT '${rules.benefitStacking.code}'")
        addColumnIfMissing(db, "club_configuration", "scholarship_waives_late_fee", "INTEGER NOT NULL DEFAULT ${if (rules.scholarshipWaivesLateFee) 1 else 0}")
        addColumnIfMissing(db, "club_configuration", "late_loses_discount", "INTEGER NOT NULL DEFAULT ${if (rules.lateLosesDiscount) 1 else 0}")

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS descuentos (
                id_descuento INTEGER PRIMARY KEY AUTOINCREMENT,
                nombre TEXT NOT NULL,
                tipo TEXT NOT NULL,
                valor REAL NOT NULL,
                alcance TEXT NOT NULL,
                activo INTEGER NOT NULL DEFAULT 1
            );
        """.trimIndent())
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS cliente_beneficios (
                id_cliente INTEGER PRIMARY KEY,
                id_descuento INTEGER,
                beca_porcentaje INTEGER,
                beca_alcance TEXT,
                beca_motivo TEXT,
                beca_hasta TEXT,
                FOREIGN KEY (id_cliente) REFERENCES clientes(id) ON DELETE CASCADE,
                FOREIGN KEY (id_descuento) REFERENCES descuentos(id_descuento) ON DELETE SET NULL
            );
        """.trimIndent())

        listOf("cuotas", "pagos_actividad").forEach { table ->
            addColumnIfMissing(db, table, "monto_base", "NUMERIC")
            addColumnIfMissing(db, table, "descuento", "NUMERIC NOT NULL DEFAULT 0")
            addColumnIfMissing(db, table, "recargo", "NUMERIC NOT NULL DEFAULT 0")
            addColumnIfMissing(db, table, "detalle_ajustes", "TEXT")
        }
        backfillPaymentBaseAmounts(db)
    }

    private fun backfillPaymentBaseAmounts(db: SQLiteDatabase) {
        listOf("cuotas", "pagos_actividad").filter { tableExists(db, it) }.forEach { table ->
            db.execSQL("UPDATE $table SET monto_base = monto WHERE monto_base IS NULL")
        }
    }

    private fun tableExists(db: SQLiteDatabase, table: String): Boolean =
        db.rawQuery(
            "SELECT 1 FROM sqlite_master WHERE type = 'table' AND name = ?",
            arrayOf(table)
        ).use { it.moveToFirst() }

    private fun addColumnIfMissing(db: SQLiteDatabase, table: String, column: String, definition: String) {
        if (!tableExists(db, table)) return
        val exists = db.rawQuery("PRAGMA table_info($table)", null).use { cursor ->
            val nameIndex = cursor.getColumnIndexOrThrow("name")
            generateSequence { if (cursor.moveToNext()) cursor.getString(nameIndex) else null }
                .any { it == column }
        }
        if (!exists) db.execSQL("ALTER TABLE $table ADD COLUMN $column $definition")
    }

    private fun createClubConfigurationTable(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS club_configuration (
                id INTEGER PRIMARY KEY CHECK (id = 1),
                name TEXT NOT NULL,
                address TEXT NOT NULL DEFAULT '',
                phone TEXT NOT NULL DEFAULT '',
                email TEXT NOT NULL DEFAULT '',
                currency TEXT NOT NULL,
                monthly_fee REAL NOT NULL,
                due_day INTEGER NOT NULL,
                grace_days INTEGER NOT NULL,
                accepts_cash INTEGER NOT NULL,
                accepts_transfer INTEGER NOT NULL,
                accepts_card INTEGER NOT NULL,
                logo_uri TEXT
            );
        """.trimIndent())
    }

    private fun ensureDefaultClubConfiguration(db: SQLiteDatabase) {
        db.insertWithOnConflict(
            "club_configuration",
            null,
            clubConfigurationValues(ClubConfiguration.DEFAULT),
            SQLiteDatabase.CONFLICT_IGNORE
        )
    }

    fun obtenerConfiguracionClub(): ClubConfiguration {
        readableDatabase.query(
            "club_configuration",
            arrayOf(
                "name",
                "address",
                "phone",
                "email",
                "currency",
                "monthly_fee",
                "due_day",
                "grace_days",
                "accepts_cash",
                "accepts_transfer",
                "accepts_card",
                "logo_uri",
                "late_fee_type",
                "late_fee_value",
                "late_fee_frequency",
                "late_fee_max_months",
                "benefit_stacking",
                "scholarship_waives_late_fee",
                "late_loses_discount"
            ),
            "id = 1",
            null,
            null,
            null,
            null
        ).use { cursor ->
            if (cursor.moveToFirst()) {
                return ClubConfiguration(
                    name = cursor.getString(0),
                    address = cursor.getString(1),
                    phone = cursor.getString(2),
                    email = cursor.getString(3),
                    currency = ClubCurrency.fromCode(cursor.getString(4)),
                    monthlyFee = cursor.getDouble(5),
                    dueDay = cursor.getInt(6),
                    graceDays = cursor.getInt(7),
                    acceptsCash = cursor.getInt(8) == 1,
                    acceptsTransfer = cursor.getInt(9) == 1,
                    acceptsCard = cursor.getInt(10) == 1,
                    logoUri = if (cursor.isNull(11)) null else cursor.getString(11),
                    billingRules = BillingRules(
                        lateFeeType = AdjustmentType.fromCode(cursor.getString(12)),
                        lateFeeValue = cursor.getDouble(13),
                        lateFeeFrequency = LateFeeFrequency.fromCode(cursor.getString(14)),
                        lateFeeMaxMonths = cursor.getInt(15),
                        benefitStacking = BenefitStacking.fromCode(cursor.getString(16)),
                        scholarshipWaivesLateFee = cursor.getInt(17) == 1,
                        lateLosesDiscount = cursor.getInt(18) == 1
                    )
                )
            }
        }

        val defaultConfiguration = ClubConfiguration.DEFAULT
        guardarConfiguracionClub(defaultConfiguration)
        return defaultConfiguration
    }

    fun guardarConfiguracionClub(configuration: ClubConfiguration): Boolean {
        val rowId = writableDatabase.insertWithOnConflict(
            "club_configuration",
            null,
            clubConfigurationValues(configuration),
            SQLiteDatabase.CONFLICT_REPLACE
        )
        return rowId != -1L
    }

    private fun clubConfigurationValues(configuration: ClubConfiguration) = ContentValues().apply {
        put("id", 1)
        put("name", configuration.name)
        put("address", configuration.address)
        put("phone", configuration.phone)
        put("email", configuration.email)
        put("currency", configuration.currency.code)
        put("monthly_fee", configuration.monthlyFee)
        put("due_day", configuration.dueDay)
        put("grace_days", configuration.graceDays)
        put("accepts_cash", if (configuration.acceptsCash) 1 else 0)
        put("accepts_transfer", if (configuration.acceptsTransfer) 1 else 0)
        put("accepts_card", if (configuration.acceptsCard) 1 else 0)
        putOrNull("logo_uri", configuration.logoUri)
        val rules = configuration.billingRules
        put("late_fee_type", rules.lateFeeType.code)
        put("late_fee_value", rules.lateFeeValue)
        put("late_fee_frequency", rules.lateFeeFrequency.code)
        put("late_fee_max_months", rules.lateFeeMaxMonths)
        put("benefit_stacking", rules.benefitStacking.code)
        put("scholarship_waives_late_fee", if (rules.scholarshipWaivesLateFee) 1 else 0)
        put("late_loses_discount", if (rules.lateLosesDiscount) 1 else 0)
    }

    // ----------------------------------------- READ -------------------------------------------

    // Listados
    fun obtenerNoSocios(): List<NoSocioCard> {
        val lista = mutableListOf<NoSocioCard>()
        val db = readableDatabase
        val sql = """
        SELECT 
            ns.nombre,
            ns.apellido,
            ns.dni,
            MAX(p.fecha_pago) AS ultima_pago,
            a.nombre AS actividad_pagada
        FROM clientes ns
        LEFT JOIN pagos_actividad p
               ON p.idCliente = ns.id
        $JOIN_ACTIVIDAD_DEL_PAGO
        WHERE ns.esSocio = false AND ns.activo = 1
        GROUP BY ns.dni, ns.nombre, ns.apellido
        ORDER BY ns.apellido, ns.nombre;

    """.trimIndent()
        val c = db.rawQuery(sql, null)
        if (c.moveToFirst()) {
            do {
                val nombre = c.getString(0)
                val apellido = c.getString(1)
                val dni = c.getString(2)
                val ultimoPago = if (!c.isNull(3)) c.getString(3) else null
                val nombreAct = if (!c.isNull(3)) c.getString(4) else null
                lista.add(NoSocioCard(nombre, apellido, dni, ultimoPago, nombreAct))
            } while (c.moveToNext())
        }
        c.close(); db.close()
        return lista
    }
    fun obtenerSocios(): List<SocioCard> {
        val lista = mutableListOf<SocioCard>()
        val db = readableDatabase
        val sql = """
        SELECT s.nombre, s.apellido, s.dni, MAX(c.fechaPago) AS ultimo_pago
        FROM clientes s
        LEFT JOIN cuotas c ON c.idCliente = s.id
        WHERE s.activo = 1 AND esSocio = 1
        GROUP BY s.id, s.nombre, s.apellido, s.dni
        ORDER BY s.apellido, s.nombre
    """.trimIndent()
        val c = db.rawQuery(sql, null)
        if (c.moveToFirst()) {
            do {
                val nombre = c.getString(0)
                val apellido = c.getString(1)
                val dni = c.getString(2)
                val ultimo = if (!c.isNull(3)) c.getString(3) else null
                lista.add(SocioCard(nombre, apellido, dni, ultimo))
            } while (c.moveToNext())
        }
        c.close(); db.close()
        return lista
    }
    fun obtenerVencimientos(fecha: String): List<VencimientoCard> {
            val lista = mutableListOf<VencimientoCard>()
            val db = readableDatabase
            val sql = """
                SELECT s.nombre,
                       s.apellido,
                       s.dni,
                       c.fechaVencimiento,
                       (SELECT MAX(c2.fechaPago)
                        FROM cuotas c2
                        WHERE c2.idCliente = s.id) AS ultimo_pago
                FROM cuotas c
                JOIN clientes s ON s.id = c.idCliente
                -- Nos quedamos solo con la ÚLTIMA cuota de cada socio
                JOIN (
                    SELECT idCliente, MAX(fechaVencimiento) AS maxVenc
                    FROM cuotas
                    GROUP BY idCliente
                ) ult ON ult.idCliente = c.idCliente
                     AND ult.maxVenc = c.fechaVencimiento
                WHERE s.activo = 1
                    AND esSocio = 1
                ORDER BY s.apellido, s.nombre
            """.trimIndent()

            val c = db.rawQuery(sql, null)
            if (c.moveToFirst()) {
                do {
                    val nombre  = c.getString(0)
                    val apellido = c.getString(1)
                    val dni      = c.getString(2)
                    val fv       = c.getString(3)   // fechaVencimiento (YYYY-MM-DD)
                    val ultimo   = if (!c.isNull(4)) c.getString(4) else null

                    lista.add(VencimientoCard(nombre, apellido, dni, fv, ultimo))
                } while (c.moveToNext())
            }
            c.close()
            db.close()
            return lista
        }
    fun obtenerResumenVencimientos(fecha: String): ResumenVencimientos {
        val hoy = LocalDate.parse(fecha)
        var alDia = 0
        var porVencer = 0
        var vencidos = 0

        obtenerVencimientos(fecha).forEach { item ->
            when (VencimientoCalculator.clasificar(item.fechaVenc, hoy).categoria) {
                "Al dia" -> alDia++
                "Por vencer" -> porVencer++
                "Vencido" -> vencidos++
            }
        }

        return ResumenVencimientos(alDia, porVencer, vencidos)
    }
    fun obtenerActividadesDelDia(dia: Int): List<InicioActivity.ActividadHoy> {
        val lista = mutableListOf<InicioActivity.ActividadHoy>()
        val db = readableDatabase
        val sql = """
        SELECT a.nombre, a.precio, dh.id, dh.dia, dh.hora_inicio, dh.hora_fin
        FROM dias_horarios dh
        JOIN actividad_profesor ap ON ap.id = dh.actividad_profesor_id
        JOIN actividades a         ON a.id_actividad = ap.actividad_id
         WHERE dh.activo = 1
           AND dh.dia = ?
        ORDER BY dh.hora_inicio
    """.trimIndent()

        fun hhmm(mins: Int) = String.format("%02d:%02d", mins / 60, mins % 60)

        db.rawQuery(sql, arrayOf(dia.toString())).use { c ->
            while (c.moveToNext()) {
                val id = c.getInt(2)
                val nombre = c.getString(0)
                val dia = c.getInt(3)
                val precio = c.getDouble(1)
                val hIni = hhmm(c.getInt(4))
                val hFin = hhmm(c.getInt(5))
                lista.add(InicioActivity.ActividadHoy(id, nombre, dia, hIni, hFin, precio))
            }
        }
        return lista
    }
    fun obtenerActividadesPorHorario(): List<ActividadCard> {
        val db = readableDatabase
        val sql = """
        SELECT
            a.id_actividad                  AS id_actividad,
            dh.id                           AS dh_id,
            a.nombre                        AS nombre,
            a.precio                        AS precio,
            (p.apellido || ' ' || p.nombre) AS profesor,
            dh.dia                          AS dia,
            dh.hora_inicio                  AS hora_inicio,
            dh.hora_fin                     AS hora_fin
        FROM dias_horarios dh
        JOIN actividad_profesor ap ON ap.id = dh.actividad_profesor_id
        JOIN actividades a         ON a.id_actividad = ap.actividad_id
        JOIN profesores p          ON p.dni = ap.profesor_dni
        WHERE dh.activo = 1 AND (ap.activo IS NULL OR ap.activo = 1)
        ORDER BY a.nombre, profesor, dh.dia, dh.hora_inicio
    """.trimIndent()

        val lista = mutableListOf<ActividadCard>()
        db.rawQuery(sql, null).use { c ->
            val idxAct   = c.getColumnIndexOrThrow("id_actividad")
            val idxDh    = c.getColumnIndexOrThrow("dh_id")
            val idxNom   = c.getColumnIndexOrThrow("nombre")
            val idxPrec  = c.getColumnIndexOrThrow("precio")
            val idxProf  = c.getColumnIndexOrThrow("profesor")
            val idxDia   = c.getColumnIndexOrThrow("dia")
            val idxIni   = c.getColumnIndexOrThrow("hora_inicio")
            val idxFin   = c.getColumnIndexOrThrow("hora_fin")

            while (c.moveToNext()) {
                val dia = c.getInt(idxDia)
                val ini = c.getInt(idxIni)
                val fin = c.getInt(idxFin)
                lista += ActividadCard(
                    idActividad    = c.getInt(idxAct),
                    idDiaHorario   = c.getInt(idxDh),
                    nombre         = c.getString(idxNom),
                    precio         = c.getDouble(idxPrec),
                    profesor       = c.getString(idxProf),
                    dia            = dia,
                    horaInicio     = ini,
                    horaFin        = fin,
                    etiquetaHorario= "${etiquetaDia(dia)} ${hhmm(ini)}-${hhmm(fin)}"
                )
            }
        }
        return lista
    }

    // Busquedas
    fun obtenerPersonaPorDni(dni: String): PersonaDTO? {
        val db = this.readableDatabase
        db.query(
            "clientes",
            null,
            "dni = ? AND activo = 1",
            arrayOf(dni),
            null, null, null
        ).use { c ->
            if (c.moveToFirst()) {
                return PersonaDTO(
                    id = c.getInt(c.getColumnIndexOrThrow("id")),
                    dni = c.getStringOrNull("dni") ?: dni,
                    nombre = c.getStringOrNull("nombre"),
                    apellido = c.getStringOrNull("apellido"),
                    telefono = c.getStringOrNull("telefono"),
                    direccion = c.getStringOrNull("direccion"),
                    email = c.getStringOrNull("email"),
                    fecha_nac = c.getStringOrNull("fecha_nac"),
                    fichaMedica = c.getStringOrNull("ficha_medica"),
                    esSocio = c.getInt(c.getColumnIndexOrThrow("esSocio")) == 1
                )
            }
        }
        return null
    }
    fun buscarActividadesPorNombre(texto: String): List<ActividadCard> {
        val db = readableDatabase
        val like = "%${texto.trim()}%"
        val sql = """
        SELECT
            a.id_actividad                  AS id_actividad,
            dh.id                           AS dh_id,
            a.nombre                        AS nombre,
            a.precio                        AS precio,
            (p.apellido || ' ' || p.nombre) AS profesor,
            dh.dia                          AS dia,
            dh.hora_inicio                  AS hora_inicio,
            dh.hora_fin                     AS hora_fin
        FROM dias_horarios dh
        JOIN actividad_profesor ap ON ap.id = dh.actividad_profesor_id
        JOIN actividades a         ON a.id_actividad = ap.actividad_id
        JOIN profesores p          ON p.dni = ap.profesor_dni
        WHERE dh.activo = 1
          AND a.nombre LIKE ?
        ORDER BY a.nombre, profesor, dh.dia, dh.hora_inicio
    """.trimIndent()

        val lista = mutableListOf<ActividadCard>()
        db.rawQuery(sql, arrayOf(like)).use { c ->
            val idxAct  = c.getColumnIndexOrThrow("id_actividad")
            val idxDh   = c.getColumnIndexOrThrow("dh_id")
            val idxNom  = c.getColumnIndexOrThrow("nombre")
            val idxPrec = c.getColumnIndexOrThrow("precio")
            val idxProf = c.getColumnIndexOrThrow("profesor")
            val idxDia  = c.getColumnIndexOrThrow("dia")
            val idxIni  = c.getColumnIndexOrThrow("hora_inicio")
            val idxFin  = c.getColumnIndexOrThrow("hora_fin")

            while (c.moveToNext()) {
                val dia = c.getInt(idxDia)
                val ini = c.getInt(idxIni)
                val fin = c.getInt(idxFin)
                lista += ActividadCard(
                    idActividad    = c.getInt(idxAct),
                    idDiaHorario   = c.getInt(idxDh),
                    nombre         = c.getString(idxNom),
                    precio         = c.getDouble(idxPrec),
                    profesor       = c.getString(idxProf),
                    dia            = dia,
                    horaInicio     = ini,
                    horaFin        = fin,
                    etiquetaHorario= "${etiquetaDia(dia)} ${hhmm(ini)}-${hhmm(fin)}"
                )
            }
        }
        return lista
    }
    fun obtenerMetricasInicio(fechaIso: String, dia: Int, anio: Int, mes: Int): MetricasInicio {
        val db = readableDatabase
        val sociosActivos = android.database.DatabaseUtils.longForQuery(
            db,
            "SELECT COUNT(*) FROM clientes WHERE activo = 1 AND esSocio = 1",
            null
        ).toInt()
        val noSociosActivos = android.database.DatabaseUtils.longForQuery(
            db,
            "SELECT COUNT(*) FROM clientes WHERE activo = 1 AND esSocio = 0",
            null
        ).toInt()
        val vencidos = android.database.DatabaseUtils.longForQuery(
            db,
            """
                SELECT COUNT(*)
                FROM clientes s
                JOIN cuotas c ON c.idCliente = s.id
                JOIN (
                    SELECT idCliente, MAX(fechaVencimiento) AS maxVenc
                    FROM cuotas
                    GROUP BY idCliente
                ) ult ON ult.idCliente = c.idCliente
                     AND ult.maxVenc = c.fechaVencimiento
                WHERE s.activo = 1
                  AND s.esSocio = 1
                  AND c.fechaVencimiento < ?
            """.trimIndent(),
            arrayOf(fechaIso)
        ).toInt()
        val actividadesHoy = android.database.DatabaseUtils.longForQuery(
            db,
            """
                SELECT COUNT(*)
                FROM dias_horarios dh
                JOIN actividad_profesor ap ON ap.id = dh.actividad_profesor_id
                WHERE dh.activo = 1
                  AND ap.activo = 1
                  AND dh.dia = ?
            """.trimIndent(),
            arrayOf(dia.toString())
        ).toInt()

        val mesStr = String.format("%02d", mes)
        var ingresosMes = 0.0
        db.rawQuery(
            """
                SELECT
                    (SELECT IFNULL(SUM(monto), 0) FROM cuotas
                     WHERE strftime('%Y', fechaPago) = ? AND strftime('%m', fechaPago) = ?) +
                    (SELECT IFNULL(SUM(monto), 0) FROM pagos_actividad
                     WHERE strftime('%Y', fecha_pago) = ? AND strftime('%m', fecha_pago) = ?)
            """.trimIndent(),
            arrayOf(anio.toString(), mesStr, anio.toString(), mesStr)
        ).use { c ->
            if (c.moveToFirst()) ingresosMes = c.getDouble(0)
        }

        return MetricasInicio(
            sociosActivos = sociosActivos,
            noSociosActivos = noSociosActivos,
            vencidos = vencidos,
            ingresosMes = ingresosMes,
            actividadesHoy = actividadesHoy
        )
    }

    fun obtenerCuentaCorriente(dni: String): CuentaCorrienteDTO? {
        val persona = obtenerPersonaPorDni(dni) ?: return null
        val db = readableDatabase

        var ultimoPagoCuota: String? = null
        var proximoVencimiento: String? = null
        var totalCuotas = 0.0
        db.rawQuery(
            """
                SELECT fechaPago, fechaVencimiento, IFNULL(monto, 0)
                FROM cuotas
                WHERE idCliente = ?
                ORDER BY fechaPago DESC, idCuota DESC
                LIMIT 1
            """.trimIndent(),
            arrayOf(persona.id.toString())
        ).use { c ->
            if (c.moveToFirst()) {
                ultimoPagoCuota = c.getString(0)
                proximoVencimiento = c.getString(1)
                totalCuotas = c.getDouble(2)
            }
        }

        var ultimoPagoActividad: String? = null
        var totalActividades = 0.0
        db.rawQuery(
            """
                SELECT MAX(fecha_pago), IFNULL(SUM(monto), 0)
                FROM pagos_actividad
                WHERE idCliente = ?
            """.trimIndent(),
            arrayOf(persona.id.toString())
        ).use { c ->
            if (c.moveToFirst()) {
                ultimoPagoActividad = c.getString(0)
                totalActividades = c.getDouble(1)
            }
        }

        val movimientos = obtenerMovimientosCuenta(db, persona.id.toString())

        val estado = if (persona.esSocio) {
            val cobroHoy = calcularCobroCuota(obtenerConfiguracionClub(), persona, LocalDate.now())
            CuentaCorrienteCalculator.evaluarSocio(proximoVencimiento, cobroHoy.total)
        } else {
            CuentaCorrienteCalculator.evaluarNoSocio(ultimoPagoActividad != null)
        }

        return CuentaCorrienteDTO(
            estado = estado.estado,
            detalleEstado = estado.detalle,
            ultimoPagoCuota = ultimoPagoCuota,
            proximoVencimiento = proximoVencimiento,
            ultimoPagoActividad = ultimoPagoActividad,
            totalCuotas = totalCuotas,
            totalActividades = totalActividades,
            deudaEstimada = estado.deudaEstimada,
            movimientos = movimientos
        )
    }

    private fun obtenerMovimientosCuenta(db: SQLiteDatabase, idCliente: String): List<MovimientoCuenta> {
        val movimientos = mutableListOf<MovimientoCuenta>()
        db.rawQuery(
            """
                SELECT fechaPago, IFNULL(monto, 0), formaPago, detalle_ajustes
                FROM cuotas
                WHERE idCliente = ?
                ORDER BY fechaPago DESC, idCuota DESC
                LIMIT 5
            """.trimIndent(),
            arrayOf(idCliente)
        ).use { c ->
            while (c.moveToNext()) {
                movimientos += MovimientoCuenta(
                    tipo = "Cuota",
                    fecha = c.getString(0),
                    monto = c.getDouble(1),
                    detalle = c.getString(2),
                    ajustes = c.getStringOrNull("detalle_ajustes")
                )
            }
        }
        db.rawQuery(
            """
                SELECT p.fecha_pago, IFNULL(p.monto, 0), a.nombre, p.detalle_ajustes
                FROM pagos_actividad p
                $JOIN_ACTIVIDAD_DEL_PAGO
                WHERE p.idCliente = ?
                ORDER BY p.fecha_pago DESC, p.id_pago DESC
                LIMIT 5
            """.trimIndent(),
            arrayOf(idCliente)
        ).use { c ->
            while (c.moveToNext()) {
                movimientos += MovimientoCuenta(
                    tipo = "Actividad",
                    fecha = c.getString(0),
                    monto = c.getDouble(1),
                    detalle = if (!c.isNull(2)) c.getString(2) else "Sin actividad",
                    ajustes = c.getStringOrNull("detalle_ajustes")
                )
            }
        }
        return movimientos.sortedWith(compareByDescending<MovimientoCuenta> { it.fecha }.thenBy { it.tipo }).take(5)
    }

    fun obtenerResumenPagosMes(anio: Int, mes: Int): ResumenPagosMes {
            val db = readableDatabase
            val anioStr = anio.toString()
            val mesStr = String.format("%02d", mes) // "01", "02", ..., "12"

            // ----- Cuotas de socios -----
            var cantSocios = 0
            var montoCuotas = 0.0
            db.rawQuery("""
                SELECT COUNT(DISTINCT idCliente) AS cant, IFNULL(SUM(monto),0) AS total
                FROM cuotas
                WHERE strftime('%Y', fechaPago) = ? 
                  AND strftime('%m', fechaPago) = ?
                """.trimIndent(),
                arrayOf(anioStr, mesStr)
            ).use { c ->
                if (c.moveToFirst()) {
                    cantSocios = c.getInt(0)
                    montoCuotas = c.getDouble(1)
                }
            }

            // ----- Pagos de actividades de NO socios -----
            var cantNoSocios = 0
            var montoActividades = 0.0
            db.rawQuery(
                """
        SELECT COUNT(DISTINCT idCliente) AS cant, IFNULL(SUM(monto),0) AS total
        FROM pagos_actividad
        WHERE strftime('%Y', fecha_pago) = ? 
          AND strftime('%m', fecha_pago) = ?
        """.trimIndent(),
                arrayOf(anioStr, mesStr)
            ).use { c ->
                if (c.moveToFirst()) {
                    cantNoSocios = c.getInt(0)
                    montoActividades = c.getDouble(1)
                }
            }

            val totalClientes = cantSocios + cantNoSocios
            val ingresosTotales = montoCuotas + montoActividades

            return ResumenPagosMes(
                anio = anio,
                mes = mes,
                cantNoSocios = cantNoSocios,
                cantSocios = cantSocios,
                totalClientes = totalClientes,
                montoCuotas = montoCuotas,
                montoActividades = montoActividades,
                ingresosTotales = ingresosTotales
            )
        }

    // ----------------------------------------- CREATE -----------------------------------------
    fun hacerSocioDesdeNoSocio(
        dni: Int,
        formaPago: String?,
        fechaPago: String // "YYYY-MM-DD"
    ): Int? {
        val configuration = obtenerConfiguracionClub()
        val fechaVenc = PaymentDbRules.cuotaVencimiento(
            fechaPago,
            configuration.dueDay,
            configuration.graceDays
        )
        val db = writableDatabase
        db.beginTransaction()

        try {
            val cliente = obtenerPersonaPorDni(dni.toString())
                ?: throw IllegalArgumentException("No existe un cliente con ese DNI")
            if (cliente.esSocio) {
                throw IllegalStateException("El cliente ya es socio")
            }
            val cobro = calcularCobroCuota(configuration, cliente, LocalDate.parse(fechaPago))
            val formaRegistrada = PaymentDbRules.recordedPaymentMethod(configuration, formaPago, cobro.total)

            val cvUpdate = ContentValues().apply {
                put("esSocio", 1)
                put("activo", 1)
                put("carnet", 1)
            }
            db.update("clientes", cvUpdate, "dni = ?", arrayOf(dni.toString()))

            val idCliente = cliente.id
            val cvCuota = ContentValues().apply {
                put("idCliente", idCliente)
                putCharge(cobro)
                put("fechaPago", fechaPago)
                put("formaPago", formaRegistrada)
                put("estadoDelPago", PaymentDbRules.cuotaEstadoPagado())
                put("fechaVencimiento", fechaVenc)
            }
            db.insertOrThrow("cuotas", null, cvCuota)
            db.setTransactionSuccessful()
            return idCliente
        } finally {
            db.endTransaction()
        }
    }
    fun insertarHorario(
        actividadId: Long,
        profesorDni: String,
        dia: Int,
        horaInicio: Int,
        horaFin: Int,
    ): Long {
        val db = writableDatabase
        db.beginTransaction()
        try {
            // Verificar que exista el profesor por DNI
            db.rawQuery("SELECT 1 FROM profesores WHERE dni=?", arrayOf(profesorDni)).use { c ->
                if (!c.moveToFirst()) throw IllegalArgumentException("Profesor no encontrado")
            }

            // Obtener o crear la dupla (actividad, profesor)
            var apId = -1L
            var apActivo = 1
            db.rawQuery(
                "SELECT id, COALESCE(activo,1) AS activo FROM actividad_profesor WHERE actividad_id=? AND profesor_dni=?",
                arrayOf(actividadId.toString(), profesorDni)
            ).use { c ->
                if (c.moveToFirst()) {
                    apId = c.getLong(0)
                    apActivo = c.getInt(1)
                }
            }

            // Si existe, y la columnaactivo esta en 0, convertir a 1
            if (apId != -1L) {
                // Existe: si estaba inactiva, reactivarla (y limpiar bajas si las tenés)
                if ( apActivo ==0) {
                    val cv = ContentValues().apply {
                        put("activo", 1)
                        putNull("fecha_baja")
                        putNull("motivo_baja")
                    }
                    db.update("actividad_profesor", cv, "id=?", arrayOf(apId.toString()))
                }
            }

            // Si no existe, crearlo
            if (apId == -1L) {
                val cvRel = ContentValues().apply {
                    put("actividad_id", actividadId)
                    put("profesor_dni", profesorDni)
                    put("activo", 1)
                }
                val ins = db.insertWithOnConflict(
                    "actividad_profesor",
                    null,
                    cvRel,
                    SQLiteDatabase.CONFLICT_IGNORE
                )
                apId = if (ins != -1L) ins else db.rawQuery(
                    "SELECT id FROM actividad_profesor WHERE actividad_id=? AND profesor_dni=?",
                    arrayOf(actividadId.toString(), profesorDni)
                )
                    .use { c -> if (c.moveToFirst()) c.getLong(0) else throw IllegalStateException("No se pudo resolver actividad_profesor_id") }
            }

            // Insertar día/horario
            validarRangoHorario(horaInicio, horaFin)
            if (profesorTieneSolapamiento(db, profesorDni, dia, horaInicio, horaFin)) {
                throw IllegalArgumentException("El profesor ya tiene un horario activo en ese rango")
            }

            val cvHorario = ContentValues().apply {
                put("actividad_profesor_id", apId)
                put("dia", dia)
                put("hora_inicio", horaInicio)
                put("hora_fin", horaFin)
            }
            val rowId = db.insertWithOnConflict(
                "dias_horarios",
                null,
                cvHorario,
                SQLiteDatabase.CONFLICT_IGNORE
            )
            db.setTransactionSuccessful()
            return rowId
        } finally {
            db.endTransaction()
        }
    }
    fun registrarPagoCuota(
        dni: String,
        formaPago: String?,
        fechaPago: String = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    ): Long {
        val configuration = obtenerConfiguracionClub()
        val cliente = obtenerPersonaPorDni(dni)
            ?: throw IllegalArgumentException(PaymentDbRules.activeClientMissing)
        val cobro = calcularCobroCuota(configuration, cliente, LocalDate.parse(fechaPago))
        val formaRegistrada = PaymentDbRules.recordedPaymentMethod(configuration, formaPago, cobro.total)
        val fechaVenc = PaymentDbRules.cuotaVencimiento(
            fechaPago,
            configuration.dueDay,
            configuration.graceDays
        )
        val cv = ContentValues().apply {
            put("idCliente", cliente.id)
            putCharge(cobro)
            put("fechaPago", fechaPago)
            put("formaPago", formaRegistrada)
            put("estadoDelPago", PaymentDbRules.cuotaEstadoPagado())
            put("fechaVencimiento", fechaVenc)
        }
        return writableDatabase.insert("cuotas", null, cv)
    }

    fun calcularCobroCuota(dni: String, fechaPago: LocalDate = LocalDate.now()): ChargeBreakdown {
        val cliente = obtenerPersonaPorDni(dni)
            ?: throw IllegalArgumentException(PaymentDbRules.activeClientMissing)
        return calcularCobroCuota(obtenerConfiguracionClub(), cliente, fechaPago)
    }

    private fun calcularCobroCuota(
        configuration: ClubConfiguration,
        cliente: PersonaDTO,
        fechaPago: LocalDate
    ): ChargeBreakdown {
        val idCliente = requireNotNull(cliente.id) { PaymentDbRules.activeClientMissing }
        return ChargeCalculator.calculate(
            base = configuration.monthlyFee,
            kind = ChargeKind.FEE,
            benefits = obtenerBeneficiosCliente(idCliente),
            rules = configuration.billingRules,
            currency = configuration.currency,
            paymentDate = fechaPago,
            dueDate = if (cliente.esSocio) ultimoVencimientoCuota(idCliente) else null
        )
    }

    private fun ultimoVencimientoCuota(idCliente: Int): LocalDate? =
        readableDatabase.rawQuery(
            """
                SELECT fechaVencimiento
                FROM cuotas
                WHERE idCliente = ?
                ORDER BY fechaPago DESC, idCuota DESC
                LIMIT 1
            """.trimIndent(),
            arrayOf(idCliente.toString())
        ).use { cursor ->
            if (cursor.moveToFirst() && !cursor.isNull(0)) {
                runCatching { LocalDate.parse(cursor.getString(0)) }.getOrNull()
            } else {
                null
            }
        }

    fun calcularCobroActividad(
        idCliente: Int,
        horarioId: Int,
        fecha: LocalDate = LocalDate.now()
    ): ChargeBreakdown? {
        val price = obtenerPrecioHorario(horarioId) ?: return null
        val configuration = obtenerConfiguracionClub()
        return ChargeCalculator.calculate(
            base = price,
            kind = ChargeKind.ACTIVITY,
            benefits = obtenerBeneficiosCliente(idCliente),
            rules = configuration.billingRules,
            currency = configuration.currency,
            paymentDate = fecha
        )
    }

    private fun ContentValues.putCharge(cobro: ChargeBreakdown) {
        put("monto", cobro.total)
        put("monto_base", cobro.base)
        put("descuento", cobro.benefitsAmount)
        put("recargo", cobro.lateFee)
        putOrNull("detalle_ajustes", ChargeBreakdownFormatter.summary(cobro))
    }

    fun obtenerPrecioHorario(horarioId: Int): Double? {
        readableDatabase.rawQuery(
            """
                SELECT a.precio
                FROM dias_horarios dh
                JOIN actividad_profesor ap ON ap.id = dh.actividad_profesor_id
                JOIN actividades a ON a.id_actividad = ap.actividad_id
                WHERE dh.id = ? AND dh.activo = 1
            """.trimIndent(),
            arrayOf(horarioId.toString())
        ).use { cursor ->
            return if (cursor.moveToFirst()) cursor.getDouble(0) else null
        }
    }

    fun registrarPagoActividadNoSocio(
        idCliente: String,
        horarioId: Int,
        medioPago: String?,
        fechaIso: String = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    ): Long {
        val configuration = obtenerConfiguracionClub()
        val price = obtenerPrecioHorario(horarioId)
            ?: throw IllegalArgumentException("No existe un horario activo para registrar el pago")
        require(price > 0.0 && price.isFinite()) { "La actividad debe tener un precio valido" }
        val cobro = ChargeCalculator.calculate(
            base = price,
            kind = ChargeKind.ACTIVITY,
            benefits = idCliente.toIntOrNull()?.let(::obtenerBeneficiosCliente) ?: ClientBenefits.NONE,
            rules = configuration.billingRules,
            currency = configuration.currency,
            paymentDate = LocalDate.parse(fechaIso)
        )
        val formaRegistrada = PaymentDbRules.recordedPaymentMethod(configuration, medioPago, cobro.total)

        val cv = ContentValues().apply {
            put("idCliente", idCliente)
            put("id_actividad", horarioId)
            putCharge(cobro)
            put("forma_pago", formaRegistrada)
            put("fecha_pago", fechaIso)
        }
        return writableDatabase.insert("pagos_actividad", null, cv)
    }
    // ----------------------------------------- Profesores y Actividades (CRUD) -----------------------------------------
    fun insertarProfesor(p: Profesor): Long {
        val cv = ContentValues().apply {
            put("dni", p.dni)
            put("nombre", p.nombre)
            put("apellido", p.apellido)
            put("fecha_nac", p.fechaNac)
            put("telefono", p.telefono)
            put("direccion", p.direccion)
            put("fecha_inscripcion", p.fechaInscripcion)
            put("ficha_medica", if (p.fichaMedica) 1 else 0)
            put("email", p.email)
            put("activo", if (p.activo) 1 else 0)
            put("titulo", p.titulo)
        }
        return writableDatabase.insert("profesores", null, cv)
    }

    fun actualizarProfesor(p: Profesor): Int {
        val cv = ContentValues().apply {
            put("nombre", p.nombre)
            put("apellido", p.apellido)
            put("fecha_nac", p.fechaNac)
            put("telefono", p.telefono)
            put("direccion", p.direccion)
            put("fecha_inscripcion", p.fechaInscripcion)
            put("ficha_medica", if (p.fichaMedica) 1 else 0)
            put("email", p.email)
            put("activo", if (p.activo) 1 else 0)
            put("titulo", p.titulo)
        }
        return writableDatabase.update("profesores", cv, "dni = ?", arrayOf(p.dni))
    }

    fun obtenerProfesor(dni: String): Profesor? =
        obtenerProfesores().firstOrNull { it.dni == dni }

    fun obtenerProfesores(): List<Profesor> {
        val list = mutableListOf<Profesor>()
        readableDatabase.rawQuery("SELECT * FROM profesores ORDER BY nombre ASC", null).use { c ->
            while (c.moveToNext()) {
                list.add(
                    Profesor(
                        dni = c.getString(c.getColumnIndexOrThrow("dni")),
                        nombre = c.getString(c.getColumnIndexOrThrow("nombre")),
                        apellido = c.getString(c.getColumnIndexOrThrow("apellido")),
                        fechaNac = c.getString(c.getColumnIndexOrThrow("fecha_nac")),
                        telefono = c.getString(c.getColumnIndexOrThrow("telefono")),
                        direccion = c.getString(c.getColumnIndexOrThrow("direccion")),
                        fechaInscripcion = c.getString(c.getColumnIndexOrThrow("fecha_inscripcion")),
                        fichaMedica = c.getInt(c.getColumnIndexOrThrow("ficha_medica")) == 1,
                        email = c.getString(c.getColumnIndexOrThrow("email")),
                        activo = c.getInt(c.getColumnIndexOrThrow("activo")) == 1,
                        titulo = c.getStringOrNull("titulo")
                    )
                )
            }
        }
        return list
    }

    fun darDeBajaProfesor(dni: String): Boolean {
        val cv = ContentValues().apply { put("activo", 0) }
        return writableDatabase.update("profesores", cv, "dni = ?", arrayOf(dni)) > 0
    }

    fun insertarCatalogoActividad(a: CatalogoActividad): Long {
        val cv = ContentValues().apply {
            put("nombre", a.nombre)
            put("precio", a.precio)
        }
        return writableDatabase.insert("actividades", null, cv)
    }

    fun actualizarCatalogoActividad(a: CatalogoActividad): Int {
        val cv = ContentValues().apply {
            put("nombre", a.nombre)
            put("precio", a.precio)
        }
        return writableDatabase.update("actividades", cv, "id_actividad = ?", arrayOf(a.id.toString()))
    }

    fun obtenerCatalogoActividad(id: Long): CatalogoActividad? =
        obtenerCatalogoActividades().firstOrNull { it.id == id }

    fun obtenerCatalogoActividades(): List<CatalogoActividad> {
        val list = mutableListOf<CatalogoActividad>()
        readableDatabase.rawQuery("SELECT * FROM actividades ORDER BY nombre ASC", null).use { c ->
            while (c.moveToNext()) {
                list.add(
                    CatalogoActividad(
                        id = c.getLong(c.getColumnIndexOrThrow("id_actividad")),
                        nombre = c.getString(c.getColumnIndexOrThrow("nombre")),
                        precio = c.getDouble(c.getColumnIndexOrThrow("precio"))
                    )
                )
            }
        }
        return list
    }

    fun eliminarCatalogoActividad(id: Long): Boolean {
        val assignments = android.database.DatabaseUtils.longForQuery(
            readableDatabase,
            "SELECT COUNT(*) FROM actividad_profesor WHERE actividad_id = ?",
            arrayOf(id.toString())
        )
        if (assignments > 0L) return false
        return writableDatabase.delete(
            "actividades",
            "id_actividad = ?",
            arrayOf(id.toString())
        ) > 0
    }

    // ----------------------------------------- Descuentos y beneficios -----------------------------------------
    fun insertarDescuento(discount: Discount): Long =
        writableDatabase.insert("descuentos", null, discountValues(discount))

    fun actualizarDescuento(discount: Discount): Int =
        writableDatabase.update(
            "descuentos",
            discountValues(discount),
            "id_descuento = ?",
            arrayOf(discount.id.toString())
        )

    fun obtenerDescuento(id: Long): Discount? =
        obtenerDescuentos().firstOrNull { it.id == id }

    fun obtenerDescuentos(soloActivos: Boolean = false): List<Discount> {
        val where = if (soloActivos) "WHERE activo = 1" else ""
        readableDatabase.rawQuery(
            "SELECT id_descuento, nombre, tipo, valor, alcance, activo FROM descuentos $where ORDER BY nombre ASC",
            null
        ).use { cursor ->
            return generateSequence { if (cursor.moveToNext()) cursor.toDiscount(0) else null }.toList()
        }
    }

    fun eliminarDescuento(id: Long): Boolean {
        val assignments = android.database.DatabaseUtils.longForQuery(
            readableDatabase,
            "SELECT COUNT(*) FROM cliente_beneficios WHERE id_descuento = ?",
            arrayOf(id.toString())
        )
        if (assignments > 0L) return false
        return writableDatabase.delete("descuentos", "id_descuento = ?", arrayOf(id.toString())) > 0
    }

    fun obtenerBeneficiosCliente(idCliente: Int): ClientBenefits {
        readableDatabase.rawQuery(
            """
                SELECT d.id_descuento, d.nombre, d.tipo, d.valor, d.alcance, d.activo,
                       b.beca_porcentaje, b.beca_alcance, b.beca_motivo, b.beca_hasta
                FROM cliente_beneficios b
                LEFT JOIN descuentos d ON d.id_descuento = b.id_descuento
                WHERE b.id_cliente = ?
            """.trimIndent(),
            arrayOf(idCliente.toString())
        ).use { cursor ->
            if (!cursor.moveToFirst()) return ClientBenefits.NONE
            val discount = if (cursor.isNull(0)) null else cursor.toDiscount(0)
            val scholarship = if (cursor.isNull(6)) {
                null
            } else {
                Scholarship(
                    percent = cursor.getInt(6),
                    scope = ChargeScope.fromCode(cursor.getString(7)),
                    reason = cursor.getString(8).orEmpty(),
                    until = if (cursor.isNull(9)) null else LocalDate.parse(cursor.getString(9))
                )
            }
            return ClientBenefits(discount, scholarship)
        }
    }

    fun guardarBeneficiosCliente(idCliente: Int, discountId: Long?, scholarship: Scholarship?): Boolean {
        if (discountId == null && scholarship == null) {
            writableDatabase.delete("cliente_beneficios", "id_cliente = ?", arrayOf(idCliente.toString()))
            return true
        }
        val values = ContentValues().apply {
            put("id_cliente", idCliente)
            if (discountId == null) putNull("id_descuento") else put("id_descuento", discountId)
            if (scholarship == null) {
                putNull("beca_porcentaje")
                putNull("beca_alcance")
                putNull("beca_motivo")
                putNull("beca_hasta")
            } else {
                put("beca_porcentaje", scholarship.percent)
                put("beca_alcance", scholarship.scope.code)
                put("beca_motivo", scholarship.reason)
                putOrNull("beca_hasta", scholarship.until?.toString())
            }
        }
        return writableDatabase.insertWithOnConflict(
            "cliente_beneficios",
            null,
            values,
            SQLiteDatabase.CONFLICT_REPLACE
        ) != -1L
    }

    private fun discountValues(discount: Discount) = ContentValues().apply {
        put("nombre", discount.name)
        put("tipo", discount.type.code)
        put("valor", discount.value)
        put("alcance", discount.scope.code)
        put("activo", if (discount.active) 1 else 0)
    }

    private fun Cursor.toDiscount(offset: Int) = Discount(
        id = getLong(offset),
        name = getString(offset + 1),
        type = AdjustmentType.fromCode(getString(offset + 2)),
        value = getDouble(offset + 3),
        scope = ChargeScope.fromCode(getString(offset + 4)),
        active = getInt(offset + 5) == 1
    )

    // ----------------------------------------- Delete -----------------------------------------
    // Borrado logico del padrón para evitar conflicto con tabla de pagos
    fun darDeBajaHorario(dhId: Int, motivo: String? = null): Boolean {
        val db = writableDatabase
        db.beginTransaction()
        return try {
            // Traer apId por si luego marcamos la relación inactiva
            val apId = android.database.DatabaseUtils.longForQuery(
                db, "SELECT actividad_profesor_id FROM dias_horarios WHERE id=?",
                arrayOf(dhId.toString())
            )

            val hoy = java.time.LocalDate.now().toString() // "YYYY-MM-DD"
            val cv = ContentValues().apply {
                put("activo", 0)
                put("fecha_baja", hoy)
                if (motivo != null) put("motivo_baja", motivo)
            }

            val rows = db.update("dias_horarios", cv, "id=?", arrayOf(dhId.toString()))
            if (rows == 0) { db.endTransaction(); return false }

            // Si esa relación ya no tiene horarios activos, marcamos la relación como inactiva (no se borra)
            val quedanActivos = android.database.DatabaseUtils.longForQuery(
                db,
                "SELECT COUNT(*) FROM dias_horarios WHERE actividad_profesor_id=? AND activo=1",
                arrayOf(apId.toString())
            )
            if (quedanActivos == 0L) {
                val cvAp = ContentValues().apply {
                    put("activo", 0)
                    put("fecha_baja", hoy)
                    if (motivo != null) put("motivo_baja", motivo)
                }
                db.update("actividad_profesor", cvAp, "id=?", arrayOf(apId.toString()))
            }
            db.setTransactionSuccessful()
            true
        } finally { db.endTransaction() }
    }
    fun eliminarPersonaPorId(id: String): Boolean {
            val db = this.writableDatabase
            db.beginTransaction()
            try {
                // Verificar si existe el cliente
                val idCliente = db.rawQuery(
                    "SELECT id FROM clientes WHERE id = ?",
                    arrayOf(id)
                ).use { c ->
                    if (c.moveToFirst()) c.getLong(0) else null
                }

                if (idCliente == null) return false

                // Borrado lógico
                val cv = ContentValues().apply {
                    put("activo", 0)
                    put("carnet", 0)
                    put("esSocio", 0)
                }

                val rows = db.update(
                    "clientes",
                    cv,
                    "id = ?",
                    arrayOf(idCliente.toString())
                )

                db.setTransactionSuccessful()
                return rows > 0

            } finally {
                db.endTransaction()
                db.close()
            }
        }

    // ----------------------------------------- Update -----------------------------------------
    // Horarios
    fun actualizarHorarioPorId(
        idDiaHorario: Int,
        dia: Int,
        horaInicio: Int,
        horaFin: Int,
    ): Boolean {
        val db = writableDatabase
        validarRangoHorario(horaInicio, horaFin)
        val profesorDni = profesorDniPorHorario(db, idDiaHorario)
            ?: throw IllegalArgumentException("Horario no encontrado")
        if (profesorTieneSolapamiento(db, profesorDni, dia, horaInicio, horaFin, idDiaHorario)) {
            throw IllegalArgumentException("El profesor ya tiene un horario activo en ese rango")
        }

        val cv = ContentValues().apply {
            put("dia", dia)
            put("hora_inicio", horaInicio)
            put("hora_fin", horaFin)
        }
        val rows = db.update("dias_horarios", cv, "id = ?", arrayOf(idDiaHorario.toString()))
        return rows > 0
    }


    private fun validarRangoHorario(horaInicio: Int, horaFin: Int) {
        if (!ScheduleOverlapValidator.isValidRange(horaInicio, horaFin)) {
            throw IllegalArgumentException("El horario de fin debe ser mayor al de inicio")
        }
    }

    private fun profesorDniPorHorario(db: SQLiteDatabase, idDiaHorario: Int): String? {
        val sql = """
            SELECT ap.profesor_dni
            FROM dias_horarios dh
            JOIN actividad_profesor ap ON ap.id = dh.actividad_profesor_id
            WHERE dh.id = ?
        """.trimIndent()
        db.rawQuery(sql, arrayOf(idDiaHorario.toString())).use { c ->
            return if (c.moveToFirst()) c.getString(0) else null
        }
    }

    private fun profesorTieneSolapamiento(
        db: SQLiteDatabase,
        profesorDni: String,
        dia: Int,
        horaInicio: Int,
        horaFin: Int,
        excluirDiaHorarioId: Int? = null
    ): Boolean {
        val args = mutableListOf(profesorDni, dia.toString())
        val excluirClause = if (excluirDiaHorarioId != null) {
            args += excluirDiaHorarioId.toString()
            "AND dh.id <> ?"
        } else {
            ""
        }
        val sql = """
            SELECT dh.hora_inicio, dh.hora_fin
            FROM dias_horarios dh
            JOIN actividad_profesor ap ON ap.id = dh.actividad_profesor_id
            WHERE ap.profesor_dni = ?
              AND dh.dia = ?
              AND COALESCE(dh.activo, 1) = 1
              $excluirClause
        """.trimIndent()
        db.rawQuery(sql, args.toTypedArray()).use { c ->
            while (c.moveToNext()) {
                if (ScheduleOverlapValidator.overlaps(horaInicio, horaFin, c.getInt(0), c.getInt(1))) {
                    return true
                }
            }
        }
        return false
    }

    // Clientes
    fun actualizarClientePorId(
        id: Int,
        dni: String,
        nombre: String,
        apellido: String,
        fechaNac: String,
        telefono: String?,
        direccion: String?,
        email: String?
    ): Boolean {
        val db = this.writableDatabase
        db.beginTransaction()
        try {
            // Opcional: verificar que exista el cliente
            val existe = db.rawQuery(
                "SELECT id FROM clientes WHERE id = ?",
                arrayOf(id.toString())
            ).use { c ->
                c.moveToFirst()
            }

            if (!existe) return false

            val cv = ContentValues().apply {
                put("nombre", nombre)
                put("apellido", apellido)
                put("dni", dni)
                put("fecha_nac", fechaNac)
                put("telefono", telefono)
                put("direccion", direccion)
                put("email", email)
            }

            val rows = db.update(
                "clientes",
                cv,
                "id = ?",
                arrayOf(id.toString())
            )

            db.setTransactionSuccessful()
            return rows > 0
        } finally {
            db.endTransaction()
            db.close()
        }
    }

    // Importacion CSV
    fun obtenerIdentidadesClientes(): IdentidadesClientes {
        val dnis = mutableSetOf<String>()
        val emails = mutableSetOf<String>()
        readableDatabase.rawQuery("SELECT dni, email FROM clientes", null).use { c ->
            while (c.moveToNext()) {
                dnis.add(c.getString(0))
                c.getStringOrNull("email")?.let { emails.add(it.trim().lowercase()) }
            }
        }
        return IdentidadesClientes(dnis, emails)
    }

    // Todo o nada: si una fila falla no queda una importacion a medias.
    fun importarClientes(clientes: List<ClientCsvImport.Client>, fechaInscripcion: String): Int {
        val db = writableDatabase
        db.beginTransaction()
        try {
            clientes.forEach { cliente ->
                val cv = ContentValues().apply {
                    put("dni", cliente.dni)
                    put("nombre", cliente.name)
                    put("apellido", cliente.lastName)
                    put("fecha_nac", cliente.birthDate)
                    put("telefono", cliente.phone)
                    put("direccion", cliente.address)
                    put("email", cliente.email)
                    put("fecha_inscripcion", fechaInscripcion)
                    put("ficha_medica", 1)
                    put("esSocio", 0)
                    put("activo", 1)
                    put("carnet", 0)
                }
                db.insertOrThrow("clientes", null, cv)
            }
            db.setTransactionSuccessful()
            return clientes.size
        } finally {
            db.endTransaction()
        }
    }

    // ----------------------------------------- Utilidades -----------------------------------------
    // Modelos de datos
    data class IdentidadesClientes(
        val dnis: Set<String>,
        val emails: Set<String>
    )
    data class NoSocioCard(
        val nombre: String,
        val apellido: String,
        val dni: String,
        val ultimaPago: String?,
        val nombreAct: String?
    )
    data class VencimientoCard(
        val nombre: String,
        val apellido: String,
        val dni: String,
        val fechaVenc: String,
        val ultimoPago: String?
    )
    data class ResumenVencimientos(
        val alDia: Int,
        val porVencer: Int,
        val vencidos: Int
    )
    data class SocioCard(
        val nombre: String,
        val apellido: String,
        val dni: String,
        val ultimoPago: String?
    )
    data class ActividadCard(
            val idActividad: Int,
            val idDiaHorario: Int,   // ← dh.id para eliminar
            val nombre: String,
            val precio: Double,
            val profesor: String,
            val dia: Int,
            val horaInicio: Int,     // en minutos
            val horaFin: Int,        // en minutos
            val etiquetaHorario: String // "Lun 08:00-09:00"
    )
    data class PersonaDTO(
        val id: Int?,
        val dni: String,
        val nombre: String?,
        val apellido: String?,
        val telefono: String?,
        val direccion: String?,
        val email: String?,
        val fecha_nac: String?,
        val fichaMedica: String?,
        val esSocio: Boolean,
        )
    data class MetricasInicio(
        val sociosActivos: Int,
        val noSociosActivos: Int,
        val vencidos: Int,
        val ingresosMes: Double,
        val actividadesHoy: Int
    )
    data class CuentaCorrienteDTO(
        val estado: String,
        val detalleEstado: String,
        val ultimoPagoCuota: String?,
        val proximoVencimiento: String?,
        val ultimoPagoActividad: String?,
        val totalCuotas: Double,
        val totalActividades: Double,
        val deudaEstimada: Double,
        val movimientos: List<MovimientoCuenta>
    )
    data class MovimientoCuenta(
        val tipo: String,
        val fecha: String,
        val monto: Double,
        val detalle: String,
        val ajustes: String? = null
    )
    data class ResumenPagosMes(
        val anio: Int,
        val mes: Int,
        val cantNoSocios: Int,
        val cantSocios: Int,
        val totalClientes: Int,
        val montoCuotas: Double,
        val montoActividades: Double,
        val ingresosTotales: Double
    )

    data class Profesor(
        val dni: String,
        val nombre: String,
        val apellido: String,
        val fechaNac: String,
        val telefono: String,
        val direccion: String,
        val fechaInscripcion: String,
        val fichaMedica: Boolean,
        val email: String,
        val activo: Boolean,
        val titulo: String?
    )

    data class CatalogoActividad(
        val id: Long,
        val nombre: String,
        val precio: Double
    )

    // Herramientas
    private fun Cursor.getStringOrNull(col: String): String? {
        val idx = getColumnIndex(col)
        return if (idx >= 0 && !isNull(idx)) getString(idx) else null
    }
    private fun ContentValues.putOrNull(key: String, value: String?) {
        if (value == null) putNull(key) else put(key, value)
    }
    private fun existeConDni(table: String, dni: String): Boolean =
        readableDatabase.query(table, arrayOf("dni"), "dni = ?", arrayOf(dni), null, null, null)
            .use { it.moveToFirst() }
    private fun etiquetaDia(dia: Int) = ClubFormatters.etiquetaDia(dia)
    private fun hhmm(mins: Int) = ClubFormatters.hhmm(mins)
}
