# Contexto de sesión — ElevaPro · Pantalla Clientes

## Qué se analizó
Se revisaron en Figma las dos pantallas del flujo de Clientes:
- **ClientesScreen** (node-id `43:1384`): lista de clientes con búsqueda
- **NuevoClienteScreen** (node-id `43:1795`): formulario de alta (visto por imagen)

---

## Diseño Figma — ClientesScreen

- Fondo general: `#f2f2f7`
- **Header**: ícono hamburguesa (izquierda) + título "Clientes" — NO es `CenterAlignedTopAppBar`, el título está alineado a la izquierda junto al ícono
- **Buscador**: pill redondeada gris (`#d5d5d5`), `borderRadius = 28.dp`, ícono lupa a la izquierda, placeholder "Buscar por nombre o dirección…"
- **Contador**: texto pequeño `#3d3d3d`, 12sp, debajo del buscador: "16 clientes"
- **Lista**: `LazyColumn` con cards separadas por `7.dp`
- **Cada ClienteCard**:
  - Fondo blanco, borde `#bdbdbd` 1dp, `borderRadius = 12.dp`, sombra sutil
  - Avatar circular azul `#1565c0`, 48dp, con iniciales en blanco (bold)
  - Nombre del consorcio: 15sp medium, color `#1a1c1e`
  - Ícono ubicación (12dp) + dirección: 12sp regular, color `#3d3d3d`
  - Ícono teléfono (12dp) debajo de la dirección
  - Chevron derecho (`>`) al borde derecho, centrado verticalmente
  - Altura fija de la card: `91.dp`
- **FAB**: botón `+` (no extendido) en esquina inferior derecha → navega a `AgregarCliente`

---

## Diseño Figma — NuevoClienteScreen (visto por imagen)

- **Header**: flecha back + título "Nuevo cliente" (alineado a la izquierda)
- **7 campos en orden**:

| # | Campo | Ícono | Obligatorio |
|---|---|---|---|
| 1 | Supervisor | persona+ | No |
| 2 | Nombre del consorcio / cliente | personas | **Sí** |
| 3 | Teléfono | teléfono | No |
| 4 | Dirección | pin ubicación | **Sí** |
| 5 | NIF / CUIT | llave | No |
| 6 | E-mail | sobre | No |
| 7 | Notas | documento | No — campo multiline (altura mayor) |

- **Botón "Agregar cliente"**: pill redondeada, pegado al fondo, deshabilitado hasta que `nombre` y `direccion` no estén vacíos

---

## Estructura del proyecto (archivos clave ya existentes)

```
data/model/domain/Cliente.kt          ← ya existe, campos: id, nombre, direccion, telefono, email, cuit, notas, supervisorId
data/repository/FakeMockData.kt       ← tiene 11 clientes mock listos
data/repository/OrdenesRepository.kt  ← patrón a seguir para ClienteRepository
di/AppModule.kt                        ← agregar binding de ClienteRepository
ui/navigation/Screen.kt               ← ya tiene: Clientes, ClienteDetalle, AgregarCliente
ui/navigation/NavGraph.kt             ← Clientes y AgregarCliente son PlaceholderScreen — reemplazar
ui/components/MD3Components.kt        ← componentes reutilizables: ElevaProTopAppBar, ElevaProTextField, FilledPrimaryButton
ui/screen/ordenes/OrdenesScreen.kt    ← patrón exacto a seguir (ViewModel + UiState + Screen en mismo archivo)
```

---

## Plan de implementación

### Archivos a CREAR

**1. `data/repository/ClienteRepository.kt`**
- Interface `ClienteRepository` con: `observarClientes(): Flow<List<Cliente>>`, `obtenerPorId()`, `agregar()`
- `FakeClienteRepository` implementación con `MutableStateFlow(FakeMockData.clientes)`

**2. `ui/screen/clientes/ClientesScreen.kt`**
- `ClientesUiState`: sealed interface con `Loading`, `Success(clientes, total)`, `Error`
- `ClientesViewModel`: combina `observarClientes()` con `_searchQuery: MutableStateFlow<String>` usando `combine()` + filtro por nombre/dirección
- `ClientesScreen`: Scaffold con TopAppBar (hamburguesa + "Clientes"), buscador pill, contador, LazyColumn de ClienteCard, FAB con ícono `+`
- `ClienteCard`: composable privado con avatar de iniciales, nombre, dirección, teléfono, chevron

**3. `ui/screen/clientes/AgregarClienteScreen.kt`**
- `AgregarClienteUiState`: sealed interface con `Idle`, `Guardando`, `Guardado`, `Error`
- `AgregarClienteViewModel`: estado de formulario con cada campo, `isFormValid = nombre.isNotBlank() && direccion.isNotBlank()`, función `guardar()` que llama al repository
- `AgregarClienteScreen`: Scaffold con TopAppBar (back + "Nuevo cliente"), Column scrolleable con 7 campos usando `ElevaProTextField`, botón "Agregar cliente" con `enabled = isFormValid`

### Archivos a MODIFICAR

**`di/AppModule.kt`**: agregar `@Binds` para `FakeClienteRepository → ClienteRepository`

**`ui/navigation/NavGraph.kt`**: reemplazar los `PlaceholderScreen` de Clientes y AgregarCliente con las screens reales

---

## Decisiones de arquitectura tomadas

- El filtrado de búsqueda va en el **ViewModel** con `combine()`, no en la UI
- El ViewModel sigue el **mismo patrón que `OrdenesScreen.kt`**: ViewModel + UiState + Screen en un solo archivo
- El campo **Supervisor** del formulario es un dropdown/selector (los supervisores están en `FakeMockData.supervisores`), no un campo de texto libre
- **No se usa debounce** por simplicidad (se puede agregar después con `debounce(300)` en el flow)
- El avatar de iniciales se calcula tomando las primeras letras de las primeras dos palabras del nombre

---

## Colores relevantes (del Figma)

```kotlin
val AvatarBlue = Color(0xFF1565C0)      // avatar de clientes
val CardBorder = Color(0xFFBDBDBD)      // borde de cards
val SearchBarBg = Color(0xFFD5D5D5)     // fondo del buscador
val TextSecondary = Color(0xFF3D3D3D)   // texto dirección/teléfono/contador
val CardTitle = Color(0xFF1A1C1E)       // nombre del consorcio
val ScreenBg = Color(0xFFF2F2F7)        // fondo general
```
