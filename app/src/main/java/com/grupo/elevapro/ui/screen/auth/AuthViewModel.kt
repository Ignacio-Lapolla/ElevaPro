package com.grupo.elevapro.ui.screen.auth

import androidx.lifecycle.ViewModel
import com.grupo.elevapro.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    val usuarioActual: StateFlow<com.grupo.elevapro.data.model.domain.Usuario?> =
        authRepository.usuarioActual

    fun logout() = authRepository.logout()
}
