package com.grupo.elevapro.data.repository

import com.grupo.elevapro.data.model.domain.Orden
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

interface OrdenesRepository {
    fun observarOrdenes(): Flow<List<Orden>>
    suspend fun obtenerPorId(id: String): Orden?
    suspend fun crear(orden: Orden)
    suspend fun firmar(id: String, firmaBase64: String, nombreFirmante: String)
}

@Singleton
class FakeOrdenesRepository @Inject constructor() : OrdenesRepository {

    private val _ordenes = MutableStateFlow(FakeMockData.ordenes)

    override fun observarOrdenes(): Flow<List<Orden>> = _ordenes.asStateFlow()

    override suspend fun obtenerPorId(id: String): Orden? = _ordenes.value.find { it.id == id }

    override suspend fun crear(orden: Orden) {
        _ordenes.update { it + orden }
    }

    override suspend fun firmar(id: String, firmaBase64: String, nombreFirmante: String) {
        _ordenes.update { lista ->
            lista.map { orden ->
                if (orden.id == id) {
                    orden.copy(firmada = true, firmaBase64 = firmaBase64, nombreFirmante = nombreFirmante)
                } else {
                    orden
                }
            }
        }
    }
}
