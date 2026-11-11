# Club deportivo

Aplicación Android nativa para la administración de finanzas y personal en clubes deportivos

El repositorio también incluye la API del servicio multi-club (`backend/`), que se construye por etapas según la [ADR-001](docs/arquitectura/ADR-001-backend-saas.md).

## Stack

- Kotlin
- Android Views con XML
- AppCompat y Material Components
- SQLite local con `SQLiteOpenHelper`
- Gradle Kotlin DSL

Versiones principales:

- Android Gradle Plugin `8.12.3`
- Kotlin `2.3.21`
- Gradle wrapper `8.14.6`
- `compileSdk` / `targetSdk` `36`
- `minSdk` `30`
- JVM target `11` (app y `shared`) y `17` (backend)
- Spring Boot `4.1.2` y PostgreSQL `18` (backend)

## Variantes de la aplicacion

- `demo`: usa `com.example.clubdeportivo.demo`, muestra una etiqueta de demostracion y carga datos ficticios.
- `production`: usa `com.example.clubdeportivo` y crea solamente el esquema vacio.

Los inicializadores viven en source sets separados. Los datos ficticios de `src/demo` no forman parte del codigo de produccion.

Comandos por variante:

```powershell
.\gradlew.bat assembleDemoDebug
.\gradlew.bat assembleProductionDebug
.\gradlew.bat :shared:test
.\gradlew.bat testDemoDebugUnitTest
.\gradlew.bat testProductionDebugUnitTest
.\gradlew.bat connectedDemoDebugAndroidTest
.\gradlew.bat connectedProductionDebugAndroidTest
```

## Requisitos

- Android Studio o JDK compatible configurado en `JAVA_HOME`
- Android SDK instalado
- Emulador o dispositivo fisico para pruebas instrumentadas
- Docker (Docker Desktop en Windows) para correr el backend y sus tests

## Comandos

Desde la raiz del proyecto en Windows:

```powershell
.\gradlew.bat assembleDebug
.\gradlew.bat test
.\gradlew.bat connectedAndroidTest
```

`connectedAndroidTest` requiere un emulador o dispositivo conectado. `test` también corre los tests del backend, que necesitan Docker.

## Backend

API REST en Spring Boot con Kotlin, PostgreSQL y Flyway. Usa directamente la lógica de `shared/`. Hoy tiene salud, migraciones, errores, documentación e identidad: alta de clubes, login con tokens, personal con roles y aislamiento entre clubes. Los datos de negocio de los clubes llegan en las próximas entregas. El diseño está en la [ADR-001](docs/arquitectura/ADR-001-backend-saas.md).

Para correrla localmente, desde la raíz del proyecto:

```powershell
docker compose -f backend/docker-compose.yml up -d
$clave = New-Object byte[] 32
[Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($clave)
$env:CLUB_AUTH_JWT_SECRET = [Convert]::ToBase64String($clave)
.\gradlew.bat :backend:bootRun
```

El primer comando levanta PostgreSQL en `localhost:5432` con una base `clubdeportivo` solo para desarrollo. Los tres siguientes generan la clave con la que la API firma los tokens de acceso. La API la toma de `CLUB_AUTH_JWT_SECRET` (Base64, 32 bytes o más) y no arranca sin ella. La clave nunca va en el repositorio. En Git Bash alcanza con `export CLUB_AUTH_JWT_SECRET=$(openssl rand -base64 32)`. Con una clave nueva, los tokens emitidos antes dejan de servir.

Al arrancar, la API aplica con Flyway las migraciones de `backend/src/main/resources/db/migration/` y queda en `http://localhost:8080`:

- Salud: `GET /api/v1/health`
- OpenAPI en JSON: `GET /api/v1/openapi`
- Swagger UI: `/api/v1/docs`. El botón Authorize recibe el token de acceso.
- Alta de club: `POST /api/v1/clubs`
- Sesión: `POST /api/v1/auth/login`, `/auth/refresh`, `/auth/logout` y `/auth/password`
- Club activo: `GET /api/v1/club`
- Personal del club: `GET` y `POST /api/v1/members`, `DELETE /api/v1/members/{userId}`

Los errores responden con `application/problem+json` (RFC 9457).

