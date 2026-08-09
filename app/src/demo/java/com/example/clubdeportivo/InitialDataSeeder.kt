package com.example.clubdeportivo

import android.database.sqlite.SQLiteDatabase

internal object InitialDataSeeder {
    fun seed(db: SQLiteDatabase) {
        db.beginTransaction()
        try {
            // --------- ACTIVIDADES ---------
            db.execSQL("""
                INSERT OR IGNORE INTO actividades (nombre, precio) VALUES
                ('Fútbol', 8000.00),
                ('Básquet', 8500.00),
                ('Vóley', 7000.00),
                ('Yoga', 6500.00),
                ('CrossFit', 9500.00),
                ('Funcional', 6000.00),
                ('GAP', 5000.00),
                ('Natación Adultos', 9000.00);
                """.trimIndent())

            // --------- PROFESORES ---------
            db.execSQL("""
                INSERT OR IGNORE INTO profesores
                (dni, nombre, apellido, fecha_nac, telefono, direccion, fecha_inscripcion, ficha_medica, email, activo, titulo) VALUES
                ('20123456','Juan','Pérez','1988-04-12','3415551111','San Martín 123, Rosario','2025-01-10',1,'juan.perez@club.com',1,'Prof. Ed. Física'),
                ('22333444','María','Giménez','1990-09-02','3415552222','Mendoza 456, Rosario','2025-01-15',1,'maria.gimenez@club.com',1,'Instructora de Yoga'),
                ('27999888','Diego','Sosa','1985-07-22','3415553333','Sarmiento 789, Rosario','2025-01-20',1,'diego.sosa@club.com',1,'Entrenador de Fútbol'),
                ('25444777','Lucía','Benítez','1992-03-18','3415554444','Oroño 321, Rosario','2025-01-22',1,'lucia.benitez@club.com',1,'Entrenadora de Natación'),
                ('23111222','Agustín','Rossi','1987-11-05','3415555555','Italia 999, Rosario','2025-01-25',1,'agustin.rossi@club.com',1,'Coach CrossFit'),
                ('20888999','Sofía','Almada','1991-12-01','3415556666','Córdoba 1500, Rosario','2025-02-01',1,'sofia.almada@club.com',1,'Prof. Vóley');
                """.trimIndent()
            )
            // --------- CLIENTES (SOCIOS) ---------
            db.execSQL("""
                INSERT OR IGNORE INTO clientes
                (id, nombre, apellido, dni, fecha_nac, telefono, direccion, fecha_inscripcion, ficha_medica, email, activo, carnet, esSocio) VALUES
                (1,'Pablo','Álvarez','40111111','1993-02-15','3415557001','San Luis 101, Rosario',date('now','-4 months'),1,'p.alvarez@club.com',1,1,1),
                (2,'Mariana','Cabral','40222222','1991-07-09','3415557002','Santiago 220, Rosario',date('now','-2 months'),1,'m.cabral@club.com',1,1,1),
                (3,'Diego','Ortiz','40333333','1989-11-20','3415557003','Pellegrini 1500, Rosario',date('now','-6 months'),1,'d.ortiz@club.com',1,1,1),
                (4,'Lucía','Funes','40444444','1995-03-03','3415557004','Riobamba 800, Rosario',date('now','-8 months'),1,'l.funes@club.com',1,1,1),
                (6,'Carla','Vega','40666666','1992-12-12','3415557006','Mitre 200, Rosario',date('now','-3 months'),1,'c.vega@club.com',1,1,1),
                (7,'Sofía','Ramos','40777777','1990-09-17','3415557007','Salta 900, Rosario',date('now','-10 months'),1,'s.ramos@club.com',1,1,1),
                (8,'Hernán','Molina','40888888','1994-01-30','3415557008','España 1200, Rosario',date('now','-1 months'),1,'h.molina@club.com',1,1,1);
                """.trimIndent())

            // --------- CLIENTES (NO SOCIOS) ---------
            db.execSQL("""
                INSERT OR IGNORE INTO clientes
                (nombre, apellido, dni, fecha_nac, telefono, email, direccion, fecha_inscripcion, ficha_medica, activo, carnet, esSocio) VALUES
                ('Carlos','Ruiz','33111222','1999-05-10','3416000001','carlos.ruiz@gmail.com','Mitre 120, Rosario','2025-03-01',1,1,0,0),
                ('Ana','Martínez','30999888','2001-11-23','3416000002','ana.martinez@gmail.com','Belgrano 450, Rosario','2025-03-02',1,1,0,0),
                ('Matías','Ojeda','28123456','1995-08-14','3416000003','matias.ojeda@gmail.com','Dorrego 980, Rosario','2025-03-03',1,1,0,0),
                ('Camila','Lopez','32123456','2000-02-28','3416000004','camila.lopez@gmail.com','Tucumán 2100, Rosario','2025-03-04',1,1,0,0),
                ('Bruno','Ferreyra','34123456','1998-07-07','3416000005','bruno.ferreyra@gmail.com','Paraguay 300, Rosario','2025-03-05',1,1,0,0),
                ('Valentina','Suárez','35123456','2002-09-19','3416000006','valentina.suarez@gmail.com','Catamarca 750, Rosario','2025-03-06',1,1,0,0),
                ('Ezequiel','Páez','36123456','1997-01-30','3416000007','eze.paez@gmail.com','Urquiza 210, Rosario','2025-03-07',1,1,0,0),
                ('Julieta','Bianchi','37123456','2003-04-22','3416000008','julieta.bianchi@gmail.com','Salta 1750, Rosario','2025-03-08',1,1,0,0);
                 """.trimIndent())

            // --------- CUOTAS ---------
            // Cliente 1: AL DÍA (última cuota paga hoy, vence el mes que viene)
            db.execSQL("""
                INSERT OR IGNORE INTO cuotas (idCliente, monto, fechaPago, formaPago, estadoDelPago, fechaVencimiento) VALUES
                (1, 30000, date('now','-2 months'), 'Efectivo', 1, date('now','-1 months')),
                (1, 30000, date('now','-1 months'), 'Efectivo', 1, date('now')),
                (1, 30000, date('now'),            'Efectivo', 1, date('now','+1 months'));
                """.trimIndent())

            // Cliente 2: POR VENCER (faltan < 7 días)
            db.execSQL("""
                INSERT OR IGNORE INTO cuotas (idCliente, monto, fechaPago, formaPago, estadoDelPago, fechaVencimiento) VALUES
                (2, 30000, date('now','-25 days'), 'Transferencia', 1, date('now','+5 days'));
                """.trimIndent())

            // Cliente 3: VENCIDO hace 10 días
            db.execSQL("""
                INSERT OR IGNORE INTO cuotas (idCliente, monto, fechaPago, formaPago, estadoDelPago, fechaVencimiento) VALUES
                (3, 30000, date('now','-40 days'), 'Tarjeta', 1, date('now','-10 days'));
                """.trimIndent())

            // Cliente 4: VENCIDO hace 40 días
            db.execSQL("""
                INSERT OR IGNORE INTO cuotas (idCliente, monto, fechaPago, formaPago, estadoDelPago, fechaVencimiento) VALUES
                (4, 30000, date('now','-70 days'), 'Efectivo', 1, date('now','-40 days'));
                """.trimIndent())

            // Cliente 6: AL DÍA con historial
            db.execSQL("""
                INSERT OR IGNORE INTO cuotas (idCliente, monto, fechaPago, formaPago, estadoDelPago, fechaVencimiento) VALUES
                (6, 30000, date('now','-2 months'), 'Tarjeta', 1, date('now','-1 months')),
                (6, 30000, date('now','-1 months'), 'Tarjeta', 1, date('now'));
                """.trimIndent())

            // Cliente 7: Vencido hace 5 meses
            db.execSQL("""
                INSERT OR IGNORE INTO cuotas (idCliente, monto, fechaPago, formaPago, estadoDelPago, fechaVencimiento) VALUES
                (7, 30000, date('now','-6 months'), 'Efectivo', 1, date('now','-5 months'));
                """.trimIndent())

            // Cliente 8: VENCE HOY
            db.execSQL("""
                INSERT OR IGNORE INTO cuotas (idCliente, monto, fechaPago, formaPago, estadoDelPago, fechaVencimiento) VALUES
                (8, 30000, date('now','-30 days'), 'Transferencia', 1, date('now'));
                """.trimIndent())

            // --------- Actividad_Profesor ---------
            db.execSQL("""
                INSERT OR IGNORE INTO actividad_profesor (actividad_id, profesor_dni, activo) VALUES
                (1, '27999888', 1),  -- Fútbol - Diego Sosa
                (2, '20888999', 1),  -- Básquet - Sofía Almada
                (3, '20888999', 1),  -- Vóley  - Sofía Almada
                (4, '22333444', 1),  -- Yoga   - María Giménez
                (5, '23111222', 1),  -- CrossFit - Agustín Rossi
                (6, '20123456', 1),  -- Funcional - Juan Pérez
                (7, '20123456', 1),  -- GAP - Juan Pérez
                (8, '25444777', 1);  -- Natación Adultos - Lucía Benítez
                """.trimIndent())

            // --------- Horarios ---------

            // Lunes (1) – Fútbol y CrossFit
            db.execSQL("""
                INSERT OR IGNORE INTO dias_horarios (actividad_profesor_id, dia, hora_inicio, hora_fin)
                SELECT id, 1, 1080, 1140   -- 18:00–19:00
                FROM actividad_profesor
                WHERE actividad_id = 1 AND profesor_dni = '27999888';
                """.trimIndent())
            db.execSQL("""
                INSERT OR IGNORE INTO dias_horarios (actividad_profesor_id, dia, hora_inicio, hora_fin)
                SELECT id, 1, 1140, 1200   -- 19:00–20:00
                FROM actividad_profesor
                WHERE actividad_id = 5 AND profesor_dni = '23111222';
                """.trimIndent())

            // Martes (2) – Básquet y Vóley
            db.execSQL("""
                INSERT OR IGNORE INTO dias_horarios (actividad_profesor_id, dia, hora_inicio, hora_fin)
                SELECT id, 2, 1080, 1170   -- 18:00–19:30
                FROM actividad_profesor
                WHERE actividad_id = 2 AND profesor_dni = '20888999';
                """.trimIndent())
            db.execSQL("""
                INSERT OR IGNORE INTO dias_horarios (actividad_profesor_id, dia, hora_inicio, hora_fin)
                SELECT id, 2, 1170, 1260   -- 19:30–21:00
                FROM actividad_profesor
                WHERE actividad_id = 3 AND profesor_dni = '20888999';
                """.trimIndent())

            // Miércoles (3) – Yoga mañana, Funcional tarde
            db.execSQL("""
                INSERT OR IGNORE INTO dias_horarios (actividad_profesor_id, dia, hora_inicio, hora_fin)
                SELECT id, 3, 540, 600     -- 09:00–10:00
                FROM actividad_profesor
                WHERE actividad_id = 4 AND profesor_dni = '22333444';
                """.trimIndent())
            db.execSQL("""
                INSERT OR IGNORE INTO dias_horarios (actividad_profesor_id, dia, hora_inicio, hora_fin)
                SELECT id, 3, 1080, 1140   -- 18:00–19:00
                FROM actividad_profesor
                WHERE actividad_id = 6 AND profesor_dni = '20123456';
                """.trimIndent())

            // Jueves (4) – GAP y Natación
            db.execSQL("""
                INSERT OR IGNORE INTO dias_horarios (actividad_profesor_id, dia, hora_inicio, hora_fin)
                SELECT id, 4, 1020, 1080   -- 17:00–18:00
                FROM actividad_profesor
                WHERE actividad_id = 7 AND profesor_dni = '20123456';
                """.trimIndent())
            db.execSQL("""
                INSERT OR IGNORE INTO dias_horarios (actividad_profesor_id, dia, hora_inicio, hora_fin)
                SELECT id, 4, 1080, 1140   -- 18:00–19:00
                FROM actividad_profesor
                WHERE actividad_id = 8 AND profesor_dni = '25444777';
                """.trimIndent())

            // Viernes (5) – Fútbol y CrossFit
            db.execSQL("""
                INSERT OR IGNORE INTO dias_horarios (actividad_profesor_id, dia, hora_inicio, hora_fin)
                SELECT id, 5, 1080, 1170   -- 18:00–19:30
                FROM actividad_profesor
                WHERE actividad_id = 1 AND profesor_dni = '27999888';
                """.trimIndent())
            db.execSQL("""
                INSERT OR IGNORE INTO dias_horarios (actividad_profesor_id, dia, hora_inicio, hora_fin)
                SELECT id, 5, 1170, 1230   -- 19:30–20:30
                FROM actividad_profesor
                WHERE actividad_id = 5 AND profesor_dni = '23111222';
                """.trimIndent())

            // Sábado (6) – Natación mañana, Yoga tarde
            db.execSQL("""
                INSERT OR IGNORE INTO dias_horarios (actividad_profesor_id, dia, hora_inicio, hora_fin)
                SELECT id, 6, 600, 660     -- 10:00–11:00
                FROM actividad_profesor
                WHERE actividad_id = 8 AND profesor_dni = '25444777';
                """.trimIndent())
            db.execSQL("""
                INSERT OR IGNORE INTO dias_horarios (actividad_profesor_id, dia, hora_inicio, hora_fin)
                SELECT id, 6, 1080, 1140   -- 18:00–19:00
                FROM actividad_profesor
                WHERE actividad_id = 4 AND profesor_dni = '22333444';
                """.trimIndent())
            db.execSQL("""
                INSERT OR IGNORE INTO dias_horarios (actividad_profesor_id, dia, hora_inicio, hora_fin)
                SELECT id, 0, 600, 660     -- 10:00–11:00
                FROM actividad_profesor
                WHERE actividad_id = 6 AND profesor_dni = '20123456';
                """.trimIndent())
            db.execSQL("""
                INSERT OR IGNORE INTO dias_horarios (actividad_profesor_id, dia, hora_inicio, hora_fin)
                SELECT id, 0, 1080, 1140   -- 18:00–19:00
                FROM actividad_profesor
                WHERE actividad_id = 3 AND profesor_dni = '20888999';
                """.trimIndent())

            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }
}
