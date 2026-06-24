package com.grupo.elevapro

import com.grupo.elevapro.data.model.domain.Cliente
import com.grupo.elevapro.data.repository.ClienteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class FakeClienteRepository(initial: List<Cliente> = emptyList()) : ClienteRepository {

    private val _clientes = MutableStateFlow(initial)

    override fun observarClientes(): Flow<List<Cliente>> = _clientes.asStateFlow()

    override suspend fun obtenerPorId(id: String): Cliente? = _clientes.value.find { it.id == id }

    override suspend fun agregar(cliente: Cliente) { _clientes.update { it + cliente } }

    override suspend fun actualizar(cliente: Cliente) {
        _clientes.update { lista ->
            lista.map { if (it.id == cliente.id) cliente else it }
        }
    }

    override suspend fun eliminar(id: String) { _clientes.update { lista -> lista.filter { it.id != id } } }

    override fun obtenerSupervisorNombre(supervisorId: String): String? = null
}
