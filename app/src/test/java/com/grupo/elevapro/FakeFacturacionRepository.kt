package com.grupo.elevapro

import com.grupo.elevapro.data.model.domain.EstadoFactura
import com.grupo.elevapro.data.model.domain.Factura
import com.grupo.elevapro.data.repository.FacturacionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class FakeFacturacionRepository(initial: List<Factura> = emptyList()) : FacturacionRepository {

    private val _facturas = MutableStateFlow(initial)

    override fun observarFacturas(): Flow<List<Factura>> = _facturas.asStateFlow()

    override suspend fun obtenerPorId(id: String): Factura? = _facturas.value.find { it.id == id }

    override suspend fun agregar(factura: Factura) { _facturas.update { it + factura } }

    override suspend fun actualizarEstado(id: String, estado: EstadoFactura) {
        _facturas.update { lista ->
            lista.map { if (it.id == id) it.copy(estado = estado) else it }
        }
    }
}
