package com.grupo.elevapro.data.repository

import com.grupo.elevapro.data.local.datasource.PermisoLocalDataSource
import com.grupo.elevapro.data.local.entity.PermisoEntity
import com.grupo.elevapro.data.model.domain.Permiso
import com.grupo.elevapro.data.model.domain.Rol
import com.grupo.elevapro.data.model.domain.permisosDefault
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

interface PermisosRepository {
    fun observarPermisos(usuarioId: String): Flow<Set<Permiso>>
    suspend fun actualizar(usuarioId: String, permisos: Set<Permiso>)
    suspend fun inicializarSiVacio(usuarioId: String, rol: Rol)
}

@Singleton
class RoomPermisosRepository @Inject constructor(
    private val local: PermisoLocalDataSource,
) : PermisosRepository {

    private val initMutex = Mutex()

    override fun observarPermisos(usuarioId: String): Flow<Set<Permiso>> =
        local.observar(usuarioId).map { entities ->
            entities.mapNotNull { runCatching { Permiso.valueOf(it.permiso) }.getOrNull() }.toSet()
        }

    override suspend fun actualizar(usuarioId: String, permisos: Set<Permiso>) {
        local.eliminarDeUsuario(usuarioId)
        local.insertar(permisos.map { permiso ->
            PermisoEntity(
                id = "$usuarioId:${permiso.name}",
                usuarioId = usuarioId,
                permiso = permiso.name,
            )
        })
    }

    override suspend fun inicializarSiVacio(usuarioId: String, rol: Rol) {
        initMutex.withLock {
            if (!local.existeAlguno(usuarioId)) {
                actualizar(usuarioId, rol.permisosDefault)
            }
        }
    }
}