Si el puerto 5432 está ocupado, se puede usar otro para la base y avisarle a la API:

```powershell
$env:POSTGRES_PORT = "5433"
docker compose -f backend/docker-compose.yml up -d
$env:SPRING_DATASOURCE_URL = "jdbc:postgresql://localhost:5433/clubdeportivo"
.\gradlew.bat :backend:bootRun
```

En otros entornos la conexión se configura con `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME` y `SPRING_DATASOURCE_PASSWORD`.

### Probar el login localmente

Con la API corriendo, desde otra terminal de PowerShell:

```powershell
$api = "http://localhost:8080/api/v1"
function Post($ruta, $cuerpo, $token) {
    $headers = @{}
    if ($token) { $headers.Authorization = "Bearer $token" }
    Invoke-RestMethod -Method Post "$api$ruta" -Headers $headers -ContentType "application/json" -Body ($cuerpo | ConvertTo-Json)
}

# 1. Dar de alta un club con su administradora e ingresar
Post "/clubs" @{ clubName = "Club Demo"; adminName = "Ana Admin"; adminEmail = "ana@example.com"; adminPassword = "clave-segura-123" }
$ana = Post "/auth/login" @{ email = "ana@example.com"; password = "clave-segura-123" }
Invoke-RestMethod "$api/club" -Headers @{ Authorization = "Bearer $($ana.accessToken)" }

# 2. Sumar personal: la respuesta trae una contraseña temporal, que se muestra una sola vez
$alta = Post "/members" @{ name = "Beto Gomez"; email = "beto@example.com"; role = "RECEPTION" } $ana.accessToken

# 3. El personal ingresa con la temporal y la cambia: hasta entonces el token no sirve para otra cosa
$beto = Post "/auth/login" @{ email = "beto@example.com"; password = $alta.temporaryPassword }
$beto = Post "/auth/password" @{ currentPassword = $alta.temporaryPassword; newPassword = "otra-clave-segura" } $beto.accessToken

# 4. Renovar la sesión, que rota el token de renovación, y cerrarla
$ana = Post "/auth/refresh" @{ refreshToken = $ana.refreshToken }
Post "/auth/logout" @{ refreshToken = $ana.refreshToken }
```

Si una cuenta tiene varios clubes, el login acepta `clubId` y la respuesta trae la lista en `clubs`. Para pasar a otro club sin volver a ingresar, se manda `clubId` en `/auth/refresh`. Pasados 5 intentos fallidos por cuenta, o 20 por IP, la API responde 429 durante 15 minutos.

Tests:

```powershell
.\gradlew.bat :backend:test
```

Los tests de integración levantan un PostgreSQL real con Testcontainers, así que necesitan Docker corriendo. No usan la base de `docker-compose.yml`. Cubren login, renovación y cierre de sesión, contraseña temporal, límite de intentos, y que cada endpoint rechace los datos de otro club, incluidas las reglas por fila de la base.

Para detener la base: `docker compose -f backend/docker-compose.yml down`. Con `-v` también se borran sus datos.

## Estructura

- `backend/`: API del servicio multi-club. Código en `backend/src/main/kotlin/`, migraciones de Flyway en `backend/src/main/resources/db/migration/` y tests de integración en `backend/src/test/`.
- `shared/`: modulo Kotlin puro, sin dependencias de Android, con la logica de negocio que tambien va a usar el backend: cobros, descuentos, becas y recargos, vencimientos, cuenta corriente y validaciones. Sus tests JVM viven en `shared/src/test/`.
- `app/src/main/java/com/example/clubdeportivo/`: Activities, adapters, acceso a SQLite y helpers de interfaz.
- `app/src/main/res/layout/`: pantallas XML.
- `app/src/main/res/menu/menu_bottom_nav.xml`: navegacion inferior.
- `app/src/main/res/values/`: colores, strings y temas.
- `app/src/test/`: tests unitarios JVM de la capa de interfaz e importacion.
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
- `club_configuration`: identidad del club, moneda, cuota, vencimiento, gracia, medios manuales y URI persistente del logo.

Convenciones importantes:

- Fechas en formato ISO `yyyy-MM-dd`.
- Dias como enteros `0..6`, donde `0 = Domingo`.
- Horas como minutos desde medianoche.
- Bajas de clientes y horarios como borrado logico.
- Esquema actual en version 3, con migraciones incrementales que preservan los datos existentes.

