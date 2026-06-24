# ElevaPro

Aplicación Android nativa para la gestión digital de órdenes de trabajo de técnicos de ascensores.

Reemplaza planillas de papel, fotos por WhatsApp y Excels por un flujo digital completo: el técnico crea la orden desde el celular, captura la firma del cliente en pantalla y el administrador genera la factura electrónica ARCA sin reingresar datos. Funciona sin conexión, ideal para salas de máquinas y subsuelos.

---

## Stack

| Capa | Tecnología | Versión |
|---|---|---|
| Lenguaje | Kotlin | 2.2.10 |
| UI | Jetpack Compose + Material 3 | BOM 2024.09.00 |
| Arquitectura | MVVM + Repository + UDF | — |
| Inyección de dependencias | Hilt | 2.51.1 |
| Navegación | Navigation Compose | 2.8.0 |
| Persistencia local | Room | — |
| Imágenes | Coil | 2.6.0 |
| Build | Kotlin DSL + AGP 9.1.1 | — |
| minSdk / targetSdk | API 27 / API 36 | Android 8.1 / 16 |

---

## Cómo correr el proyecto

1. Clonar el repositorio
   ```
   git clone https://github.com/<org>/ElevaPro.git
   ```
2. Abrir en **Android Studio Meerkat** o superior
3. Esperar a que Gradle sincronice las dependencias
4. Correr en un emulador o dispositivo físico con Android 8.1+
   - `Run → Run 'app'` o `Shift+F10`

No se requiere configuración de backend ni variables de entorno. La app funciona de forma autónoma con datos locales.

---

## Credenciales de prueba

| Número de empresa | Email / Handle | Contraseña | Rol |
|---|---|---|---|
| cualquier valor | `admin` | cualquier valor | Administrador |
| cualquier valor | `admin.dramirez` | cualquier valor | Administrador |
| cualquier valor | `usuario` | cualquier valor | Operativo |
| cualquier valor | `juan.perez` | cualquier valor | Operativo |

El campo contraseña acepta cualquier valor no vacío.

---

## Roles y acceso

| Módulo | Operativo | Administrador |
|---|---|---|
| Órdenes de trabajo | ✅ | ✅ |
| Clientes | ✅ | ✅ |
| Artículos | ✅ | ✅ |
| Perfil | ✅ | ✅ |
| Facturación | ❌ | ✅ |
| Administración (usuarios, roles, empresa) | ❌ | ✅ |

---

## Estructura del proyecto

```
app/src/main/java/com/grupo/elevapro/
├── data/
│   ├── model/domain/     ← Modelos de dominio (Orden, Cliente, Factura, ...)
│   └── repository/       ← Interfaces + implementaciones (Room / Fake)
├── di/
│   ├── AppModule.kt      ← @Binds de repositorios
│   ├── DatabaseModule.kt ← Room database
│   └── NetworkModule.kt  ← Retrofit stub (H3)
└── ui/
    ├── theme/            ← Color, Type, Theme (Material 3)
    ├── components/       ← MD3Components, BottomNav, DrawerMenu
    ├── navigation/       ← Screen.kt (sealed class), NavGraph.kt
    └── screen/
        ├── auth/         ← Onboarding, Login
        ├── ordenes/      ← Lista, Detalle, Nueva orden, Firma
        ├── clientes/     ← Lista, Detalle, Agregar
        ├── facturacion/  ← Lista, Detalle, Generar, Nueva
        ├── articulos/    ← Lista, agregar artículo
        ├── admin/        ← Usuarios, Roles, Supervisores, Empresa
        └── perfil/       ← Perfil, Notificaciones, Configuración, Ayuda
```

---

## Documentación

| Documento | Descripción |
|---|---|
| [Casos de uso](docs/casos_de_uso.md) | HU-01→HU-07: actores, flujos principales, flujos alternativos, postcondiciones |
| [Entrega H2](docs/H2_entrega.md) | Persistencia Room, nuevas features, ciclo de vida, tests, limitaciones |
| [Diagramas de secuencia](docs/diagramas_secuencia.md) | 6 flujos principales en Mermaid |
| [Medición cold start](docs/cold_start_medicion.md) | Métodos de medición, script ADB, requisito <2.5s |
| [Figma](https://www.figma.com/design/jyNGLF4qhkJ9JGUdCEWJQI/ElevaPro-figma) | Diseño completo, 25 pantallas, Material 3 |

---

## Equipo

| Dev | Integrante | Módulos |
|---|---|---|
| Dev 1 | Ignacio Lapolla | Estructura, autenticación, órdenes |
| Dev 2 | Jonathan Dominguez | Clientes, facturación |
| Dev 3 | Federico Isla | Administración |
| Dev 4 | Agustín Pérez | Perfil, artículos |

UADE 2026 · Tecnología de Aplicaciones para Dispositivos Móviles
