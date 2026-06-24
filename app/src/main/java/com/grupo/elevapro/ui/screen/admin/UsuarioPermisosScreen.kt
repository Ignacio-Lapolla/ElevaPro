package com.grupo.elevapro.ui.screen.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.grupo.elevapro.data.model.domain.Permiso
import com.grupo.elevapro.data.model.domain.Rol
import com.grupo.elevapro.data.model.domain.Usuario
import com.grupo.elevapro.data.repository.PermisosRepository
import com.grupo.elevapro.data.repository.UsuariosRepository
import com.grupo.elevapro.ui.components.ElevaProTopAppBar
import com.grupo.elevapro.ui.components.FilledPrimaryButton
import com.grupo.elevapro.ui.components.StatusChip
import com.grupo.elevapro.ui.components.TipoEstado
import com.grupo.elevapro.ui.navigation.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

// ── UiState ─────────────────────────────────────────────────────────────────

sealed interface UsuarioPermisosUiState {
    data object Loading : UsuarioPermisosUiState
    data class Success(
        val usuario: Usuario,
        val permisos: Set<Permiso>,
    ) : UsuarioPermisosUiState
    data class Error(val mensaje: String) : UsuarioPermisosUiState
}

// ── ViewModel ────────────────────────────────────────────────────────────────

@HiltViewModel
class UsuarioPermisosViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val usuariosRepository: UsuariosRepository,
    private val permisosRepository: PermisosRepository,
) : ViewModel() {

    private val usuarioId: String = checkNotNull(savedStateHandle[Screen.UsuarioPermisos.ARG_ID])

    private val _permisosEditados = MutableStateFlow<Set<Permiso>?>(null)
    private val _guardadoExitoso  = Channel<Unit>(Channel.CONFLATED)
    val guardadoExitoso = _guardadoExitoso.receiveAsFlow()

    val estado: StateFlow<UsuarioPermisosUiState> = combine(
        usuariosRepository.observarUsuarios(),
        permisosRepository.observarPermisos(usuarioId),
        _permisosEditados,
    ) { usuarios, permisosRepo, editados ->
        val usuario = usuarios.find { it.id == usuarioId }
        if (usuario == null) {
            UsuarioPermisosUiState.Error("Usuario no encontrado")
        } else {
            UsuarioPermisosUiState.Success(usuario, editados ?: permisosRepo)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UsuarioPermisosUiState.Loading)

    fun togglePermiso(permiso: Permiso) {
        val actual = (estado.value as? UsuarioPermisosUiState.Success)?.permisos ?: return
        _permisosEditados.value = if (permiso in actual) actual - permiso else actual + permiso
    }

    fun guardar() {
        val editados = _permisosEditados.value
        if (editados == null) {
            viewModelScope.launch { _guardadoExitoso.send(Unit) }
            return
        }
        viewModelScope.launch {
            permisosRepository.actualizar(usuarioId, editados)
            _permisosEditados.value = null
            _guardadoExitoso.send(Unit)
        }
    }
}

// ── Container ────────────────────────────────────────────────────────────────

@Composable
fun UsuarioPermisosScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: UsuarioPermisosViewModel = hiltViewModel(),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.guardadoExitoso.collect { onBack() } }

    UsuarioPermisosContent(
        estado   = estado,
        onToggle = viewModel::togglePermiso,
        onGuardar = viewModel::guardar,
        onBack   = onBack,
        modifier = modifier,
    )
}

// ── Stateless content ────────────────────────────────────────────────────────

@Composable
private fun UsuarioPermisosContent(
    estado: UsuarioPermisosUiState,
    onToggle: (Permiso) -> Unit,
    onGuardar: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val titulo = (estado as? UsuarioPermisosUiState.Success)?.usuario?.nombre ?: "Permisos"

    Scaffold(
        topBar = { ElevaProTopAppBar(titulo = titulo, onBack = onBack) },
        bottomBar = {
            Surface(shadowElevation = 8.dp) {
                FilledPrimaryButton(
                    text = "Guardar cambios",
                    onClick = onGuardar,
                    enabled = estado is UsuarioPermisosUiState.Success,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                )
            }
        },
        modifier = modifier,
    ) { padding ->
        when (estado) {
            UsuarioPermisosUiState.Loading -> Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }

            is UsuarioPermisosUiState.Error -> Text(
                text = estado.mensaje,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(padding).padding(16.dp),
            )

            is UsuarioPermisosUiState.Success -> LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                item { UsuarioResumenCard(estado.usuario) }

                item {
                    Text(
                        text = "Permisos individuales",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 20.dp, bottom = 4.dp),
                    )
                }

                CATEGORIAS.forEach { cat ->
                    item {
                        Text(
                            text = cat.nombre,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 12.dp, bottom = 2.dp),
                        )
                    }
                    items(cat.permisos, key = { it.name }) { permiso ->
                        PermisoSwitchItem(
                            label = permiso.label(),
                            activado = permiso in estado.permisos,
                            onToggle = { onToggle(permiso) },
                        )
                    }
                }
            }
        }
    }
}

// ── UsuarioResumenCard ────────────────────────────────────────────────────────

@Composable
private fun UsuarioResumenCard(usuario: Usuario, modifier: Modifier = Modifier) {
    ElevatedCard(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = usuario.nombre.first().uppercaseChar().toString(),
                    style = MaterialTheme.typography.titleLarge,
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

// ── PermisoSwitchItem ─────────────────────────────────────────────────────────

@Composable
private fun PermisoSwitchItem(
    label: String,
    activado: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
        Switch(
            checked = activado,
            onCheckedChange = { onToggle() },
            modifier = Modifier.semantics {
                contentDescription = if (activado) "$label: activado" else "$label: desactivado"
            },
        )
    }
}
