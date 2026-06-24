# Casos de Uso — ElevaPro

**Materia:** Tecnología de Aplicaciones para Dispositivos Móviles  
**Institución:** Universidad Argentina de la Empresa (UADE)  
**Año:** 2026

---

## Índice

| ID | Nombre | Actor principal |
|---|---|---|
| [HU-01](#hu-01-autenticarse-en-el-sistema) | Autenticarse en el sistema | Operativo / Administrador |
| [HU-02](#hu-02-crear-orden-de-trabajo) | Crear orden de trabajo | Operativo |
| [HU-03](#hu-03-registrar-firma-de-conformidad) | Registrar firma de conformidad del cliente | Operativo |
| [HU-04](#hu-04-adjuntar-foto-a-una-orden) | Adjuntar foto a una orden de trabajo | Operativo |
| [HU-05](#hu-05-gestionar-clientes) | Gestionar clientes | Operativo / Administrador |
| [HU-06](#hu-06-generar-factura-electrónica) | Generar factura electrónica | Administrador |
| [HU-07](#hu-07-gestionar-permisos-de-usuario) | Gestionar permisos de usuario | Administrador |

---

## HU-01: Autenticarse en el sistema

**Actor principal:** Operativo / Administrador  
**Pantallas involucradas:** `OnboardingScreen` → `LoginScreen` → Home

**Precondiciones:**
- La aplicación está instalada en el dispositivo.
- El usuario conoce el número de empresa, su email (handle) y contraseña.

**Flujo principal:**
1. El usuario abre la app por primera vez y ve el onboarding (3 slides informativos).
2. En el último slide presiona "Comenzar" o "Ya tenés cuenta".
3. El sistema muestra `LoginScreen` con los campos: número de empresa, correo electrónico y contraseña.
4. El usuario completa los tres campos y presiona "Ingresar".
5. `LoginViewModel` delega la validación a `AuthRepository`.
6. `AuthRepository` verifica las credenciales y devuelve el `Usuario` con su rol (Operativo o Administrador).
7. El sistema inicializa los permisos del usuario en `PermisosRepository`.
8. El sistema navega al Home limpiando el back stack (el botón Atrás no regresa al Login).
9. El `BottomNavigationBar` se adapta al rol del usuario:
   - **Operativo:** Órdenes · Clientes · Artículos · Perfil
   - **Administrador:** Órdenes · Clientes · Facturación · Perfil (+ drawer con Administración)

**Flujos alternativos:**
- *Credenciales incorrectas:* `AuthRepository` devuelve error → `LoginViewModel` emite estado `Error` → se muestra mensaje en pantalla, los campos se mantienen.
- *Contraseña vacía:* el botón "Ingresar" permanece deshabilitado hasta que los tres campos tengan contenido.

**Postcondiciones:**
- El usuario está autenticado y accede al módulo correspondiente a su rol.
- El back stack no contiene `LoginScreen`.

---

## HU-02: Crear orden de trabajo

**Actor principal:** Operativo  
**Pantallas involucradas:** `OrdenesScreen` → `NuevaOrdenScreen` → `OrdenDetalleScreen`

**Precondiciones:**
- El usuario está autenticado como Operativo.
- Existe al menos un cliente registrado en el sistema.

**Flujo principal:**
1. El usuario navega a la pestaña "Órdenes" y presiona el botón flotante "+".
2. El sistema muestra `NuevaOrdenScreen` con los campos: cliente (dropdown), tipo de trabajo (dropdown), plantilla de tarea (dropdown, opcional) y observaciones.
3. El usuario selecciona un cliente del listado.
4. El usuario selecciona el tipo de trabajo:
   - Mantenimiento Preventivo
   - Reparación de Emergencia
   - Inspección Anual Reglamentaria
   - Modernización de Cabina
   - Limpieza de Foso
   - Instalación Nueva
5. El usuario selecciona opcionalmente una plantilla de tareas predefinida.
6. El usuario agrega observaciones opcionales y presiona "Crear orden".
7. `NuevaOrdenViewModel` genera un UUID, asigna la fecha actual y persiste la orden en Room mediante `OrdenesRepository`.
8. El sistema navega automáticamente a `OrdenDetalleScreen` con la orden recién creada.

**Flujos alternativos:**
- *Cliente o tipo no seleccionados:* el botón "Crear orden" permanece deshabilitado.
- *Error de persistencia:* el sistema muestra un `Snackbar` con el mensaje de error y permanece en `NuevaOrdenScreen`.

**Postcondiciones:**
- La orden queda persistida en Room con estado "Pendiente".
- La orden aparece en la lista de `OrdenesScreen`.
- El usuario puede continuar el flujo desde `OrdenDetalleScreen`.

---

## HU-03: Registrar firma de conformidad del cliente

**Actor principal:** Operativo (con el cliente físicamente presente)  
**Pantallas involucradas:** `OrdenDetalleScreen` → `FirmaScreen` → `OrdenDetalleScreen`

**Precondiciones:**
- Existe una orden de trabajo con estado "Pendiente" (no firmada).
- El usuario está autenticado como Operativo.

**Flujo principal:**
1. El usuario accede a `OrdenDetalleScreen` de la orden correspondiente.
2. El usuario presiona "Solicitar firma".
3. El sistema navega a `FirmaScreen` mostrando el nombre del cliente y el número de orden.
4. El cliente traza su firma en el canvas táctil usando el dedo.
5. El cliente ingresa su nombre en el campo "Nombre del firmante".
6. El usuario presiona "Confirmar firma".
7. `FirmaViewModel` serializa el canvas a Base64 y llama a `OrdenesRepository.firmar()`.
8. `OrdenesRepository` actualiza la `OrdenEntity` en Room con `firmada = true` y la firma en Base64.
9. El sistema navega de regreso a `OrdenDetalleScreen` mostrando la orden en estado "Firmada".

**Flujos alternativos:**
- *Firma incorrecta:* el cliente presiona el ícono de limpieza (↺) para borrar el canvas y volver a trazar.
- *Nombre vacío o sin trazos:* el botón "Confirmar firma" permanece deshabilitado hasta que haya trazos en el canvas y el nombre no esté vacío.
- *El usuario cancela:* presiona Atrás, el `BackHandler` cierra la pantalla sin modificar la orden.

**Postcondiciones:**
- La orden queda persistida en Room con `firmada = true`.
- El estado visible en `OrdenDetalleScreen` cambia a "Firmada".
- La orden queda disponible para facturación por el Administrador.

---

## HU-04: Adjuntar foto a una orden de trabajo

**Actor principal:** Operativo  
**Pantallas involucradas:** `OrdenDetalleScreen` (acción inline)

**Precondiciones:**
- Existe una orden de trabajo en el sistema.
- El dispositivo tiene cámara disponible.

**Flujo principal:**
1. El usuario accede a `OrdenDetalleScreen` y presiona "Adjuntar foto".
2. El sistema solicita el permiso `CAMERA` en runtime mediante `ActivityResultContracts.RequestPermission()`.
3. El usuario concede el permiso.
4. El sistema abre la cámara nativa mediante `ActivityResultContracts.TakePicture()`.
5. El usuario captura la foto.
6. El callback de `ActivityResult` devuelve `true` (foto guardada).
7. `OrdenDetalleScreen` recupera la URI desde `rutaRef` (almacenada en `mutableStateOf` con `remember` para evitar race condition).
8. `OrdenesViewModel.adjuntarFoto()` actualiza la entidad en Room con la URI de la foto.
9. La miniatura de la foto aparece en `OrdenDetalleScreen`.

**Flujos alternativos:**
- *Permiso denegado:* el sistema muestra un `Snackbar` "Permiso de cámara requerido" y no abre la cámara.
- *El usuario cancela la cámara:* el callback devuelve `false`, la orden no se modifica.

**Postcondiciones:**
- La URI de la foto queda persistida en la `OrdenEntity` de Room.
- La miniatura es visible en el detalle de la orden.

---

## HU-05: Gestionar clientes

**Actor principal:** Operativo / Administrador  
**Pantallas involucradas:** `ClientesScreen` → `AgregarClienteScreen` / `ClienteDetalleScreen`

**Precondiciones:**
- El usuario está autenticado (cualquier rol).

### HU-05a: Agregar cliente

**Flujo principal:**
1. El usuario navega a la pestaña "Clientes" y presiona el botón flotante "+".
2. El sistema muestra `AgregarClienteScreen` con los campos: nombre, dirección, teléfono, email, código de edificio, notas y supervisor asignado (dropdown).
3. El usuario completa los campos obligatorios (nombre es requerido) y selecciona un supervisor del listado.
4. El usuario presiona "Guardar".
5. `AgregarClienteViewModel` genera un UUID y persiste el cliente en Room mediante `ClienteRepository`.
6. El sistema navega de regreso a `ClientesScreen` con el nuevo cliente en la lista.

**Flujos alternativos:**
- *Nombre vacío:* el botón "Guardar" permanece deshabilitado.

### HU-05b: Eliminar cliente

**Flujo principal:**
1. El usuario accede a `ClienteDetalleScreen` del cliente.
2. El usuario presiona el ícono de eliminar (🗑).
3. El sistema muestra un diálogo de confirmación "¿Eliminar cliente?".
4. El usuario confirma.
5. `ClienteRepository` elimina el `ClienteEntity` de Room.
6. El sistema navega de regreso a `ClientesScreen`.

**Flujos alternativos:**
- *El usuario cancela el diálogo:* el cliente no se elimina.

**Postcondiciones:**
- El cliente queda persistido en Room y es visible en la lista.
- El cliente queda disponible para ser asociado a nuevas órdenes.

---

## HU-06: Generar factura electrónica

**Actor principal:** Administrador  
**Pantallas involucradas:** `FacturacionScreen` → `GenerarFacturaScreen` → `FacturacionScreen`

**Precondiciones:**
- El usuario está autenticado como Administrador.
- Existe al menos un cliente registrado en el sistema.
- Existe al menos una orden firmada y aún no facturada.

**Flujo principal:**
1. El usuario navega a la pestaña "Facturación" y presiona "Nueva factura".
2. El sistema muestra `GenerarFacturaScreen`. En paralelo, `GenerarFacturaViewModel` carga el listado de clientes desde `ClienteRepository` y las órdenes disponibles desde `OrdenesRepository`.
3. El usuario selecciona:
   - Cliente del dropdown
   - Orden de trabajo asociada (opcional)
   - Tipo de comprobante (Factura A / B / C)
   - Fecha de emisión y fecha de vencimiento
   - Monto neto
4. El sistema calcula automáticamente el IVA (21%) y muestra el total en tiempo real.
5. El usuario presiona "Generar factura".
6. `GenerarFacturaViewModel` valida el formulario y llama a `FacturacionRepository.agregar()`.
7. La factura queda registrada con estado "Pendiente".
8. El sistema navega de regreso a `FacturacionScreen` mostrando la nueva factura en la lista.

**Flujos alternativos:**
- *Formulario incompleto:* el botón "Generar factura" permanece deshabilitado hasta que cliente, tipo y monto neto estén completados.
- *Monto neto inválido:* el campo no acepta texto no numérico; el total se muestra como $0,00 hasta ingresar un valor válido.

**Postcondiciones:**
- La factura queda registrada en `FacturacionRepository` con estado "Pendiente".
- La factura aparece en la lista de `FacturacionScreen`.

> **Nota técnica:** en H2, `FacturacionRepository` es una implementación Fake (datos en memoria). La conexión real con el servicio ARCA de AFIP está planificada para H3 mediante el stub `ArcaApiService` ya creado en `NetworkModule`.

---

## HU-07: Gestionar permisos de usuario

**Actor principal:** Administrador  
**Pantallas involucradas:** Drawer → `UsuariosScreen` → `UsuarioPermisosScreen`

**Precondiciones:**
- El usuario está autenticado como Administrador.
- Existen usuarios registrados en el sistema.

**Flujo principal:**
1. El Administrador abre el `ModalNavigationDrawer` y navega a "Administración → Usuarios".
2. El sistema muestra `UsuariosScreen` con la lista de usuarios y sus roles actuales.
3. El Administrador selecciona un usuario.
4. El sistema muestra `UsuarioPermisosScreen` con:
   - Tarjeta resumen del usuario (nombre, email, rol)
   - Lista de permisos individuales agrupados por categoría, cada uno con un `Switch`
5. El Administrador activa o desactiva los permisos deseados mediante los switches.
6. El Administrador presiona "Guardar cambios".
7. `UsuarioPermisosViewModel` persiste los permisos modificados en Room mediante `PermisosRepository`.
8. El sistema navega de regreso a `UsuariosScreen`.

**Flujos alternativos:**
- *Sin cambios realizados:* al presionar "Guardar cambios" el sistema navega hacia atrás sin realizar escrituras en Room.
- *Usuario con rol Administrador:* la tarjeta resumen muestra el chip "Admin" en verde; los permisos pueden ser igualmente modificados.

**Postcondiciones:**
- Los permisos del usuario quedan persistidos en Room mediante `PermisoEntity`.
- Los cambios afectan el acceso del usuario en su próxima sesión.

---

## Relación entre casos de uso

```
[HU-01 Login] ──────────────────────────────────────────────────────┐
                                                                     │
    ┌── Operativo ───────────────────────────────────────────────────┤
    │   [HU-02 Crear orden]                                          │
    │         └──► [HU-03 Firma]      (requiere orden existente)     │
    │         └──► [HU-04 Foto]       (requiere orden existente)     │
    │   [HU-05 Gestionar clientes]                                   │
    │                                                                │
    └── Administrador ───────────────────────────────────────────────┤
        [HU-06 Generar factura]       (requiere orden firmada)       │
        [HU-07 Gestionar permisos]                                   │
        [HU-02, HU-03, HU-04, HU-05] (acceso completo)             │
                                                                     ┘
```

---

*ElevaPro — Casos de Uso · UADE 2026*
