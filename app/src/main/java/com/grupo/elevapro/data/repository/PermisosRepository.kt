package com.grupo.elevapro.data.repository

import com.grupo.elevapro.data.model.domain.Permiso
import com.grupo.elevapro.data.model.domain.Rol
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

interface PermisosRepository {
    fun observarPermisos(usuarioId: String): Flow<Set<Permiso>>
    suspend fun actualizar(usuarioId: String, permisos: Set<Permiso>)
    fun defaultsPara(rol: Rol): Set<Permiso>
}

@Singleton
class FakePermisosRepository @Inject constructor() : PermisosRepository {

    private val _permisos = MutableStateFlow(FakeMockData.permisos)

    override fun observarPermisos(usuarioId: String): Flow<Set<Permiso>> =
        _permisos.map { it[usuarioId] ?: emptySet() }

    override suspend fun actualizar(usuarioId: String, permisos: Set<Permiso>) {
        _permisos.update { it + (usuarioId to permisos) }
    }

    override fun defaultsPara(rol: Rol): Set<Permiso> = when (rol) {
        Rol.OPERATIVO -> setOf(
            Permiso.VER_ORDENES, Permiso.CREAR_ORDENES, Permiso.FIRMAR_ORDENES,
            Permiso.VER_CLIENTES,
        )
        Rol.ADMINISTRADOR -> enumValues<Permiso>().toSet()
    }
}
