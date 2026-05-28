package com.grupo.elevapro.data.repository

import com.grupo.elevapro.data.model.domain.Permiso
import com.grupo.elevapro.data.model.domain.Rol
import com.grupo.elevapro.data.model.domain.permisosDefault
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

interface PermisosRepository {
    fun observarPermisos(usuarioId: String): Flow<Set<Permiso>>
    suspend fun actualizar(usuarioId: String, permisos: Set<Permiso>)
    fun observarDefaultsRol(rol: Rol): Flow<Set<Permiso>>
    suspend fun actualizarDefaultsRol(rol: Rol, permisos: Set<Permiso>)
}

@Singleton
class FakePermisosRepository @Inject constructor() : PermisosRepository {

    private val _permisos = MutableStateFlow(FakeMockData.permisos)

    private val _defaultsRol = MutableStateFlow(
        mapOf(
            Rol.ADMINISTRADOR to Rol.ADMINISTRADOR.permisosDefault,
            Rol.OPERATIVO     to Rol.OPERATIVO.permisosDefault,
        )
    )

    override fun observarPermisos(usuarioId: String): Flow<Set<Permiso>> =
        _permisos.map { it[usuarioId] ?: emptySet() }

    override suspend fun actualizar(usuarioId: String, permisos: Set<Permiso>) {
        _permisos.update { it + (usuarioId to permisos) }
    }

    override fun observarDefaultsRol(rol: Rol): Flow<Set<Permiso>> =
        _defaultsRol.map { it[rol] ?: emptySet() }

    override suspend fun actualizarDefaultsRol(rol: Rol, permisos: Set<Permiso>) {
        _defaultsRol.update { it + (rol to permisos) }
    }
}
