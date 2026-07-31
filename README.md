# Club deportivo

Aplicacion Android nativa para administrar un club deportivo: clientes, socios, no socios, actividades, profesores, horarios, pagos de cuotas, pagos por actividad y resumen mensual de ingresos.

## Stack

- Kotlin
- Android Views con XML
- AppCompat y Material Components
- SQLite local con `SQLiteOpenHelper`
- Gradle Kotlin DSL

Versiones principales:

- Android Gradle Plugin `8.12.3`
- Kotlin `2.0.21`
- Gradle wrapper `8.13`
- `compileSdk` / `targetSdk` `36`
- `minSdk` `30`
- JVM target `11`

## Requisitos

- Android Studio o JDK compatible configurado en `JAVA_HOME`
- Android SDK instalado
- Emulador o dispositivo fisico para pruebas instrumentadas

## Comandos

Desde la raiz del proyecto en Windows:

```powershell
.\gradlew.bat assembleDebug
.\gradlew.bat test
.\gradlew.bat connectedAndroidTest
```

`connectedAndroidTest` requiere un emulador o dispositivo conectado.

## Credenciales de prueba

El login acepta usuarios de prueba donde usuario y contrasena coinciden:

- `admin` / `admin`
- `charlie` / `charlie`
- `sacha` / `sacha`
- `javo` / `javo`
- `heber` / `heber`

La validacion vive en `LoginCredentials.kt` y tiene tests unitarios.

## Estructura

- `app/src/main/java/com/example/clubdeportivo/`: Activities, adapters, helpers y reglas simples.
- `app/src/main/res/layout/`: pantallas XML.
- `app/src/main/res/menu/menu_bottom_nav.xml`: navegacion inferior.
- `app/src/main/res/values/`: colores, strings y temas.
- `app/src/test/`: tests unitarios JVM.
- `app/src/androidTest/`: tests instrumentados de SQLite real y smoke visual.

## Modelo de datos

La base local se crea en `DBHelper.kt`. Tablas principales:

- `clientes`: socios y no socios unificados por `esSocio`, `activo` y `carnet`.
- `actividades`: catalogo de actividades y precios.
- `profesores`: docentes disponibles.
- `actividad_profesor`: relacion entre actividad y profesor.
- `dias_horarios`: horarios activos/inactivos por actividad-profesor.
- `cuotas`: pagos de cuota de socios.
- `pagos_actividad`: pagos puntuales de no socios por horario de actividad.

Convenciones importantes:

- Fechas en formato ISO `yyyy-MM-dd`.
- Dias como enteros `0..6`, donde `0 = Domingo`.
- Horas como minutos desde medianoche.
- Bajas de clientes y horarios como borrado logico.

## Flujos principales

1. Login con usuario de prueba.
2. Inicio muestra fecha, bienvenida y actividades del dia.
3. Actividades permite buscar, agregar, editar y dar de baja horarios.
4. Listados alterna entre no socios, socios y vencimientos.
5. Pago de cuota registra pago o convierte un no socio en socio.
6. Pago de actividad registra pagos puntuales de no socios.
7. Resumen mensual agrupa ingresos por cuotas y actividades.

## Pruebas

Tests unitarios JVM:

- `ClubFormattersTest`: formato de dias, horas y vencimientos.
- `LoginCredentialsTest`: credenciales validas e invalidas.

Tests instrumentados:

- `DBHelperInstrumentedTest`: verifica que el pago de cuota se guarde con `idCliente`, monto, forma de pago y vencimiento correcto.
- `VisualSmokeInstrumentedTest`: abre Login, Inicio y Listados, valida que sus controles principales se muestren y genera capturas con `Screenshot.capture()`.

Para obtener evidencia visual, ejecutar:

```powershell
.\gradlew.bat connectedAndroidTest
```

Luego revisar los artefactos generados por Android Test Orchestrator/runner en el reporte de instrumented tests del modulo `app`.

## Notas para desarrollo

- Mantener la arquitectura actual salvo que se planifique una refactorizacion mayor.
- Evitar introducir Compose, Room, Hilt o Navigation Component sin decision explicita.
- Revisar `AGENTS.md` y `CLAUDE.md` antes de cambios grandes.
- Si se modifica el esquema de SQLite, subir version de DB y definir migracion en `onUpgrade`.
- No asumir que cambios en `onCreate` impactan instalaciones existentes: solo corren al crear la base.
