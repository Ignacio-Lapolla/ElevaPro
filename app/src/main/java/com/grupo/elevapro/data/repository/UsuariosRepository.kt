package com.grupo.elevapro.data.repository

import com.grupo.elevapro.data.model.domain.Usuario
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

interface UsuariosRepository {
    fun observarUsuarios(): Flow<List<Usuario>>
    suspend fun obtenerPorId(id: String): Usuario?
    suspend fun crear(usuario: Usuario)
    suspend fun actualizar(usuario: Usuario)
    suspend fun eliminar(id: String)
}

@Singleton
class FakeUsuariosRepository @Inject constructor() : UsuariosRepository {

    private val _usuarios = MutableStateFlow(FakeMockData.usuarios)

    override fun observarUsuarios(): Flow<List<Usuario>> = _usuarios.asStateFlow()

    override suspend fun obtenerPorId(id: String): Usuario? = _usuarios.value.find { it.id == id }

    override suspend fun crear(usuario: Usuario) {
        _usuarios.update { it + usuario }
    }

    override suspend fun actualizar(usuario: Usuario) {
        _usuarios.update { lista -> lista.map { if (it.id == usuario.id) usuario else it } }
    }

    override suspend fun eliminar(id: String) {
        _usuarios.update { lista -> lista.filter { it.id != id } }
    }
}