## Flujos principales

1. Login con usuario de prueba.
2. Inicio muestra fecha, bienvenida y actividades del dia.
3. Actividades permite buscar, agregar, editar y dar de baja horarios.
4. Listados alterna entre no socios, socios y vencimientos.
5. Pago de cuota registra pago o convierte un no socio en socio.
6. Pago de actividad registra pagos puntuales de no socios.
7. Resumen mensual agrupa ingresos por cuotas y actividades.
8. Configuracion permite guardar identidad, logo y reglas comerciales del club sin integrar cobros reales.
9. Administracion permite crear, editar y dar de baja profesores, ademas de mantener el catalogo y los precios de actividades.
10. Importar clientes desde CSV muestra una previsualizacion con validacion por fila y resumen de errores, y pide confirmacion antes de guardar. Acepta archivos separados por coma o punto y coma (UTF-8 o Windows-1252), hasta 1 MB y 2000 filas. Las filas validas se guardan juntas en una sola transaccion como no socios activos; las filas con errores no se importan.

## Pruebas

Tests unitarios JVM:

- `ClubFormattersTest`: formato de dias, horas y vencimientos.
- `LoginCredentialsTest`: credenciales validas e invalidas.
- `ClubConfigurationValidatorTest`: normalizacion, importes locales y reglas de campos.
- `DatabaseMigrationPlannerTest`: orden y seguridad de las migraciones de esquema.
- `CatalogManagementValidatorTest`: identidad, contacto, fechas e importes regionales de profesores y actividades.
- `CsvParserTest`: comillas, saltos de linea, separador `;` de Excel, lineas vacias y codificacion Windows-1252.
- `ClientCsvImportTest`: columnas obligatorias, normalizacion de DNI, telefono, fecha y email, errores por fila y duplicados contra la base y dentro del archivo.

Tests instrumentados:

- `DBHelperInstrumentedTest`: usa fixtures aislados y verifica pagos, cuenta corriente, solapamientos, CRUD protegido de profesores/actividades e importacion de clientes todo o nada sin depender del seed demo.
- `EnvironmentDatabaseInstrumentedTest`: confirma que demo inicia con datos y production con tablas operativas vacias.
- `ClubConfigurationInstrumentedTest`: verifica valores iniciales y persistencia completa en SQLite.
- `ClubConfigurationMigrationInstrumentedTest`: migra de v2 a v3 sin borrar datos.
- `VisualSmokeInstrumentedTest`: recorre las pantallas principales y genera capturas por variante, incluidos los listados y formularios de profesores y actividades y la previsualizacion, el filtro de errores y la confirmacion de la importacion CSV.

En cada PR, GitHub Actions corre los tests JVM de `shared` y de las dos variantes, Lint y el build de la app (`.github/workflows/android.yml`), y los tests y el build del backend contra PostgreSQL real (`.github/workflows/backend.yml`). Las pruebas instrumentadas todavia se corren a mano en un emulador.

Para obtener evidencia visual, ejecutar:

```powershell
.\gradlew.bat connectedAndroidTest
```

Luego revisar los artefactos generados por Android Test Orchestrator/runner en el reporte de instrumented tests del modulo `app`.

## Arquitectura

Las decisiones de arquitectura se registran en `docs/arquitectura/`. La [ADR-001](docs/arquitectura/ADR-001-backend-saas.md) define el backend propio para el servicio multi-club.

## Notas para desarrollo

- Mantener la arquitectura actual salvo que se planifique una refactorizacion mayor.
- Con `targetSdk` 36 la app se dibuja detras de las barras del sistema. Las pantallas sin `BottomNavigationView` deben llamar a `SystemBarInsets.applyTo(this)` despues de `setContentView`.
- Evitar introducir Compose, Room, Hilt o Navigation Component sin decision explicita.
- Revisar `AGENTS.md` y `CLAUDE.md` antes de cambios grandes.
- Si se modifica el esquema de SQLite, subir version de DB y definir migracion en `onUpgrade`.
- No asumir que cambios en `onCreate` impactan instalaciones existentes: solo corren al crear la base.
