# ADR-001: Backend propio para el servicio multi-club

- **Estado:** aceptado
- **Fecha:** 2026-11-08
- **Alcance:** Fase 2 de la hoja de ruta (seguridad y servicio)

## Contexto

La app es un cliente Android en Kotlin con Views XML que guarda todo en SQLite local (`SQLiteOpenHelper`, esquema v4). Funciona sin red y administra un solo club por instalación. Hoy:

- **El login no es seguro.** `LoginCredentials` compara contra una lista de usuarios incluida en el APK, y la contraseña es igual al usuario.
- **No hay usuarios, roles ni sesiones.** El nombre de usuario viaja entre pantallas como un extra del `Intent`.
- **Un club por dispositivo.** `club_configuration` tiene una sola fila (`id = 1`) y ninguna tabla tiene `club_id`.
- **Ids locales.** Casi todas las tablas usan enteros autoincrementales, que chocarían si dos dispositivos crean registros sin conexión. `profesores` usa el DNI como clave primaria.
- **Borrados mixtos.** Clientes y horarios se dan de baja con un borrado lógico. Las actividades del catálogo y los descuentos sin uso, los beneficios y la relación entre actividad y profesor se borran físicamente.
- **Lógica de negocio pura en Kotlin.** `ChargeCalculator`, `VencimientoCalculator` y los validadores no dependen de Android y tienen tests JVM.

El objetivo de la Fase 2 es tener autenticación real, usuarios con roles, un backend HTTPS para varios clubes con sus datos aislados, funcionamiento offline con sincronización, y backup con restauración probada.

El proyecto es, ante todo, una pieza de portfolio que se presenta como SaaS. No hay apuro comercial, así que se prioriza demostrar diseño y operación de backend por sobre salir rápido al mercado.

### Restricciones que se mantienen

- No se integran pagos reales. Las cuotas y los pagos de actividades siguen siendo registros manuales que carga el personal del club.
- La app sigue en Kotlin, Views XML, AppCompat, Material y `SQLiteOpenHelper`. No se suman Compose, Room, Hilt ni Navigation.
- No se registran datos personales en logs, capturas ni reportes de errores.

## Decisión

Construir una **API propia en Spring Boot con Kotlin y PostgreSQL**, desplegada con Docker en la plataforma existente de OCI detrás de HTTPS.

La app pasa a ser **offline-first**: SQLite sigue siendo la base con la que trabaja la interfaz, y una capa de sincronización envía los cambios pendientes y trae los del servidor. El club deja de estar implícito: cada dato pertenece a un club y el servidor lo hace cumplir en cada operación.

## Opciones consideradas

| Criterio | Supabase | Firebase | API propia (Ktor) | **API propia (Spring Boot)** |
|---|---|---|---|---|
| Valor para el CV | Medio: integración de un servicio | Bajo-medio | Alto | **Alto, y es lo más pedido en ofertas de backend** |
| Aislamiento entre clubes | Bueno, con reglas por fila de Postgres | Reglas de seguridad por documento | Lo diseñamos nosotros | **Lo diseñamos nosotros, con reglas por fila como respaldo** |
| Offline y sincronización | Hay que construirla en la app | Viene resuelta | Hay que construirla | **Hay que construirla** |
| Encaje con el modelo actual | Relacional, muy parecido | Documental, hay que rehacerlo | Relacional | **Relacional** |
| Dependencia del proveedor | Media: es Postgres y se puede autoalojar | Alta | Ninguna | **Ninguna** |
| Costo de infraestructura | Plan gratuito con límites; después, pago por proyecto | Pago por lecturas y escrituras | Casi nulo en OCI | **Casi nulo en OCI** |
| Esfuerzo | Bajo | Bajo-medio | Alto | **Alto** |
| Operación | La resuelve el proveedor | La resuelve el proveedor | A cargo nuestro | **A cargo nuestro** |

**Supabase** es la alternativa más rápida y la que menos ata a un proveedor, pero deja la parte más valiosa para el portfolio (autenticación, autorización y aislamiento) en manos del servicio. **Firebase** resuelve el offline, pero obliga a pasar de un modelo relacional a uno documental y es el que más ata. **Ktor** encaja muy bien con Kotlin, pero aparece mucho menos en búsquedas laborales que Spring Boot, que con Kotlin tiene soporte de primera.

## Arquitectura

```mermaid
flowchart LR
    subgraph Android
        UI[Pantallas XML] --> DB[(SQLite local)]
        DB --> OUT[Cola de cambios]
        SYNC[Sincronización] --> DB
        OUT --> SYNC
    end
    SYNC -- HTTPS / JSON --> PROXY[Proxy inverso con TLS]
    subgraph OCI [OCI: Docker]
        PROXY --> API[API Spring Boot]
        API --> PG[(PostgreSQL)]
        PG -. pg_dump diario .-> OBJ[(Object Storage)]
    end
```

