package com.grupo.elevapro.ui.screen.perfil

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ExitToApp
import androidx.compose.material.icons.outlined.Business
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Engineering
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.People
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.grupo.elevapro.data.model.domain.Rol
import com.grupo.elevapro.data.model.domain.Usuario
import com.grupo.elevapro.data.repository.AuthRepository
import com.grupo.elevapro.ui.components.ElevaProTopAppBar
import com.grupo.elevapro.ui.components.StatusChip
import com.grupo.elevapro.ui.components.TipoEstado
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

sealed interface PerfilUiState {
    data object Loading : PerfilUiState
    data class Success(val usuario: Usuario) : PerfilUiState
}

@HiltViewModel
class PerfilViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    val estado: StateFlow<PerfilUiState> = authRepository.usuarioActual
        .map { u -> if (u != null) PerfilUiState.Success(u) else PerfilUiState.Loading as PerfilUiState }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PerfilUiState.Loading)

    init {
        // El usuario actual es provisto reactivamente por AuthRepository.
        // Para H2: aquí se refrescarían los datos del perfil desde el backend.
    }

    private val _loggedOut = MutableStateFlow(false)
    val loggedOut: StateFlow<Boolean> = _loggedOut.asStateFlow()

    fun logout() {
        authRepository.logout()
        _loggedOut.value = true
    }

    fun onLogoutHandled() { _loggedOut.value = false }
}

@Composable
fun PerfilScreen(
    onSupervisores: () -> Unit,
    onPlantillas: () -> Unit,
    onDatosEmpresa: () -> Unit,
    onUsuarios: () -> Unit,
    onRolesPermisos: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PerfilViewModel = hiltViewModel(),
) {
    val estado    by viewModel.estado.collectAsStateWithLifecycle()
    val loggedOut by viewModel.loggedOut.collectAsStateWithLifecycle()

    LaunchedEffect(loggedOut) {
        if (loggedOut) {
            viewModel.onLogoutHandled()
            onLogout()
        }
    }

    PerfilContent(
        estado          = estado,
        onSupervisores  = onSupervisores,
        onPlantillas    = onPlantillas,
        onDatosEmpresa  = onDatosEmpresa,
        onUsuarios      = onUsuarios,
        onRolesPermisos = onRolesPermisos,
        onLogout        = viewModel::logout,
        modifier        = modifier,
    )
}

@Composable
private fun PerfilContent(
    estado: PerfilUiState,
    onSupervisores: () -> Unit,
    onPlantillas: () -> Unit,
    onDatosEmpresa: () -> Unit,
    onUsuarios: () -> Unit,
    onRolesPermisos: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        topBar = { ElevaProTopAppBar(titulo = "Perfil") },
        modifier = modifier,
    ) { padding ->
        when (val s = estado) {
            PerfilUiState.Loading -> Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }

            is PerfilUiState.Success -> PerfilSuccessBody(
                usuario        = s.usuario,
                onSupervisores = onSupervisores,
                onPlantillas   = onPlantillas,
                onDatosEmpresa = onDatosEmpresa,
                onUsuarios     = onUsuarios,
                onRolesPermisos = onRolesPermisos,
                onLogout       = onLogout,
                modifier       = Modifier.fillMaxSize().padding(padding),
            )
        }
    }
}

@Composable
private fun PerfilSuccessBody(
    usuario: Usuario,
    onSupervisores: () -> Unit,
    onPlantillas: () -> Unit,
    onDatosEmpresa: () -> Unit,
    onUsuarios: () -> Unit,
    onRolesPermisos: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        UsuarioInfoCard(usuario = usuario)

        if (usuario.rol == Rol.ADMINISTRADOR) {
            Text(
                text = "Gestión",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp),
            )
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    PerfilMenuItem(icon = Icons.Outlined.People, label = "Usuarios", onClick = onUsuarios)
                    HorizontalDivider(modifier = Modifier.padding(start = 56.dp))
                    PerfilMenuItem(icon = Icons.Outlined.Engineering, label = "Supervisores", onClick = onSupervisores)
                    HorizontalDivider(modifier = Modifier.padding(start = 56.dp))
                    PerfilMenuItem(icon = Icons.Outlined.Description, label = "Plantillas de mantenimiento", onClick = onPlantillas)
                    HorizontalDivider(modifier = Modifier.padding(start = 56.dp))
                    PerfilMenuItem(icon = Icons.Outlined.Business, label = "Datos de la empresa", onClick = onDatosEmpresa)
                    HorizontalDivider(modifier = Modifier.padding(start = 56.dp))
                    PerfilMenuItem(icon = Icons.Outlined.Lock, label = "Roles y permisos", onClick = onRolesPermisos)
                }
            }
        }

        Text(
            text = "Cuenta",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp),
        )
        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            PerfilMenuItem(
                icon = Icons.AutoMirrored.Outlined.ExitToApp,
                label = "Cerrar sesión",
                onClick = onLogout,
                tintError = true,
            )
        }
    }
}

@Composable
private fun UsuarioInfoCard(usuario: Usuario, modifier: Modifier = Modifier) {
    ElevatedCard(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = usuario.nombre.first().uppercaseChar().toString(),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(usuario.nombre, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = usuario.email,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            StatusChip(
                text = if (usuario.rol == Rol.ADMINISTRADOR) "Admin" else "Operativo",
                tipo = if (usuario.rol == Rol.ADMINISTRADOR) TipoEstado.SUCCESS else TipoEstado.NEUTRAL,
            )
        }
    }
}

@Composable
private fun PerfilMenuItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tintError: Boolean = false,
) {
    val tint = if (tintError) MaterialTheme.colorScheme.error
               else MaterialTheme.colorScheme.onSurfaceVariant

    Surface(onClick = onClick, modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(24.dp),
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = if (tintError) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
