# ElevaPro — Checklist Heurísticas de Nielsen

Evidencia de aplicación de las 10 heurísticas de usabilidad de Nielsen (NN Group) en ElevaPro.

---

## H1 — Visibilidad del estado del sistema

> El sistema debe mantener al usuario informado sobre lo que está ocurriendo, mediante feedback apropiado en tiempo razonable.

| Evidencia | Implementación |
|-----------|----------------|
| Estados de carga | `CircularProgressIndicator` en todas las pantallas mientras se obtienen datos (`UiState.Loading`) |
| Modo offline visible | Banner `SinConexion` persistente sobre los datos cacheados en `OrdenesScreen` |
| Guardando en formularios | Campo `generando: Boolean` en `GenerarFacturaFormState` deshabilita el botón y muestra progreso |
| Snackbar de feedback | Confirmación de descarga de PDF en `OrdenDetalleScreen` |
| Estado de firma | `FirmaScreen` muestra canvas vacío vs firmado con feedback visual |

**✅ Cumple**

---

## H2 — Coincidencia entre el sistema y el mundo real

> El sistema debe hablar el idioma del usuario, con palabras, frases y conceptos familiares al usuario.

| Evidencia | Implementación |
|-----------|----------------|
| Terminología del dominio | "Orden de trabajo", "Técnico", "Ascensor", "CUIT", "Factura tipo A/B/C" |
| Íconos intuitivos | `LocationOn` para dirección, `Phone` para teléfono, `Inventory` para artículos |
| Formato de fechas | `dd/MM/yyyy` (estándar argentino) en formularios de facturación |
| Chips de estado | `TipoEstado` en órdenes usa labels del dominio: Pendiente, En progreso, Completada |
| Roles con nombres reales | "Operativo" y "Administrador" en lugar de IDs técnicos |

**✅ Cumple**

---

## H3 — Control y libertad del usuario

> Los usuarios eligen funciones por error y necesitan una "salida de emergencia" clara para abandonar el estado no deseado.

| Evidencia | Implementación |
|-----------|----------------|
| Navegación atrás | `BackHandler` intercepta el botón del sistema en overlays (PDF, firma) para cerrarlos sin salir de la pantalla |
| Cancelar en sheets | `ModalBottomSheet` de nuevo artículo/cliente se cierra con gesto de deslizar o botón cancelar |
| BottomNav siempre visible | El usuario puede cambiar de módulo en cualquier momento sin quedar "atrapado" |
| Limpiar búsqueda | Botón "Limpiar búsqueda" en estado vacío de `ClientesScreen` y `ArticulosScreen` |

**✅ Cumple**

---

## H4 — Consistencia y estándares

> Los usuarios no deberían preguntarse si diferentes palabras, situaciones o acciones significan lo mismo.

| Evidencia | Implementación |
|-----------|----------------|
| Design system unificado | `MD3Components.kt` centraliza `ElevaProTopAppBar`, `FilledPrimaryButton`, `StatusChip`, `InfoRow` |
| Paleta de colores | `Color.kt` con tokens semánticos (`Primary`, `Secondary`, `Error`) aplicados consistentemente |
| Tipografía | `AppTypography` con escala MD3 aplicada en toda la app via `MaterialTheme.typography` |
| Patrón UiState | `sealed interface UiState` con Loading/Success/Error en todos los ViewModels del equipo |
| FAB consistente | Siempre naranja (`tertiary`), siempre con ícono `Add`, siempre en esquina inferior derecha |

**✅ Cumple**

---

## H5 — Prevención de errores

> Mejor que un buen mensaje de error es un diseño cuidadoso que prevenga que el problema ocurra.

| Evidencia | Implementación |
|-----------|----------------|
| Validación de formularios | `isFormValid` en `AgregarClienteFormState` y `GenerarFacturaFormState` deshabilita el botón Guardar hasta completar campos requeridos |
| CUIT solo acepta dígitos | `onCuit()` filtra caracteres no numéricos y limita a 11 dígitos |
| Monto numérico | `onMontoNeto()` filtra a dígitos, coma y punto únicamente |
| Confirmación al eliminar | Diálogo de confirmación antes de eliminar un cliente |
| Permiso cámara en runtime | Se solicita permiso antes de abrir la cámara; fallback informativo si se deniega |

**✅ Cumple**

---

## H6 — Reconocimiento en lugar de recuerdo

> Minimizar la carga de memoria del usuario haciendo visibles objetos, acciones y opciones.

