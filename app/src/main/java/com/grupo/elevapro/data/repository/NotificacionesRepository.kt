package com.grupo.elevapro.data.repository

import com.grupo.elevapro.data.model.domain.Notificacion
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

interface NotificacionesRepository {
    fun observarNotificaciones(): Flow<List<Notificacion>>
    suspend fun marcarLeida(id: String)
    suspend fun marcarTodasLeidas()
    suspend fun eliminar(id: String)
    suspend fun limpiarTodas()
}

@Singleton
class FakeNotificacionesRepository @Inject constructor() : NotificacionesRepository {

    private val _notis = MutableStateFlow(FakeMockData.notificaciones)

    override fun observarNotificaciones(): Flow<List<Notificacion>> = _notis.asStateFlow()

    override suspend fun marcarLeida(id: String) {
        _notis.update { it.map { n -> if (n.id == id) n.copy(leida = true) else n } }
    }

    override suspend fun marcarTodasLeidas() {
        _notis.update { it.map { n -> n.copy(leida = true) } }
    }

    override suspend fun eliminar(id: String) {
        _notis.update { it.filter { n -> n.id != id } }
    }

    override suspend fun limpiarTodas() {
        _notis.update { emptyList() }
    }
}
