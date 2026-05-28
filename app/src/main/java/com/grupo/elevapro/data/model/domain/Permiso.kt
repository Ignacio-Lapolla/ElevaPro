package com.grupo.elevapro.data.model.domain

enum class Permiso {
    VER_ORDENES, CREAR_ORDENES, FIRMAR_ORDENES,
    VER_CLIENTES, EDITAR_CLIENTES,
    VER_FACTURAS, GENERAR_FACTURAS, APROBAR_FACTURAS,
    GESTIONAR_USUARIOS, GESTIONAR_EMPRESA,
}

val Rol.permisosDefault: Set<Permiso>
    get() = when (this) {
        Rol.OPERATIVO -> setOf(
            Permiso.VER_ORDENES, Permiso.CREAR_ORDENES, Permiso.FIRMAR_ORDENES,
            Permiso.VER_CLIENTES,
        )
        Rol.ADMINISTRADOR -> Permiso.entries.toSet()
    }
