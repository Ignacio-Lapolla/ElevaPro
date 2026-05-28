package com.grupo.elevapro.data.repository

import com.grupo.elevapro.data.model.domain.Articulo
import com.grupo.elevapro.data.model.domain.Cliente
import com.grupo.elevapro.data.model.domain.CondicionIva
import com.grupo.elevapro.data.model.domain.Empresa
import com.grupo.elevapro.data.model.domain.Especialidad
import com.grupo.elevapro.data.model.domain.EstadoFactura
import com.grupo.elevapro.data.model.domain.Factura
import com.grupo.elevapro.data.model.domain.Orden
import com.grupo.elevapro.data.model.domain.Permiso
import com.grupo.elevapro.data.model.domain.Plantilla
import com.grupo.elevapro.data.model.domain.Rol
import com.grupo.elevapro.data.model.domain.Supervisor
import com.grupo.elevapro.data.model.domain.Usuario

object FakeMockData {

    val clientes: List<Cliente> = listOf(
        Cliente("1", "Cons. Prop. Lafinur 3060", "Lafinur 3060", "+54 11 4801-1234", "admin@lafinur3060.com.ar", "30-65432189-1"),
        Cliente("2", "Cons. Prop. Av. San Juan 3967", "Av. San Juan 3967", "+54 11 4923-5678", "consorcio@sanjuan3967.com.ar", "30-71234567-9"),
        Cliente("3", "Cons. Prop. Cerviño 3735", "Cerviño 3735", "+54 11 4801-9012", "admin@cervino3735.com.ar", "30-69876543-2"),
        Cliente("4", "Cons. Prop. Padilla 667", "Padilla 667", "+54 11 4856-3456", "contacto@padilla667.com.ar", "30-62345678-3"),
        Cliente("5", "Cons. Prop. Charcas 4298", "Charcas 4298", "+54 11 4778-7890", "info@charcas4298.com.ar", "30-58765432-4"),
        Cliente("6", "Cons. Prop. Av. Libertador 5102", "Av. Libertador 5102", "+54 11 4811-2345", "admin@libertador5102.com.ar", "30-66789012-5"),
        Cliente("7", "Cons. Prop. Juncal 3150", "Juncal 3150", "+54 11 4824-6789", "consorcio@juncal3150.com.ar", "30-59276609-0"),
        Cliente("8", "Cons. Prop. Charlone 349", "Charlone 349", "+54 11 4554-1122", "admin@charlone349.com.ar", "30-67890123-6"),
        Cliente("9", "Cons. Prop. Virrey Arredondo 2698", "Virrey Arredondo 2698", "+54 11 4783-3344", "contacto@arredondo2698.com.ar", "30-64567891-7"),
        Cliente("10", "Cons. Prop. Pueyrredon 740", "Pueyrredon 740", "+54 11 4961-5566", "admin@pueyrredon740.com.ar", "30-72345678-8"),
        Cliente("11", "Cons. Prop. Valentín Gomez 2711", "Valentín Gomez 2711", "+54 11 4865-7788", "consorcio@vgomez2711.com.ar", "30-68901234-9"),
    )

    val supervisores: List<Supervisor> = listOf(
        Supervisor("s1", "Carlos Méndez", "+54 11 5555-1001", "c.mendez@elevapro.com.ar", Especialidad.PREVENTIVO),
        Supervisor("s2", "Lucía Fernández", "+54 11 5555-1002", "l.fernandez@elevapro.com.ar", Especialidad.EMERGENCIA),
        Supervisor("s3", "Roberto Sosa", "+54 11 5555-1003", "r.sosa@elevapro.com.ar", Especialidad.INSTALACIONES),
        Supervisor("s4", "Marina Vega", "+54 11 5555-1004", "m.vega@elevapro.com.ar", Especialidad.MODERNIZACION),
    )