| Evidencia | Implementación |
|-----------|----------------|
| BottomNav con labels | Cada tab muestra ícono + label textual ("Órdenes", "Clientes", "Artículos", "Perfil") |
| Placeholder descriptivo | Campo de búsqueda muestra "Buscar por nombre o dirección…" como guía |
| Filtro por categoría visible | Chips de categoría en `ArticulosScreen` siempre visibles sin necesidad de menú oculto |
| Cards con info completa | Cada card de cliente muestra nombre, dirección y teléfono sin necesidad de abrir el detalle |
| Drawer con módulos admin | `ModalNavigationDrawer` lista todas las opciones del admin sin requerir memorización |

**✅ Cumple**

---

## H7 — Flexibilidad y eficiencia de uso

> Los aceleradores —invisibles para el usuario novato— pueden acelerar la interacción para el usuario experto.

| Evidencia | Implementación |
|-----------|----------------|
| Búsqueda en tiempo real | `ClientesScreen` y `ArticulosScreen` filtran mientras el usuario escribe |
| Filtro por categoría | `ArticulosScreen` permite filtrar por categoría con un tap en el chip |
| FAB como acceso directo | Acción principal (agregar) siempre accesible con un tap desde el listado |
| Drawer para módulos avanzados | Admin accede a módulos de gestión sin pasar por la navegación principal |
| Role-aware navigation | La app oculta módulos irrelevantes según el rol, reduciendo cognitive load |

**✅ Cumple**

---

## H8 — Diseño estético y minimalista

> Los diálogos no deben contener información irrelevante o raramente necesaria.

| Evidencia | Implementación |
|-----------|----------------|
| Material Design 3 | Jerarquía visual clara con tipografía MD3, elevación y color como señales |
| Cards con info esencial | Cada card muestra solo los datos necesarios para identificar el ítem |
| Íconos sin texto redundante | `BottomNav` usa label solo en el tab seleccionado (comportamiento MD3 por defecto) |
| Dark mode | Esquema oscuro completo reduce fatiga visual en uso nocturno o en campo |
| Sin decoración innecesaria | Ausencia de fondos texturizados, gradientes complejos o animaciones superfluas |

**✅ Cumple**

---

## H9 — Ayuda al usuario para reconocer, diagnosticar y recuperarse de errores

> Los mensajes de error deben expresarse en lenguaje llano, indicar el problema con precisión y sugerir una solución.

| Evidencia | Implementación |
|-----------|----------------|
| Mensajes de error en lenguaje llano | `UiState.Error` muestra mensajes como "No se pudo guardar el cliente" en lugar de códigos técnicos |
| Estado vacío descriptivo | Pantalla vacía muestra ícono + texto explicativo + botón de acción ("Limpiar búsqueda") |
| Snackbar de conectividad | Banner `SinConexion` informa claramente que la app está en modo offline |
| Error en formulario | Campo con error muestra mensaje inline sin desaparecer hasta que se corrige |
| Fallback permiso cámara | Si se deniega el permiso, `Snackbar` indica al usuario que debe habilitarlo en ajustes |

**✅ Cumple**

---

## H10 — Ayuda y documentación

> Aunque es mejor si el sistema puede usarse sin documentación, puede ser necesario proveer ayuda.

| Evidencia | Implementación |
|-----------|----------------|
| `AyudaSoporteScreen` | Pantalla dedicada en el módulo Perfil con preguntas frecuentes y contacto de soporte |
| Placeholders descriptivos | Todos los campos de formulario tienen placeholder que indica qué ingresar |
| `contentDescription` en iconos | Todos los íconos interactivos tienen `contentDescription` para lectores de pantalla (TalkBack) |
| Onboarding inicial | `OnboardingScreen` explica la propuesta de valor de la app al primer arranque |

**✅ Cumple**

---

## Resumen

| Heurística | Estado |
|------------|--------|
| H1 — Visibilidad del estado | ✅ |
| H2 — Coincidencia con el mundo real | ✅ |
| H3 — Control y libertad | ✅ |
| H4 — Consistencia y estándares | ✅ |
| H5 — Prevención de errores | ✅ |
| H6 — Reconocimiento en lugar de recuerdo | ✅ |
| H7 — Flexibilidad y eficiencia | ✅ |
| H8 — Diseño minimalista | ✅ |
| H9 — Recuperación de errores | ✅ |
| H10 — Ayuda y documentación | ✅ |

**Las 10 heurísticas de Nielsen están evidenciadas en la implementación de ElevaPro.**

---

*ElevaPro — UADE 2026*
