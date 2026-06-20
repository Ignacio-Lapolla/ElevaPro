# Diagramas de Secuencia — ElevaPro

> Flujos principales del TP Integrador H2. Cada diagrama muestra la interacción entre capas siguiendo el patrón MVVM + Repository + UDF.

---

## 1. Autenticación (Login)

Flujo: el usuario ingresa sus credenciales → `LoginViewModel` delega al `AuthRepository` → se inicializan los permisos del rol → navegación al Home.

```mermaid
sequenceDiagram
    actor Usuario
    participant LoginScreen
    participant LoginViewModel
    participant AuthRepository
    participant PermisosRepository
    participant NavGraph

    Usuario->>LoginScreen: ingresa empresa, email, password
    LoginScreen->>LoginViewModel: onEvent(LoginEvent.Ingresar)
    LoginViewModel->>LoginViewModel: _estado = Cargando

    LoginViewModel->>AuthRepository: login(empresa, email, password)
    AuthRepository-->>LoginViewModel: Result.success(usuario)

    LoginViewModel->>PermisosRepository: inicializarSiVacio(usuario.id, usuario.rol)
    PermisosRepository-->>LoginViewModel: permisos sembrados

    LoginViewModel->>LoginViewModel: _estado = Exito
    LoginScreen->>NavGraph: navega a Home (popUpTo Login inclusive)

    Note over LoginScreen,NavGraph: El back stack limpia Login<br/>para que el botón Atrás no regrese al login
```

---

## 2. Crear y Firmar una Orden de Trabajo

Flujo completo de trabajo: crear orden → cargar detalle → firmar con Canvas → orden marcada como firmada en Room.

```mermaid
sequenceDiagram
    actor Operativo
    participant NuevaOrdenScreen
    participant NuevaOrdenViewModel
    participant OrdenesRepository
    participant Room as Room (OrdenDao)
    participant OrdenDetalleScreen
    participant FirmaScreen
    participant FirmaViewModel

    Operativo->>NuevaOrdenScreen: selecciona cliente, tipo, plantilla
    NuevaOrdenScreen->>NuevaOrdenViewModel: guardar()
    NuevaOrdenViewModel->>NuevaOrdenViewModel: estadoGuardado = Guardando

    NuevaOrdenViewModel->>OrdenesRepository: crear(orden)
    OrdenesRepository->>Room: insertarUna(OrdenEntity)
    Room-->>OrdenesRepository: OK

    OrdenesRepository-->>NuevaOrdenViewModel: OK
    NuevaOrdenViewModel->>NuevaOrdenViewModel: estadoGuardado = Guardado
    NuevaOrdenScreen-->>Operativo: navega a OrdenDetalle

    Operativo->>OrdenDetalleScreen: toca "Solicitar firma"
    OrdenDetalleScreen-->>FirmaScreen: navega con ordenId

    Operativo->>FirmaScreen: dibuja firma en Canvas, ingresa nombre
    FirmaScreen->>FirmaViewModel: confirmarFirma(firmaBase64, nombre)
    FirmaViewModel->>OrdenesRepository: firmar(id, firmaBase64, nombre)
    OrdenesRepository->>Room: actualizar(entity.copy(firmada=true))
    Room-->>OrdenesRepository: OK
    FirmaViewModel->>FirmaViewModel: estado = confirmado=true
    FirmaScreen-->>Operativo: navega de vuelta a OrdenDetalle (orden firmada)
```

---

## 3. Modo Offline — Detección y Estado Sin Conexión

Flujo: la red se pierde → `NetworkMonitor` emite `false` → `OrdenesViewModel` emite `SinConexion` → UI muestra banner sin bloquear datos cacheados.

```mermaid
sequenceDiagram
    participant Sistema as Android ConnectivityManager
    participant NetworkMonitor
    participant OrdenesViewModel
    participant OrdenesRepository
    participant Room as Room (OrdenDao)
    participant OrdenesScreen

    Sistema->>NetworkMonitor: onLost(network)
    NetworkMonitor-->>OrdenesViewModel: isOnline emite false

    OrdenesViewModel->>OrdenesRepository: observarOrdenes() [Flow activo]
    OrdenesRepository->>Room: observarTodas()
    Room-->>OrdenesRepository: List<OrdenEntity> (cache local)
    OrdenesRepository-->>OrdenesViewModel: List<Orden>

    OrdenesViewModel->>OrdenesViewModel: combine(online=false, lista) → SinConexion
    OrdenesViewModel-->>OrdenesScreen: estado = SinConexion

    OrdenesScreen-->>OrdenesScreen: muestra BannerSinConexion<br/>+ lista con datos cacheados

    Note over NetworkMonitor,OrdenesScreen: Cuando vuelve la red,<br/>NetworkMonitor emite true<br/>y el estado vuelve a Success automáticamente
```

