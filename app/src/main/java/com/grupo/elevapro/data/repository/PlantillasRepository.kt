package com.grupo.elevapro.data.repository

import com.grupo.elevapro.data.model.domain.Plantilla
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

interface PlantillasRepository {
    fun observarPlantillas(): Flow<List<Plantilla>>
    suspend fun obtenerPorId(id: String): Plantilla?
    suspend fun crear(plantilla: Plantilla)
    suspend fun actualizar(plantilla: Plantilla)
}

@Singleton
class FakePlantillasRepository @Inject constructor() : PlantillasRepository {

    private val _plantillas = MutableStateFlow(FakeMockData.plantillas)

    override fun observarPlantillas(): Flow<List<Plantilla>> = _plantillas.asStateFlow()

    override suspend fun obtenerPorId(id: String): Plantilla? = _plantillas.value.find { it.id == id }

    override suspend fun crear(plantilla: Plantilla) {
        _plantillas.update { it + plantilla }
    }

    override suspend fun actualizar(plantilla: Plantilla) {
        _plantillas.update { lista -> lista.map { if (it.id == plantilla.id) plantilla else it } }
    }
}
