# CLAUDE.md

Instrucciones para Claude Code y asistentes similares en este proyecto.

## Identidad del repo

`Club deportivo` es una aplicacion Android nativa de gestion para un club deportivo. Esta construida con Kotlin, Activities XML, AppCompat, Material Components y SQLite local. No usa Compose, Room ni una arquitectura por capas formal.

Trabaja con el estilo existente: cambios concretos, poca abstraccion nueva y cuidado especial con la base de datos.

## Como empezar

1. Lee `AGENTS.md`.
2. Revisa `app/build.gradle.kts` y `gradle/libs.versions.toml` si el cambio toca dependencias o SDK.
3. Revisa `DBHelper.kt` si el cambio toca socios, no socios, actividades, horarios, cuotas, pagos o resumen mensual.
4. Revisa el layout XML de la Activity antes de cambiar su Kotlin.

Comandos principales:

```powershell
.\gradlew.bat assembleDebug
.\gradlew.bat test
```

Usa `.\gradlew.bat connectedAndroidTest` solo si hay emulador o dispositivo conectado.

## Pautas de implementacion

- Mantener `findViewById`, XML layouts y `AppCompatActivity`.
- Mantener `BottomNavigationView` y el extra `usuario` al navegar entre pantallas autenticadas.
- Para UI, reutilizar recursos existentes en `res/drawable`, `res/values` y layouts similares.
- Para datos, preferir metodos nuevos o ajustes localizados en `DBHelper`.
- Usar transacciones para operaciones que escriben en mas de una tabla.
- Mantener fechas como `yyyy-MM-dd`.
- Mantener dias como enteros `0..6`, con `0 = Domingo`.
- Mantener horarios como minutos desde medianoche.
- Evitar migraciones grandes salvo pedido explicito.

## Mapa rapido de archivos

- `app/src/main/java/com/example/clubdeportivo/DBHelper.kt`: esquema SQLite, seed, queries, writes y DTOs.
- `LoginActivity.kt`: pantalla inicial y credenciales hardcodeadas.
- `InicioActivity.kt`: dashboard con actividades del dia.
- `ActividadesActivity.kt`: listado y busqueda de horarios/actividades.
- `NuevoHorarioActividadActivity.kt`: alta de horarios.
- `EditarActividadActivity.kt`: edicion de horarios/actividad.
- `ListadosActivity.kt`: tabs manuales para socios, no socios y vencimientos.
- `PagoDeCuotaActivity.kt`: pagos de cuota y conversion de no socio a socio.
- `PagoActividadActivity.kt`: pagos de actividades para no socios.
- `ResumenMensualActivity.kt`: resumen de ingresos.
- `VerMasActivity.kt`, `EditarUsuarioActivity.kt`, `NuevoUsuarioActivity.kt`: detalle, edicion y alta de clientes.

## Riesgos conocidos

Ten presente estos puntos antes de hacer cambios:

- El seed en `app/src/main/assets/sql/seed_inicial.sql` no parece alineado con el esquema actual. El seed que realmente importa esta embebido en `DBHelper.onCreate`.
- La tabla `clientes` reemplaza la separacion historica entre socios/no socios; no reintroducir tablas `socios` o `no_socios` sin migracion clara.
- Verificar columnas de `cuotas`: el esquema usa `idCliente`.
- `ListadosActivity.hoyISO` esta declarado como `lateinit` y puede usarse sin inicializar en refrescos.
- Hay mojibake en strings y comentarios. Si editas una zona, guarda en UTF-8 y corrige solo lo necesario.
- La navegacion inferior esta duplicada en varias Activities; si se corrige un flujo, revisar que el extra `usuario` no se pierda.

## Estilo de respuesta esperado

Cuando termines una tarea:

- Explica brevemente que cambiaste.
- Indica archivos modificados con rutas.
- Di que comandos corriste.
- Si no corriste tests/build, explica por que.
- Menciona cualquier riesgo residual concreto.

## Que evitar

- No agregar frameworks nuevos por comodidad.
- No convertir la app a Compose.
- No reemplazar SQLiteOpenHelper por Room sin pedido explicito.
- No hacer refactors globales de navegacion o arquitectura junto con fixes pequenos.
- No tocar archivos generados, `.idea`, `.gradle`, `build/` ni `local.properties`.
- No asumir que un cambio de `onCreate` afectara usuarios existentes: para bases ya creadas hace falta `onUpgrade` o limpiar datos.

