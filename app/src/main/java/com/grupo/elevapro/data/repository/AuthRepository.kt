package com.grupo.elevapro.data.repository

import com.grupo.elevapro.data.model.domain.Usuario
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

interface AuthRepository {
    val usuarioActual: StateFlow<Usuario?>
    suspend fun login(empresa: String, email: String, password: String): Result<Usuario>
    fun logout()
}

@Singleton
class FakeAuthRepository @Inject constructor() : AuthRepository {

    private val _usuarioActual = MutableStateFlow<Usuario?>(null)
    override val usuarioActual: StateFlow<Usuario?> = _usuarioActual.asStateFlow()

    override suspend fun login(empresa: String, email: String, password: String): Result<Usuario> = runCatching {
        delay(900)
        if (empresa.isBlank() || email.isBlank() || password.isBlank()) error("Completá todos los campos")
        val perfil = FakeMockData.usuarioPorEmail(email) ?: error("Usuario no encontrado")
        val usuario = perfil.copy(numeroEmpresa = empresa)
        _usuarioActual.value = usuario
        usuario
    }

    override fun logout() {
        _usuarioActual.value = null
    }
}
