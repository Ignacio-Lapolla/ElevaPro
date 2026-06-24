package com.grupo.elevapro.data.repository

import com.grupo.elevapro.data.local.datasource.ClienteLocalDataSource
import com.grupo.elevapro.data.local.entity.toEntity
import com.grupo.elevapro.data.local.entity.toDomain
import com.grupo.elevapro.data.model.domain.Cliente
import com.grupo.elevapro.di.ApplicationScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomClienteRepository @Inject constructor(
    private val local: ClienteLocalDataSource,
    @ApplicationScope private val scope: CoroutineScope,
) : ClienteRepository {

    private val seedMutex = Mutex()

    init {
        scope.launch {
            seedMutex.withLock {
                if (local.contar() == 0) {
                    local.insertar(FakeMockData.clientes.map { it.toEntity() })
                }
            }
        }
    }

    override fun observarClientes(): Flow<List<Cliente>> =
        local.observarTodos().map { entities -> entities.map { it.toDomain() } }

    override suspend fun obtenerPorId(id: String): Cliente? =
        local.obtenerPorId(id)?.toDomain()

    override suspend fun agregar(cliente: Cliente) {
        local.insertarUno(cliente.toEntity())
    }

    override suspend fun actualizar(cliente: Cliente) {
        local.actualizar(cliente.toEntity())
    }

    override suspend fun eliminar(id: String) {
        local.eliminar(id)
    }

    override fun obtenerSupervisorNombre(supervisorId: String): String? =
        FakeMockData.supervisores.find { it.id == supervisorId }?.nombre
}
