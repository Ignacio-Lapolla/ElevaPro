package com.grupo.elevapro.data.model.domain

enum class Permiso {
    VER_ORDENES, CREAR_ORDENES, FIRMAR_ORDENES,
    VER_CLIENTES, EDITAR_CLIENTES,
    VER_FACTURAS, GENERAR_FACTURAS, APROBAR_FACTURAS,
    GESTIONAR_USUARIOS, GESTIONAR_EMPRESA,
}

data class PermisosUsuario(val usuarioId: String, val permisos: Set<Permiso>)
