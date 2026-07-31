# AGENTS.md

Guia para agentes que trabajen en este repositorio.

## Resumen del proyecto

Este repositorio contiene una app Android nativa llamada `Club deportivo`.

- Modulo principal: `:app`
- Package/namespace: `com.example.clubdeportivo`
- Lenguaje: Kotlin
- UI: Activities + layouts XML, no Compose
- Base de datos: SQLite local mediante `SQLiteOpenHelper` en `DBHelper.kt`
- Navegacion: `Intent` entre Activities y `BottomNavigationView`
- Build: Gradle Kotlin DSL con wrapper incluido

La app administra clientes/socios/no socios, actividades, profesores, horarios, pagos de cuotas, pagos de actividades y resumen mensual.

## Comandos utiles

Ejecutar desde la raiz del repo:

```powershell
.\gradlew.bat assembleDebug
.\gradlew.bat test
.\gradlew.bat connectedAndroidTest
```

En Windows usa `gradlew.bat`. En Linux/macOS usa `./gradlew`.

`connectedAndroidTest` requiere emulador o dispositivo Android conectado.

## Estructura relevante

- `settings.gradle.kts`: incluye solo el modulo `:app`.
- `build.gradle.kts`: plugins Android/Kotlin compartidos.
- `app/build.gradle.kts`: configuracion Android, SDKs y dependencias.
- `gradle/libs.versions.toml`: versiones de AGP, Kotlin, AndroidX, Material, JUnit y Espresso.
- `app/src/main/AndroidManifest.xml`: declara `LoginActivity` como launcher y el resto de Activities.
- `app/src/main/java/com/example/clubdeportivo/`: codigo Kotlin de Activities, adapters y DB.
- `app/src/main/res/layout/`: pantallas XML.
- `app/src/main/res/menu/menu_bottom_nav.xml`: items de navegacion inferior.
- `app/src/main/assets/sql/seed_inicial.sql`: seed historico; ver advertencias abajo.

## Stack y versiones

- Android Gradle Plugin: `8.12.3`
- Kotlin: `2.0.21`
- Gradle wrapper: `8.13`
- `compileSdk`: `36`
- `targetSdk`: `36`
- `minSdk`: `30`
- Java/Kotlin JVM target: `11`
- Dependencias principales: AndroidX Core KTX, AppCompat, Material Components, Activity, ConstraintLayout.

No introducir Compose, Room, Hilt, Navigation Component ni corrutinas salvo que el usuario lo pida explicitamente o que el cambio lo justifique muy claramente.

## Arquitectura actual

La app es simple y monolitica por pantalla:

- Cada pantalla es una `AppCompatActivity`.
- Las Activities obtienen vistas con `findViewById`.
- La navegacion pasa el nombre de usuario con el extra `usuario`.
- `DBHelper` concentra creacion de tablas, seed inicial, consultas, DTOs y operaciones de escritura.
- Los listados usan `RecyclerView` con adapters propios:
  - `NoSocioAdapter`
  - `SocioAdapter`
  - `VencimientoAdapter`
  - `ActividadCardAdapter`

Pantallas principales:

- `LoginActivity`: login hardcodeado (`admin`, `charlie`, `sacha`, `javo`, `heber`).
- `InicioActivity`: bienvenida, fecha y actividades del dia.
- `ActividadesActivity`: lista, busca, agrega, edita y da de baja horarios/actividades.
- `ListadosActivity`: alterna entre no socios, socios y vencimientos.
- `PagoDeCuotaActivity`: registra cuota o convierte no socio en socio.
- `PagoActividadActivity`: registra pago de actividad para no socios.
- `ResumenMensualActivity`: resume ingresos por mes.
- `ConfiguracionActivity`, `EditarAdminActivity`, `EditarUsuarioActivity`, `EditarActividadActivity`, `NuevoUsuarioActivity`, `NuevoHorarioActividadActivity`, `VerMasActivity`: flujos auxiliares.

## Modelo de datos

`DBHelper` crea estas tablas principales:

- `actividades`
- `clientes`
- `profesores`
- `cuotas`
- `pagos_actividad`
- `actividad_profesor`
- `dias_horarios`

Reglas importantes:

- `clientes` unifica socios y no socios mediante `esSocio`, `activo` y `carnet`.
- Los borrados de clientes y horarios son logicos en los flujos actuales.
- `dias_horarios.dia` usa `0..6`, con `0 = Domingo`, `1 = Lunes`, etc.
- Las horas se guardan como minutos desde medianoche (`hora_inicio`, `hora_fin`).
- Fechas de negocio se manejan como texto ISO `yyyy-MM-dd`.
- Las claves foraneas se activan en `onConfigure`.

## Zonas de cuidado

Antes de tocar datos o pagos, revisar el SQL real en `DBHelper.kt`.

- `app/src/main/assets/sql/seed_inicial.sql` parece desactualizado frente al esquema actual: menciona `no_socios`, pero la tabla vigente es `clientes` con `esSocio`. No lo trates como fuente autoritativa sin verificar.
- En `DBHelper.registrarPagoCuota`, confirmar nombres de columnas antes de modificar pagos: la tabla `cuotas` define `idCliente`, no `idSocio`.
- `ListadosActivity` declara `hoyISO` pero no se ve inicializado antes de usarse en `refreshVisibleList`.
- Hay texto con problemas de encoding/mojibake en varios archivos. Si editas texto visible, conserva UTF-8 y corrige solo lo relacionado con la tarea.
- Varias pantallas repiten la misma configuracion de `BottomNavigationView`; evita refactors amplios si el usuario pidio un cambio puntual.
- Algunas Activities cierran la DB manualmente despues de operaciones; revisa ciclos de vida antes de reutilizar instancias.
- Muchas validaciones son locales y con `Toast`; mantener el estilo salvo pedido contrario.

## Convenciones para cambios

- Mantener Kotlin + XML + AppCompat.
- Preferir cambios pequenos y localizados.
- Si agregas una pantalla:
  - crear Activity Kotlin en el package existente;
  - crear layout XML correspondiente;
  - declararla en `AndroidManifest.xml`;
  - pasar `usuario` en los intents si forma parte del flujo autenticado;
  - actualizar `menu_bottom_nav.xml` solo si corresponde.
- Si agregas datos:
  - cambiar `DBHelper.onCreate`;
  - actualizar `onUpgrade` con cuidado;
  - mantener fechas ISO y horarios en minutos;
  - validar constraints y claves foraneas.
- Si agregas recursos visuales:
  - usar `res/drawable`, `res/mipmap`, `res/values` segun corresponda;
  - no hardcodear colores nuevos si ya existe un recurso apropiado.

## Verificacion recomendada

Para cambios Kotlin/XML:

```powershell
.\gradlew.bat assembleDebug
.\gradlew.bat test
```

Para cambios de UI o navegacion, ademas abrir la app en emulador/dispositivo y recorrer:

1. Login.
2. Inicio.
3. Actividades.
4. Listados.
5. Pagos.
6. Ajustes.

Para cambios de DB, probar instalacion limpia o limpiar datos de la app, porque `onCreate` solo corre al crear la base.

## Reglas de colaboracion

- No reescribir arquitectura sin pedido explicito.
- No pisar cambios no relacionados.
- No modificar `.idea`, `local.properties`, `.gradle`, `build/` ni archivos generados.
- Documentar en la respuesta final que comandos se ejecutaron y si fallaron.
- Si no se pudo correr un test por falta de SDK, emulador o red, decirlo claramente.


