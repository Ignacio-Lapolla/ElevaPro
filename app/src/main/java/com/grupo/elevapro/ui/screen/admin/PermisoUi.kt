package com.grupo.elevapro.ui.screen.admin

import com.grupo.elevapro.data.model.domain.Permiso

internal fun Permiso.label(): String = when (this) {
    Permiso.VER_ORDENES       -> "Ver órdenes"
    Permiso.CREAR_ORDENES     -> "Crear órdenes"
    Permiso.FIRMAR_ORDENES    -> "Firmar órdenes"
    Permiso.VER_CLIENTES      -> "Ver clientes"
    Permiso.EDITAR_CLIENTES   -> "Editar clientes"
    Permiso.VER_FACTURAS      -> "Ver facturas"
    Permiso.GENERAR_FACTURAS  -> "Generar facturas"
    Permiso.APROBAR_FACTURAS  -> "Aprobar facturas"
    Permiso.GESTIONAR_USUARIOS -> "Gestionar usuarios"
    Permiso.GESTIONAR_EMPRESA  -> "Gestionar empresa"
}

internal data class CategoriaPermisos(val nombre: String, val permisos: List<Permiso>)

internal val CATEGORIAS: List<CategoriaPermisos> = listOf(
    CategoriaPermisos("Órdenes",  listOf(Permiso.VER_ORDENES, Permiso.CREAR_ORDENES, Permiso.FIRMAR_ORDENES)),
    CategoriaPermisos("Clientes", listOf(Permiso.VER_CLIENTES, Permiso.EDITAR_CLIENTES)),
    CategoriaPermisos("Facturas", listOf(Permiso.VER_FACTURAS, Permiso.GENERAR_FACTURAS, Permiso.APROBAR_FACTURAS)),
    CategoriaPermisos("Admin",    listOf(Permiso.GESTIONAR_USUARIOS, Permiso.GESTIONAR_EMPRESA)),
)
