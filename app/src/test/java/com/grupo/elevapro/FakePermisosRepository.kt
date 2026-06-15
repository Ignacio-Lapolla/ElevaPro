package com.grupo.elevapro

import com.grupo.elevapro.data.model.domain.Permiso
import com.grupo.elevapro.data.model.domain.Rol
import com.grupo.elevapro.data.model.domain.permisosDefault
import com.grupo.elevapro.data.repository.PermisosRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

class FakePermisosRepository : PermisosRepository {

    private val _permisos = MutableStateFlow<Map<String, Set<Permiso>>>(emptyMap())

    override fun observarPermisos(usuarioId: String): Flow<Set<Permiso>> =
        _permisos.map { it[usuarioId] ?: emptySet() }

    override suspend fun actualizar(usuarioId: String, permisos: Set<Permiso>) {
        _permisos.update { it + (usuarioId to permisos) }
    }

    override suspend fun inicializarSiVacio(usuarioId: String, rol: Rol) {
        if (_permisos.value[usuarioId] == null) {
            actualizar(usuarioId, rol.permisosDefault)
        }
    }
}
