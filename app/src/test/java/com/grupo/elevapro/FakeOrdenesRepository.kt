package com.grupo.elevapro

import com.grupo.elevapro.data.model.domain.Orden
import com.grupo.elevapro.data.repository.OrdenesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class FakeOrdenesRepository(initial: List<Orden> = emptyList()) : OrdenesRepository {

    private val _ordenes = MutableStateFlow(initial)

    override fun observarOrdenes(): Flow<List<Orden>> = _ordenes.asStateFlow()

    override suspend fun obtenerPorId(id: String) = _ordenes.value.find { it.id == id }

    override suspend fun crear(orden: Orden) { _ordenes.update { it + orden } }

    override suspend fun firmar(id: String, firmaBase64: String, nombreFirmante: String) {
        _ordenes.update { lista ->
            lista.map { if (it.id == id) it.copy(firmada = true, firmaBase64 = firmaBase64, nombreFirmante = nombreFirmante) else it }
        }
    }

    override suspend fun agregarFoto(id: String, rutaFoto: String) {
        _ordenes.update { lista ->
            lista.map { if (it.id == id) it.copy(fotos = it.fotos + rutaFoto) else it }
        }
    }

    override suspend fun eliminarFoto(id: String, rutaFoto: String) {
        _ordenes.update { lista ->
            lista.map { if (it.id == id) it.copy(fotos = it.fotos - rutaFoto) else it }
        }
    }
}
