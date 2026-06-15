package com.grupo.elevapro.data.local.datasource

import com.grupo.elevapro.data.local.dao.PermisoDao
import com.grupo.elevapro.data.local.entity.PermisoEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PermisoLocalDataSource @Inject constructor(private val dao: PermisoDao) {
    fun observar(usuarioId: String): Flow<List<PermisoEntity>> = dao.observar(usuarioId)
    suspend fun insertar(permisos: List<PermisoEntity>) = dao.insertar(permisos)
    suspend fun eliminarDeUsuario(usuarioId: String) = dao.eliminarDeUsuario(usuarioId)
    suspend fun existeAlguno(usuarioId: String): Boolean = dao.existeAlguno(usuarioId)
}