    val plantillas: List<Plantilla> = listOf(
        Plantilla(
            "p1", "Mantenimiento Preventivo Mensual", "Inspección general de seguridad y limpieza", "Preventivo",
            tareas = listOf(
                "Revisión de cables de tracción",
                "Lubricación de guías",
                "Verificación de paradas en cada piso",
                "Test de botonera y cabina",
                "Limpieza de foso",
                "Control de iluminación de emergencia",
            ),
            tiempoEstimadoMin = 90,
        ),
        Plantilla(
            "p2", "Reparación de Emergencia", "Atención de fallas urgentes", "Emergencia",
            tareas = listOf(
                "Diagnóstico inicial",
                "Bloqueo y aviso",
                "Reparación o reemplazo de pieza",
                "Prueba de funcionamiento",
                "Reporte de causa raíz",
            ),
            tiempoEstimadoMin = 180,
        ),
        Plantilla(
            "p3", "Inspección Anual Reglamentaria", "Inspección obligatoria GCBA", "Preventivo",
            tareas = listOf(
                "Verificación de frenos",
                "Test de paracaídas",
                "Inspección de poleas y motor",
                "Control de puertas automáticas",
                "Confección de acta",
            ),
            tiempoEstimadoMin = 240,
        ),
        Plantilla(
            "p4", "Modernización de Cabina", "Renovación estética e instalaciones", "Modernización",
            tareas = listOf(
                "Desmontaje de cabina",
                "Reemplazo de iluminación LED",
                "Instalación de botonera nueva",
                "Pintura y terminaciones",
                "Pruebas finales",
            ),
            tiempoEstimadoMin = 480,
        ),
        Plantilla(
            "p5", "Limpieza de Foso", "Limpieza y desinfección de foso", "Preventivo",
            tareas = listOf("Bloqueo del equipo", "Aspirado", "Lavado con detergente", "Verificación de bomba de achique"),
            tiempoEstimadoMin = 45,
        ),
        Plantilla(
            "p6", "Instalación Nueva", "Montaje completo de ascensor nuevo", "Instalaciones",
            tareas = listOf(
                "Replanteo de hueco",
                "Montaje de guías",
                "Instalación de motor",
                "Cableado eléctrico",
                "Habilitación municipal",
            ),
            tiempoEstimadoMin = 2400,
        ),
    )

    val ordenes: List<Orden> = listOf(
        Orden("o1", "OT-0001", "11", "Cons. Prop. Valentín Gomez 2711", "26 mar 2026", "Mantenimiento Preventivo", "p1", "Equipo principal con vibración leve", firmada = true, nombreFirmante = "Juan García"),
        Orden("o2", "OT-0002", "3", "Cons. Prop. Cerviño 3735", "27 mar 2026", "Reparación de Emergencia", "p2", "Puerta de PB no cierra", firmada = false),
        Orden("o3", "OT-0003", "7", "Cons. Prop. Juncal 3150", "28 mar 2026", "Inspección Anual Reglamentaria", "p3", "Inspección anual GCBA", firmada = true, nombreFirmante = "María López"),
        Orden("o4", "OT-0004", "1", "Cons. Prop. Lafinur 3060", "01 abr 2026", "Mantenimiento Preventivo", "p1", "", firmada = false),
        Orden("o5", "OT-0005", "5", "Cons. Prop. Charcas 4298", "03 abr 2026", "Limpieza de Foso", "p5", "Foso con agua de filtración", firmada = false),
        Orden("o6", "OT-0006", "9", "Cons. Prop. Virrey Arredondo 2698", "05 abr 2026", "Mantenimiento Preventivo", "p1", "", firmada = true, nombreFirmante = "Pedro Sosa"),
        Orden("o7", "OT-0007", "2", "Cons. Prop. Av. San Juan 3967", "08 abr 2026", "Reparación de Emergencia", "p2", "Botonera de PB fuera de servicio", firmada = false),
        Orden("o8", "OT-0008", "6", "Cons. Prop. Av. Libertador 5102", "10 abr 2026", "Modernización de Cabina", "p4", "Renovación completa, 3 cabinas", firmada = false),
        Orden("o9", "OT-0009", "10", "Cons. Prop. Pueyrredon 740", "12 abr 2026", "Mantenimiento Preventivo", "p1", "", firmada = true, nombreFirmante = "Laura Pérez"),
        Orden("o10", "OT-0010", "4", "Cons. Prop. Padilla 667", "15 abr 2026", "Inspección Anual Reglamentaria", "p3", "", firmada = false),
    )

