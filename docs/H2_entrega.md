# ElevaPro — Entrega H2

**Materia:** Tecnología de Aplicaciones para Dispositivos Móviles  
**Institución:** Universidad Argentina de la Empresa (UADE)  
**Año:** 2026

**Equipo de desarrollo:**

| Dev | Integrante | Módulos responsables |
|---|---|---|
| Dev 1 | Ignacio Lapolla | Estructura, autenticación, módulo órdenes |
| Dev 2 | Jonathan Dominguez | Módulo clientes y facturación |
| Dev 3 | Federico Isla | Módulo administración |
| Dev 4 | Agustín Pérez | Módulo perfil y artículos |

---

## Índice

1. [Resumen ejecutivo H1 → H2](#1-resumen-ejecutivo-h1--h2)
2. [Actualización del estado de persistencia](#2-actualización-del-estado-de-persistencia)
3. [Nuevas funcionalidades implementadas](#3-nuevas-funcionalidades-implementadas)
4. [Ciclo de vida, navegación y estado de UI](#4-ciclo-de-vida-navegación-y-estado-de-ui)
5. [Tests unitarios](#5-tests-unitarios)
6. [Justificación del API Level mínimo](#6-justificación-del-api-level-mínimo)
7. [Arquitectura actualizada H1 → H2](#7-arquitectura-actualizada-h1--h2)
8. [Limitaciones actuales](#8-limitaciones-actuales)
9. [Mejoras futuras (H3)](#9-mejoras-futuras-h3)

**Documentación complementaria:**
- [Casos de uso HU-01 a HU-07](casos_de_uso.md)
- [Diagramas de secuencia](diagramas_secuencia.md)
- [Heurísticas de Nielsen — checklist](nielsen_checklist.md)
- [Métricas de cold start](cold_start_medicion.md)

---

## 1. Resumen ejecutivo H1 → H2

En H1 se entregó una APK navegable con arquitectura MVVM + Repository completa pero con toda la capa de datos implementada mediante `FakeRepository` (datos en memoria con `MutableStateFlow`). El contrato de las interfaces de repositorio quedó definido desde H1, lo que permitió en H2 reemplazar las implementaciones de datos reales sin modificar una sola línea de ViewModel o UI.

### Compromisos de H1 cumplidos en H2

| Compromiso H1 | Estado H2 |
|---|---|
| Reemplazar `FakeOrdenesRepository` por Room | ✅ Implementado |
| Reemplazar `FakeClienteRepository` por Room | ✅ Implementado |
| Reemplazar `FakePermisosRepository` por Room | ✅ Implementado |
| Agregar tests unitarios de ViewModel | ✅ 5 tests en `OrdenesViewModelTest` |
| Preparar stub Retrofit para H3 | ✅ `NetworkModule` + `ArcaApiService` creados |
| Permiso CAMERA en runtime | ✅ Implementado antes de abrir cámara |
| Adjuntar fotos a órdenes | ✅ Implementado con fix de race condition |

### Compromisos pendientes para H3

- Reemplazar `FakeAuthRepository` por Firebase Authentication
- Conectar `ArcaApiService` con el backend real
- Sincronización offline → online completa para todos los módulos
- Reemplazar repositorios Fake restantes (Facturación, Artículos, Usuarios, etc.)

---

## 2. Actualización del estado de persistencia

### Repositorios migrados a Room en H2

| Repositorio | H1 | H2 | Entidad Room |
|---|---|---|---|
| `AuthRepository` | Fake | Fake (H3: Firebase) | — |
| `OrdenesRepository` | Fake | **Room** | `OrdenEntity` |
| `ClienteRepository` | Fake | **Room** | `ClienteEntity` |
| `PermisosRepository` | Fake | **Room** | `PermisoEntity` |
| `FacturacionRepository` | Fake | Fake | — |
| `ArticulosRepository` | Fake | Fake | — |
| `NotificacionesRepository` | Fake | Fake | — |
| `UsuariosRepository` | Fake | Fake | — |
| `SupervisoresRepository` | Fake | Fake | — |
| `PlantillasRepository` | Fake | Fake | — |
| `EmpresaRepository` | Fake | Fake | — |

### Justificación del enfoque parcial

Los módulos migrados a Room (Órdenes, Clientes, Permisos) corresponden al dominio core de la aplicación: son los datos que el técnico operativo necesita en campo, incluso sin conexión. Los módulos que permanecen en Fake (Facturación, Administración, etc.) son funcionalidades de back-office que en producción consumirán el backend ARCA directamente, sin necesidad de caché local persistente.

### Configuración de Room

```kotlin
// DatabaseModule.kt
@Database(
    entities = [OrdenEntity::class, ClienteEntity::class, PermisoEntity::class],
    version = 1
)
@TypeConverters(Converters::class)
abstract class ElevaProDatabase : RoomDatabase()
```

La base de datos usa `fallbackToDestructiveMigration()` de forma intencional durante el desarrollo del TP: como los datos son semilla y no persisten entre releases de desarrollo, esta configuración permite iterar el esquema sin scripts de migración.

### Stub Retrofit preparado para H3

Se creó la infraestructura de red sin implementar llamadas reales aún:

```
di/
└── NetworkModule.kt       ← Retrofit + Gson + OkHttp configurados
data/
└── remote/
    └── ArcaApiService.kt  ← Interface con endpoints ARCA (stub)
```

Cuando en H3 se conecte el backend, solo se reemplaza el `@Binds` en `AppModule.kt` de `FakeXxxRepository` a `RoomXxxRepository` o `RemoteXxxRepository`, sin tocar ViewModels ni pantallas.

---

## 3. Nuevas funcionalidades implementadas

### 3.1 Adjuntar fotos a órdenes de trabajo

El técnico puede capturar fotos de comprobantes y adjuntarlas directamente a la orden desde `OrdenDetalleScreen`. El flujo utiliza `ActivityResultContracts.TakePicture()`.

**Decisión técnica:** la URI de la foto se almacena en un `mutableStateOf` con `remember` en el scope del composable. Esto evita una race condition que ocurría en la implementación previa, donde el callback de `ActivityResult` llegaba antes de que la composición reconociera la variable como observable, descartando la foto silenciosamente.

**Permiso CAMERA:** se solicita en runtime inmediatamente antes de abrir la cámara. En versiones anteriores la app crasheaba en dispositivos con `targetSdk ≥ 29` si el permiso no había sido otorgado previamente. Se usa `ActivityResultContracts.RequestPermission()` con fallback a `Snackbar` informativo si el usuario deniega.

### 3.2 Modo offline mejorado

El banner `SinConexion` ahora se muestra sobre los datos cacheados, no en lugar de ellos. En H1 la condición `lista.isEmpty()` ocultaba el banner cuando había datos en Room, dando la falsa impresión de que la app estaba online. La corrección:

```kotlin
// Antes (H1)
val mostrarBanner = !isOnline && lista.isEmpty()

// Después (H2)
val mostrarBanner = !isOnline
```

El técnico ahora ve claramente el estado de conectividad mientras sigue trabajando con los datos disponibles localmente.

### 3.3 Correcciones de UX y robustez

| Área | Corrección |
|---|---|
| `BackHandler` | Ya no se ignora silenciosamente durante estados Loading/Error cuando hay un overlay abierto |
| `mostrarVistaPrevia` | Migrado a `SavedStateHandle.getStateFlow()` — sobrevive process death |
| PDF descarga | Bloque duplicado extraído a lambda `descargarPdf()` compartida |
| `VistaPreviaOverlay` | TopBar manual reemplazada por `ElevaProTopAppBar` (consistencia visual) |
| `InfoRow` | Movido a `MD3Components.kt` como componente reutilizable del equipo |
| Artículos | Sheet "Nuevo artículo" ahora abre completamente expandido (`skipPartiallyExpanded = true`) |

---

## 4. Ciclo de vida, navegación y estado de UI

### 4.1 Manejo de estado con `sealed interface UiState`

Todos los ViewModels del equipo siguen el mismo contrato de estado:

```kotlin
sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
    data object SinConexion : UiState<Nothing>  // extiende Success con banner
}
```

La UI reacciona mediante `collectAsStateWithLifecycle()` para respetar el ciclo de vida del composable y evitar colecciones en background cuando la app no está en primer plano.

### 4.2 Persistencia ante process death

Las variables de estado que representan decisiones de navegación o UI efímera (como `mostrarVistaPrevia` en `OrdenDetalleScreen`) se almacenan en `SavedStateHandle`:

```kotlin
// OrdenesViewModel.kt
val mostrarVistaPrevia: StateFlow<Boolean> =
    savedStateHandle.getStateFlow("mostrarVistaPrevia", false)
```

Esto garantiza que si Android mata el proceso en background (por presión de memoria), el usuario retoma exactamente el mismo estado visual al volver a la app.

### 4.3 Navegación role-aware

`ElevaProApp.kt` calcula `rutasConBottomNav` en función del rol del usuario autenticado:

```
Rol Operativo  → [Órdenes, Clientes, Artículos, Perfil]
Rol Admin      → [Órdenes, Clientes, Facturación, Perfil]
```

El `NavGraph` tiene ~28 rutas definidas en `sealed class Screen` con parámetros tipados. El `ModalNavigationDrawer` expone acceso secundario a módulos de administración para el rol Admin.

### 4.4 Gestión del back stack

`BackHandler` se registra en pantallas con overlays (vista previa de PDF, firma) para interceptar el botón Atrás del sistema y cerrar el overlay antes de navegar hacia atrás, en lugar de saltar directamente a la pantalla anterior.

---

## 5. Tests unitarios

**36 tests unitarios — todos passing ✅**

Se usa `kotlinx-coroutines-test` con `runTest` y `StandardTestDispatcher` para testear flows y coroutines sin timers reales. Cada suite tiene su propio `FakeRepository` en memoria para aislar la lógica del ViewModel de la capa de datos.

### AgregarClienteViewModelTest — 10 tests

| Test | Qué verifica |
|---|---|
| `estado inicial es Formulario vacio` | El estado inicial es `Formulario` con campos vacíos |
| `onNombre actualiza el form` | `onNombre()` propaga el cambio al form |
| `cuit se recorta a 11 digitos` | CUIT no acepta más de 11 caracteres |
| `cuit solo acepta digitos` | CUIT filtra caracteres no numéricos |
| `form sin nombre no es valido` | `isFormValid` es `false` sin nombre |
| `guardar sin nombre no emite Guardado` | `guardar()` no avanza si el form es inválido |
| `guardar con nombre valido emite Guardado` | `guardar()` emite `Guardado` con form válido |
| `guardar crea el cliente en el repositorio` | El cliente se persiste en el repositorio |
| `en modo edicion carga los datos del cliente existente` | Edición pre-carga los datos del cliente |
| `en modo edicion guardar actualiza en lugar de crear` | Edición llama `actualizar()` no `agregar()` |

### GenerarFacturaViewModelTest — 10 tests

| Test | Qué verifica |
|---|---|
| `estado inicial es Loading luego Formulario` | Transición Loading → Formulario al iniciar |
| `Formulario expone los clientes del repositorio` | El listado de clientes se carga correctamente |
| `form sin datos no es valido` | `isFormValid` es `false` con form vacío |
| `generar sin form valido no navega atras` | `generar()` no avanza si el form es inválido |
| `generar con form completo navega atras` | `generar()` emite en `navegarAtras` con form válido |
| `generar agrega la factura al repositorio` | La factura se persiste en el repositorio |
| `onMontoNeto solo acepta digitos coma y punto` | Filtrado de entrada numérica |
| `calculo IVA correcto para tipo A` | IVA 21% calculado correctamente para factura tipo A |
| `calculo IVA es cero para tipo C` | Sin IVA para factura tipo C |
| `factura generada usa monto total con IVA para tipo B` | El monto guardado incluye IVA para tipo B |

### OrdenesViewModelTest — 10 tests

| Test | Qué verifica |
|---|---|
| `vistaPrevia_estadoInicial_esFalse` | El valor inicial en `SavedStateHandle` es `false` |
| `vistaPrevia_alActivar_emiteTrue` | `activarVistaPrevia()` emite `true` |
| `vistaPrevia_alDesactivar_emiteFalse` | `cerrarVistaPrevia()` emite `false` |
| `vistaPrevia_persisteEnSavedStateHandle` | El valor se escribe en `SavedStateHandle` |
| `vistaPrevia_restauradaTrasProcessDeath` | Recrear el ViewModel con handle pre-poblado restaura el estado |
| *(5 tests adicionales de filtrado y estado de órdenes)* | Filtros por estado, búsqueda y carga de lista |

### PermisosRepositoryTest — 5 tests

| Test | Qué verifica |
|---|---|
| *(5 tests de PermisosRepository)* | CRUD de permisos en Room: insertar, consultar, actualizar, eliminar, listar |

### ExampleUnitTest — 1 test

| Test | Qué verifica |
|---|---|
| `addition_isCorrect` | Sanity check del entorno de testing |

### Setup común

```kotlin
@Before
fun setUp() { Dispatchers.setMain(StandardTestDispatcher()) }

@After
fun tearDown() { Dispatchers.resetMain() }
```

---

## 6. Justificación del API Level mínimo

| Parámetro | Valor |
|-----------|-------|
| `minSdk` | 27 (Android 8.1 Oreo) |
| `targetSdk` | 36 (Android 16) |
| `compileSdk` | 36 |

### Público objetivo

ElevaPro está destinada a **técnicos de ascensores en Argentina**, que utilizan el dispositivo como herramienta de trabajo provista o gestionada por la empresa. En entornos corporativos y de campo, el ciclo de renovación de dispositivos es más lento que en el mercado de consumo: es habitual encontrar equipos con 3-4 años de antigüedad corriendo Android 8.x o 9.x.

### Cobertura de mercado

Según el Android Studio Distribution Dashboard, **Android 8.1 (API 27) o superior cubre aproximadamente el 96% de los dispositivos Android activos** a nivel global. Para el mercado empresarial argentino, donde los dispositivos suelen ser de gama media-baja con actualizaciones de sistema menos frecuentes, esta cobertura es aún más representativa.

### Justificación técnica de API 27

`minSdk = 27` se eligió por las siguientes razones:

- **API 26 (Android 8.0)** introdujo los canales de notificación (`NotificationChannel`), requeridos para la futura implementación de alertas de nuevas órdenes.
- **API 27 (Android 8.1)** agrega `autofillHints` para formularios (CUIT, email, teléfono) y mejoras en `WifiManager` relevantes para detección de conectividad.
- Bajar a **API 24 o 25** hubiera requerido compatibilidad con versiones de Jetpack Compose y Material 3 más restrictivas, incrementando la complejidad de mantenimiento sin ampliar significativamente la base de usuarios objetivo.
- Subir a **API 29+** hubiera excluido dispositivos corporativos con Android 8.x que aún están en servicio activo en el sector.

### Conclusión

`minSdk = 27` representa el balance óptimo entre cobertura del público objetivo (~96%), acceso a las APIs modernas de Android necesarias para la app y compatibilidad con Jetpack Compose + Material 3.

---

## 7. Arquitectura actualizada H1 → H2


### Diagrama de capas actualizado

```
┌─────────────────────────────────────────────────────────────┐
│  UI Layer — Jetpack Compose + Material 3                    │
│  OnboardingScreen · LoginScreen · OrdenesScreen · ...       │
│  (28 pantallas, sin cambios de contrato respecto a H1)      │
└──────────────────────┬──────────────────────────────────────┘
                       │ StateFlow / eventos
┌──────────────────────▼──────────────────────────────────────┐
│  ViewModel Layer — Hilt ViewModels                          │
│  AuthVM · OrdenesVM · ClientesVM · PerfilVM · ...           │
│  Estado: sealed UiState  |  SavedStateHandle para death     │
└──────────────────────┬──────────────────────────────────────┘
                       │ suspend / Flow
┌──────────────────────▼──────────────────────────────────────┐
│  Repository Layer — interfaces inyectadas por Hilt          │
│  ┌─────────────────────┐  ┌──────────────────────────────┐  │
│  │  Room (H2)          │  │  Fake (pendiente H3)         │  │
│  │  OrdenesRepository  │  │  FacturacionRepository       │  │
│  │  ClienteRepository  │  │  ArticulosRepository         │  │
│  │  PermisosRepository │  │  UsuariosRepository · ...    │  │
│  └─────────────────────┘  └──────────────────────────────┘  │
└──────────────────────┬──────────────────────────────────────┘
                       │
┌──────────────────────▼──────────────────────────────────────┐
│  DataSource Layer                                           │
│  Room SQLite (local) · NetworkModule stub (H3: ARCA REST)   │
└─────────────────────────────────────────────────────────────┘
```

### Diferencias respecto al diagrama H1

| Aspecto | H1 | H2 |
|---|---|---|
| Órdenes DataSource | `MutableStateFlow` en memoria | Room DAOs |
| Clientes DataSource | `MutableStateFlow` en memoria | Room DAOs |
| Permisos DataSource | `MutableStateFlow` en memoria | Room DAOs |
| Red | No implementado | `NetworkModule` + `ArcaApiService` stub |
| Migraciones | N/A | `fallbackToDestructiveMigration` (dev) |

---

## 8. Limitaciones actuales

| Limitación | Detalle | Impacto |
|---|---|---|
| Autenticación local | `FakeAuthRepository` valida contra un mapa hardcodeado. No hay sesión real ni tokens. | En producción cualquier usuario podría acceder con cualquier contraseña |
| Facturación sin persistencia real | `FakeFacturacionRepository` almacena facturas en memoria. Se pierden al cerrar la app. | No apto para entorno productivo |
| Sin sincronización bidireccional | Room almacena datos localmente pero no sincroniza con ningún servidor. | Múltiples técnicos no ven los datos del otro en tiempo real |
| Sin notificaciones push | `NotificacionesRepository` es Fake. Las notificaciones son datos estáticos. | No hay alertas reales de nuevas órdenes |
| Fotos no persistidas en Room | Las URIs de fotos se guardan en la entidad de orden pero no se hace backup a la nube. | Las fotos se pierden si se desinstala la app |
| `fallbackToDestructiveMigration` | Cualquier cambio de esquema en Room borra todos los datos. | Intencional para desarrollo; debe resolverse antes de producción |

---

## 9. Mejoras futuras (H3)

| Mejora | Descripción | Prioridad |
|---|---|---|
| Firebase Authentication | Reemplazar `FakeAuthRepository`. Email/password con tokens JWT reales. | Alta |
| Conexión ARCA | Implementar `ArcaApiService` con los endpoints reales de facturación electrónica. | Alta |
| Migrar Facturación a Room + Retrofit | Persistencia local de facturas con sincronización remota. | Alta |
| Sync offline completo | WorkManager para encolar operaciones offline y ejecutarlas al recuperar conectividad. | Media |
| Migrar módulos Fake restantes | Artículos, Usuarios, Supervisores, Plantillas, Empresa. | Media |
| Backup de fotos | Upload a Firebase Storage o bucket S3 de las fotos adjuntas a órdenes. | Media |
| Migraciones Room productivas | Reemplazar `fallbackToDestructiveMigration` por scripts de migración versionados. | Baja (pre-deploy) |

---

*ElevaPro — Entrega H2 · UADE 2026*
