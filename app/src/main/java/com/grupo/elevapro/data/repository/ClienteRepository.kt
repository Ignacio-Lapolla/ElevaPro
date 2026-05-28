package com.grupo.elevapro.data.repository

import com.grupo.elevapro.data.model.domain.Cliente
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

interface ClienteRepository {
    fun observarClientes(): Flow<List<Cliente>>
    suspend fun obtenerPorId(id: String): Cliente?
    suspend fun agregar(cliente: Cliente)
    suspend fun actualizar(cliente: Cliente)
    fun obtenerSupervisorNombre(supervisorId: String): String?
}

@Singleton
class FakeClienteRepository @Inject constructor() : ClienteRepository {

    private val _clientes = MutableStateFlow(FakeMockData.clientes)

    override fun observarClientes(): Flow<List<Cliente>> = _clientes.asStateFlow()

    override suspend fun obtenerPorId(id: String): Cliente? =
        _clientes.value.find { it.id == id }

    override suspend fun agregar(cliente: Cliente) =
        _clientes.update { it + cliente }

    override suspend fun actualizar(cliente: Cliente) =
        _clientes.update { lista -> lista.map { if (it.id == cliente.id) cliente else it } }

    override fun obtenerSupervisorNombre(supervisorId: String): String? =
        FakeMockData.supervisores.find { it.id == supervisorId }?.nombre
}