---

## 4. Generar Factura Electrónica

Flujo: el Administrador completa el formulario → `GenerarFacturaViewModel` valida → crea la `Factura` en el `FacturacionRepository` → estado `Generado` → navegación de vuelta.

```mermaid
sequenceDiagram
    actor Admin
    participant GenerarFacturaScreen
    participant GenerarFacturaViewModel
    participant ClienteRepository
    participant OrdenesRepository
    participant FacturacionRepository
    participant NavGraph

    GenerarFacturaViewModel->>ClienteRepository: observarClientes()
    ClienteRepository-->>GenerarFacturaViewModel: List<Cliente>
    GenerarFacturaViewModel->>OrdenesRepository: observarOrdenes()
    OrdenesRepository-->>GenerarFacturaViewModel: List<Orden> (firmadas y no facturadas)

    GenerarFacturaViewModel-->>GenerarFacturaScreen: estado = Formulario(clientes, ordenes)

    Admin->>GenerarFacturaScreen: selecciona cliente, tipo, fechas, monto
    GenerarFacturaScreen->>GenerarFacturaViewModel: onCliente / onTipo / onMontoNeto...
    GenerarFacturaViewModel->>GenerarFacturaViewModel: actualiza _form (montoIVA calculado automáticamente)

    Admin->>GenerarFacturaScreen: toca "Generar factura"
    GenerarFacturaScreen->>GenerarFacturaViewModel: generar()

    alt form inválido
        GenerarFacturaViewModel-->>GenerarFacturaScreen: estado sin cambio (guarda validación)
    else form válido
        GenerarFacturaViewModel->>FacturacionRepository: agregar(Factura(monto=total+IVA, estado=PENDIENTE))
        FacturacionRepository-->>GenerarFacturaViewModel: OK
        GenerarFacturaViewModel->>GenerarFacturaViewModel: _estado = Generado
        GenerarFacturaScreen->>NavGraph: popBackStack() (vuelve a Facturación)
    end
```

---

## 5. Arquitectura de Capas — Diagrama General

Vista estática de cómo se relacionan las capas en ElevaPro.

```mermaid
sequenceDiagram
    participant UI as Composable (UI)
    participant VM as ViewModel
    participant Repo as Repository
    participant Local as LocalDataSource (Room)
    participant Remote as RemoteDataSource (stub → ARCA futura)

    UI->>VM: evento (onClick, onValueChange)
    VM->>VM: actualiza UiState (Loading)
    VM->>Repo: solicita datos o acción
    Repo->>Local: consulta cache (Flow / suspend)
    Local-->>Repo: datos locales

    alt hay red disponible
        Repo->>Remote: sincroniza con servidor
        Remote-->>Repo: datos remotos
        Repo->>Local: guarda en Room
    else sin red
        Note over Repo,Remote: usa datos del cache local
    end

    Repo-->>VM: Flow<List<T>> o Result
    VM->>VM: actualiza UiState (Success / SinConexion / Error)
    VM-->>UI: StateFlow emite nuevo estado
    UI->>UI: recomposición solo donde cambió el estado
```

---

## Resumen de flujos cubiertos

| # | Flujo | Capas involucradas |
|---|---|---|
| 1 | Login + inicialización de permisos | UI → VM → AuthRepo + PermisosRepo → NavGraph |
| 2 | Crear orden + firma con Canvas | UI → VM → OrdenesRepo → Room → VM → UI |
| 3 | Modo offline con datos cacheados | ConnectivityManager → NetworkMonitor → VM → Room → UI |
| 4 | Generar factura con validación de IVA | UI → VM → ClienteRepo + OrdenesRepo + FacturacionRepo → UI |
| 5 | Arquitectura general de capas | UI ↔ VM ↔ Repo ↔ Local / Remote |
