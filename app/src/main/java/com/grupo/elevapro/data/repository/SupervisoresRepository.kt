package com.grupo.elevapro.data.repository

import com.grupo.elevapro.data.model.domain.Supervisor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

interface SupervisoresRepository {
    fun observarSupervisores(): Flow<List<Supervisor>>
    suspend fun obtenerPorId(id: String): Supervisor?
    suspend fun crear(supervisor: Supervisor)
    suspend fun actualizar(supervisor: Supervisor)
}

@Singleton
class FakeSupervisoresRepository @Inject constructor() : SupervisoresRepository {

    private val _supervisores = MutableStateFlow(FakeMockData.supervisores)

    override fun observarSupervisores(): Flow<List<Supervisor>> = _supervisores.asStateFlow()

    override suspend fun obtenerPorId(id: String): Supervisor? = _supervisores.value.find { it.id == id }

    override suspend fun crear(supervisor: Supervisor) {
        _supervisores.update { it + supervisor }
    }

    override suspend fun actualizar(supervisor: Supervisor) {
        _supervisores.update { lista -> lista.map { if (it.id == supervisor.id) supervisor else it } }
    }
}