### Varios clubes en una misma base

- **Modelo:** una sola base y un solo esquema, con una columna `club_id` en cada tabla de datos del club. Se descarta un esquema por club: multiplica las migraciones y complica la operación sin aportar a esta escala.
- **Dónde se aplica:** el `club_id` sale del token del usuario, nunca de un parámetro que mande la app. La capa de servicios lo exige y todas las consultas filtran por él.
- **Respaldo en la base:** reglas por fila de PostgreSQL (row-level security), con el club activo fijado en cada transacción. Si una consulta se olvida del filtro, la base igual no devuelve datos de otro club.
- **Pruebas:** cada endpoint tiene un test que intenta leer y escribir datos de otro club y debe fallar.

### Identidad, roles y sesiones

- **Usuarios:** email y contraseña con hash Argon2id. Un usuario puede pertenecer a más de un club con un rol distinto en cada uno.
- **Roles:** administrador, recepción, caja y solo lectura. Los permisos se validan en el servidor en cada operación. La app oculta lo que el rol no permite, pero no es la que decide.
- **Tokens:** un token de acceso de vida corta (15 minutos) y un token de renovación opaco que rota en cada uso. El servidor guarda el hash del token de renovación, así que cerrar sesión o dar de baja a un usuario lo invalida de inmediato.
- **En el dispositivo:** el token de renovación se guarda cifrado con una clave del Android Keystore. Nada de credenciales en el APK.
- **Alta de un club:** un endpoint crea el club y su administrador. El administrador da de alta a su personal con una contraseña temporal que hay que cambiar en el primer ingreso. Las invitaciones por email quedan para más adelante porque requieren un proveedor de correo.
- **Fuerza bruta:** límite de intentos de login por cuenta y por IP.

### Datos en el servidor

- Cada tabla de datos del club replica la local y suma `club_id`, un `id` UUID, `version`, `updated_at`, `updated_by` y `deleted_at`. Los borrados pasan a ser lógicos para que se puedan sincronizar.
- **Importes en `NUMERIC(12,2)`**, no en coma flotante.
- **El DNI es único dentro de cada club**, no en toda la base: una misma persona puede ser socia de dos clubes. `profesores` deja de usar el DNI como clave primaria.
- Migraciones versionadas con Flyway.

### Ids que no chocan sin conexión

Los registros nuevos reciben un UUID generado en el dispositivo, así dos equipos pueden crear datos sin conexión sin pisarse. En SQLite, la migración a la v5 suma una columna `uuid` a cada tabla y mantiene los ids enteros actuales, así el código existente sigue funcionando.

### Sincronización

La app trabaja siempre contra SQLite. La sincronización corre al abrir la app, después de cada cambio si hay conexión y periódicamente en segundo plano.

- **Envío:** cada cambio local se guarda en una cola junto con la versión del registro que se editó. La app manda la cola en lotes. El servidor aplica cada cambio en una transacción y lo identifica con un id propio, así reenviarlo no lo duplica.
- **Recepción:** el servidor numera los cambios de cada club en una secuencia creciente. La app pide "todo lo posterior al último número que recibí" y lo aplica en SQLite.
- **Conflictos:** según el tipo de dato.
  - **Pagos y cuotas:** solo se agregan, nunca se editan. Una corrección es una anulación con contramovimiento, que es justamente lo que pide la Fase 3. Así no hay conflictos de edición sobre la plata.
  - **Clientes, catálogo, horarios y configuración:** control de versión optimista. Si el registro cambió en el servidor desde que se editó en el dispositivo, el servidor rechaza ese cambio y devuelve la versión actual. La app lo muestra en una lista de conflictos para que el personal elija qué versión conservar. No hay "gana el último" silencioso.
  - **Reglas de negocio:** el servidor las vuelve a validar. Si dos equipos registran sin conexión la cuota del mismo socio para el mismo mes, el segundo se rechaza y aparece como conflicto.

### Lógica compartida

`ChargeCalculator`, `VencimientoCalculator` y los validadores pasan a un módulo Kotlin puro (`shared`) que usan la app y el backend. El servidor recalcula cada cobro con exactamente las mismas reglas que la app, en lugar de confiar en el importe que llega.

### API

- REST con JSON, versionada en `/api/v1` y documentada con OpenAPI.
- Errores con el formato estándar de detalles de problema (RFC 9457).
- Endpoints principales: autenticación (`/auth/login`, `/auth/refresh`, `/auth/logout`), alta de clubes, administración de miembros, `/sync/push` y `/sync/pull`.

