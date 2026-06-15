package com.grupo.elevapro.data.local.datasource

import com.grupo.elevapro.data.local.dao.OrdenDao
import com.grupo.elevapro.data.local.entity.OrdenEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OrdenLocalDataSource @Inject constructor(private val dao: OrdenDao) {
    fun observarTodas(): Flow<List<OrdenEntity>> = dao.observarTodas()
    suspend fun obtenerPorId(id: String): OrdenEntity? = dao.obtenerPorId(id)
    suspend fun insertar(ordenes: List<OrdenEntity>) = dao.insertar(ordenes)
    suspend fun insertarUna(orden: OrdenEntity) = dao.insertarUna(orden)
    suspend fun actualizar(orden: OrdenEntity) = dao.actualizar(orden)
    suspend fun contar(): Int = dao.contar()
}