    val articulos: List<Articulo> = listOf(
        Articulo("a1", "ASC-CAB-001", "Cable de tracción 8mm", "Cable de acero galvanizado 8mm x 50m", 145000.0, "Cables", 12),
        Articulo("a2", "ASC-BOT-002", "Botonera de cabina LED", "Panel completo con display segmentado", 85000.0, "Electrónica", 4),
        Articulo("a3", "ASC-MOT-003", "Motor reductor 7.5HP", "Motor trifásico para ascensor", 950000.0, "Motores", 1),
        Articulo("a4", "ASC-PUE-004", "Puerta automática piso", "Puerta de 800mm automática", 320000.0, "Puertas", 6),
        Articulo("a5", "ASC-FRE-005", "Pastilla de freno", "Juego de pastillas para freno electromagnético", 18500.0, "Frenos", 22),
        Articulo("a6", "ASC-ILU-006", "Iluminación LED cabina", "Tira LED 4000K 24V", 12500.0, "Iluminación", 35),
        Articulo("a7", "ASC-CON-007", "Contactor 40A", "Contactor trifásico 220V/40A", 24000.0, "Electricidad", 8),
        Articulo("a8", "ASC-GUI-008", "Guía T 90mm", "Perfil guía de acero T 90mm x 5m", 68000.0, "Estructura", 0),
        Articulo("a9", "ASC-POL-009", "Polea de tracción", "Polea fundida 600mm", 240000.0, "Mecánica", 2),
        Articulo("a10", "ASC-SEN-010", "Sensor de paro", "Sensor magnético tipo herradura", 4800.0, "Electrónica", 50),
    )

    val facturas: List<Factura> = listOf(
        Factura("f1", "0001-00000123", "A", "11", "Cons. Prop. Valentín Gomez 2711", "26 mar 2026", 145000.0, EstadoFactura.APROBADA, cae = "74125896301245", vencimientoCae = "05 abr 2026", ordenesIds = listOf("o1")),
        Factura("f2", "0001-00000124", "A", "3", "Cons. Prop. Cerviño 3735", "27 mar 2026", 320000.0, EstadoFactura.PENDIENTE, ordenesIds = listOf("o2")),
        Factura("f3", "0001-00000125", "A", "7", "Cons. Prop. Juncal 3150", "28 mar 2026", 480000.0, EstadoFactura.APROBADA, cae = "74125896301246", vencimientoCae = "07 abr 2026", ordenesIds = listOf("o3")),
        Factura("f4", "0001-00000126", "B", "9", "Cons. Prop. Virrey Arredondo 2698", "05 abr 2026", 85000.0, EstadoFactura.RECHAZADA, ordenesIds = listOf("o6")),
        Factura("f5", "0001-00000127", "A", "10", "Cons. Prop. Pueyrredon 740", "12 abr 2026", 145000.0, EstadoFactura.PENDIENTE, ordenesIds = listOf("o9")),
        Factura("f6", "0001-00000128", "A", "6", "Cons. Prop. Av. Libertador 5102", "20 abr 2026", 2850000.0, EstadoFactura.PENDIENTE, ordenesIds = listOf("o8")),
    )

    val usuarios: List<Usuario> = listOf(
        Usuario("u1", "Martin Gauna", "usuario", Rol.OPERATIVO, "123", "+54 11 4123-1111", null),
        Usuario("u2", "Juan Pérez", "juan.perez", Rol.OPERATIVO, "123", "+54 11 4123-2222", null),
        Usuario("u3", "María García", "maria.garcia", Rol.OPERATIVO, "123", "+54 11 4123-3333", null),
        Usuario("u4", "Laura Pérez", "admin", Rol.ADMINISTRADOR, "123", "+54 11 4123-4444", null),
        Usuario("u5", "Diego Ramírez", "admin.dramirez", Rol.ADMINISTRADOR, "123", "+54 11 4123-5555", null),
    )

    val empresa = Empresa(
        id = "e1",
        razonSocial = "ElevaPro S.R.L.",
        cuit = "30-71234567-0",
        direccion = "Av. Corrientes 1234, CABA",
        telefono = "+54 11 5000-1234",
        email = "admin@elevapro.com.ar",
        condicionIva = CondicionIva.RESPONSABLE_INSCRIPTO,
    )

    val permisos: Map<String, Set<Permiso>> = mapOf(
        "u1" to setOf(Permiso.VER_ORDENES, Permiso.CREAR_ORDENES, Permiso.FIRMAR_ORDENES, Permiso.VER_CLIENTES),
        "u2" to setOf(Permiso.VER_ORDENES, Permiso.CREAR_ORDENES, Permiso.FIRMAR_ORDENES, Permiso.VER_CLIENTES),
        "u3" to setOf(Permiso.VER_ORDENES, Permiso.CREAR_ORDENES, Permiso.FIRMAR_ORDENES, Permiso.VER_CLIENTES),
        "u4" to Permiso.entries.toSet(),
        "u5" to Permiso.entries.toSet(),
    )

    fun usuarioPorEmail(email: String): Usuario? {
        if (email.isBlank()) return null
        val normalizado = email.trim().lowercase()
        return usuarios.firstOrNull { it.email.lowercase() == normalizado }
            ?: usuarios.firstOrNull { normalizado.startsWith(it.email.lowercase()) }
    }
}