### Cliente Android

- **HTTP:** OkHttp con kotlinx.serialization, que son livianos. No se suma Retrofit.
- **Asincronía:** corrutinas de Kotlin.
- **Sincronización en segundo plano:** WorkManager, que reintenta y respeta la batería y la conectividad.
- **Variante demo:** sigue funcionando sin backend, con los datos de ejemplo y un ingreso local de demostración, para que se pueda probar sin servidor. La variante production exige el backend.

### Despliegue y operación

- **Contenedores:** API, PostgreSQL y un proxy inverso que obtiene y renueva el certificado TLS solo, en la plataforma Docker de OCI.
- **Secretos:** fuera del repositorio, en archivos de entorno del servidor.
- **Backups:** `pg_dump` diario a OCI Object Storage con retención. Hay un procedimiento escrito de restauración y se prueba restaurando de verdad en una base aparte.
- **Monitoreo:** logs estructurados sin datos personales, un endpoint de salud y un chequeo externo de disponibilidad.
- **Límite conocido:** una sola VM es un punto único de falla. Alcanza para portfolio y piloto. Si hubiera clientes pagos, el paso siguiente es una base PostgreSQL administrada.

### CI/CD

GitHub Actions en cada PR:

- **Backend:** compilación y tests, incluidos los de integración con Testcontainers contra un PostgreSQL real.
- **Android:** compilación, tests JVM y Lint.
- **Despliegue:** se dispara al crear una etiqueta de versión, publicando la imagen y actualizando el servidor.

## Consecuencias

**A favor**

- Muestra el ciclo completo de un SaaS: autenticación, autorización, datos de varios clubes aislados, sincronización offline, despliegue, backups y CI/CD.
- No depende de ningún proveedor y el costo de infraestructura es casi nulo.
- Las reglas de cobro viven en un solo lugar y valen igual en la app y en el servidor.

**En contra**

- Es la opción que más trabajo lleva. La sincronización con conflictos es la parte más compleja.
- La operación, la seguridad y las actualizaciones quedan a nuestro cargo.
- La app suma dependencias nuevas (OkHttp, kotlinx.serialization, corrutinas, WorkManager) y una migración de SQLite a la v5.

## Plan de entregas

Cada entrega es un PR con sus tests y, cuando cambia la interfaz, evidencia visual.

1. **Base del repo:** estructura de módulos, módulo `shared` con la lógica de cobro (sin cambiar comportamiento) y CI para Android.
2. **Esqueleto del backend:** Spring Boot, endpoint de salud, Flyway, PostgreSQL local con Docker Compose, Testcontainers, OpenAPI y CI.
3. **Identidad:** usuarios, clubes, membresías y roles; login, renovación y cierre de sesión; alta de club; tests de aislamiento entre clubes.
4. **Despliegue:** HTTPS en OCI, backups automáticos y una restauración probada.
5. **Login real en la app:** la variante production usa el backend, guarda el token cifrado, maneja expiración y cierre de sesión, y muestra lo que permite cada rol.
6. **Datos del club en el servidor:** esquema de negocio en PostgreSQL y SQLite v5 (UUID, versión, borrados lógicos y cola de cambios) con migración.
7. **Sincronización:** envío, recepción, conflictos y una pantalla con el estado de la sincronización y los conflictos pendientes.
8. **Permisos por rol** en cada operación, en el servidor y en la app.
9. **Carga inicial:** subir los datos de una instalación existente al dar de alta su club.

## Decisiones tomadas

1. **El backend vive en este mismo repo**, con los módulos `app/`, `backend/` y `shared/`. El módulo compartido se usa directo, sin publicar artefactos, y el portfolio muestra todo junto.
2. **La app suma OkHttp, kotlinx.serialization, corrutinas y WorkManager.** Retrofit, Room y Hilt siguen fuera.
3. **La variante demo sigue funcionando sin backend.**
4. **El personal se da de alta con una contraseña temporal.** Las invitaciones por email quedan para más adelante.

## Decisiones abiertas

Se resuelven antes de la entrega 4 (despliegue):

1. **Dónde vive la infraestructura (Terraform):** en este repo o en el repo de infraestructura existente.
2. **Qué dominio usa la API:** hace falta uno para el certificado HTTPS.

## Fuera de alcance

- Panel web.
- Cobro real de suscripciones a los clubes.
- Envío de emails y notificaciones push.
- Recibos, anulaciones y cierre de caja, que son de la Fase 3, aunque el modelo de pagos ya queda preparado para eso.
