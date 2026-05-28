package com.grupo.elevapro.data.repository

import com.grupo.elevapro.data.model.domain.Articulo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

interface ArticulosRepository {
    fun observarArticulos(): Flow<List<Articulo>>
    suspend fun obtenerPorId(id: String): Articulo?
    suspend fun agregar(articulo: Articulo)
}

@Singleton
class FakeArticulosRepository @Inject constructor() : ArticulosRepository {

    private val _articulos = MutableStateFlow(FakeMockData.articulos)

    override fun observarArticulos(): Flow<List<Articulo>> = _articulos.asStateFlow()

    override suspend fun obtenerPorId(id: String): Articulo? = _articulos.value.find { it.id == id }

    override suspend fun agregar(articulo: Articulo) {
        _articulos.update { it + articulo }
    }
}
