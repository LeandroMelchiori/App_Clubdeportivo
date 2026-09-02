# Club deportivo

Aplicacion android nativa para la administracion de clubes deportivos

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

## Variantes de la aplicacion

- `demo`: usa `com.example.clubdeportivo.demo`, muestra una etiqueta de demostracion y carga datos ficticios.
- `production`: usa `com.example.clubdeportivo` y crea solamente el esquema vacio.

Los inicializadores viven en source sets separados. Los datos ficticios de `src/demo` no forman parte del codigo de produccion.

Comandos por variante:

```powershell
.\gradlew.bat assembleDemoDebug
.\gradlew.bat assembleProductionDebug
.\gradlew.bat testDemoDebugUnitTest
.\gradlew.bat testProductionDebugUnitTest
.\gradlew.bat connectedDemoDebugAndroidTest
.\gradlew.bat connectedProductionDebugAndroidTest
```

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

## Credenciales temporales de prueba

El login actual acepta usuarios temporales donde usuario y contrasena coinciden. Esta autenticacion debe reemplazarse antes de una entrega profesional:

- `admin` / `admin`
- `sacha` / `sacha`
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

## Pruebas

Tests unitarios JVM:

- `ClubFormattersTest`: formato de dias, horas y vencimientos.
- `LoginCredentialsTest`: credenciales validas e invalidas.
- `ClubConfigurationValidatorTest`: normalizacion, importes locales y reglas de campos.
- `DatabaseMigrationPlannerTest`: orden y seguridad de las migraciones de esquema.
- `CatalogManagementValidatorTest`: identidad, contacto, fechas e importes regionales de profesores y actividades.

Tests instrumentados:

- `DBHelperInstrumentedTest`: usa fixtures aislados y verifica pagos, cuenta corriente, solapamientos y CRUD protegido de profesores/actividades sin depender del seed demo.
- `EnvironmentDatabaseInstrumentedTest`: confirma que demo inicia con datos y production con tablas operativas vacias.
- `ClubConfigurationInstrumentedTest`: verifica valores iniciales y persistencia completa en SQLite.
- `ClubConfigurationMigrationInstrumentedTest`: migra de v2 a v3 sin borrar datos.
- `VisualSmokeInstrumentedTest`: recorre las pantallas principales y genera capturas por variante, incluidos los listados y formularios de profesores y actividades.

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
