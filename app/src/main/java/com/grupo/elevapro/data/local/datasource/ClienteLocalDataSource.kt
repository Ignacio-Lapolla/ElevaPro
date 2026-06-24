package com.grupo.elevapro.data.local.datasource

import com.grupo.elevapro.data.local.dao.ClienteDao
import com.grupo.elevapro.data.local.entity.ClienteEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ClienteLocalDataSource @Inject constructor(private val dao: ClienteDao) {
    fun observarTodos(): Flow<List<ClienteEntity>> = dao.observarTodos()
    suspend fun obtenerPorId(id: String): ClienteEntity? = dao.obtenerPorId(id)
    suspend fun insertar(clientes: List<ClienteEntity>) = dao.insertar(clientes)
    suspend fun insertarUno(cliente: ClienteEntity) = dao.insertarUno(cliente)
    suspend fun actualizar(cliente: ClienteEntity) = dao.actualizar(cliente)
    suspend fun eliminar(id: String) = dao.eliminarPorId(id)
    suspend fun contar(): Int = dao.contar()
}
