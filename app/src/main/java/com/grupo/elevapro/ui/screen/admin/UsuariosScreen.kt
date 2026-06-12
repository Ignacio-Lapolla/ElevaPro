package com.grupo.elevapro.ui.screen.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.grupo.elevapro.data.model.domain.Rol
import com.grupo.elevapro.data.model.domain.Usuario
import com.grupo.elevapro.data.model.domain.permisosDefault
import com.grupo.elevapro.data.repository.PermisosRepository
import com.grupo.elevapro.data.repository.UsuariosRepository
import com.grupo.elevapro.ui.components.ElevaProSearchBar
import com.grupo.elevapro.ui.components.ElevaProTopAppBar
import com.grupo.elevapro.ui.components.FilledPrimaryButton
import com.grupo.elevapro.ui.components.FilterChipBar
import com.grupo.elevapro.ui.components.ElevaProTextField
import com.grupo.elevapro.ui.components.StatusChip
import com.grupo.elevapro.ui.components.TipoEstado
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

// ── UiState ─────────────────────────────────────────────────────────────────

enum class FiltroUsuario(val label: String) {
    TODOS("Todos"), ADMINISTRADORES("Administradores"), OPERATIVOS("Operativos")
}

sealed interface UsuariosUiState {
    data object Loading : UsuariosUiState
    data class Success(
        val usuarios: List<Usuario>,
        val filtro: FiltroUsuario,
        val busqueda: String,
    ) : UsuariosUiState
    data class Error(val mensaje: String) : UsuariosUiState
}

// ── ViewModel ────────────────────────────────────────────────────────────────

@HiltViewModel
class UsuariosViewModel @Inject constructor(
    private val usuariosRepository: UsuariosRepository,
    private val permisosRepository: PermisosRepository,
) : ViewModel() {

    private val filtro   = MutableStateFlow(FiltroUsuario.TODOS)
    private val busqueda = MutableStateFlow("")

    val estado: StateFlow<UsuariosUiState> = combine(
        usuariosRepository.observarUsuarios(),
        filtro,
        busqueda,
    ) { lista, f, q ->
        val filtrados = lista
            .filter { u ->
                when (f) {
                    FiltroUsuario.TODOS           -> true
                    FiltroUsuario.ADMINISTRADORES -> u.rol == Rol.ADMINISTRADOR
                    FiltroUsuario.OPERATIVOS      -> u.rol == Rol.OPERATIVO
                }
            }
            .filter { u ->
                q.isBlank() || u.nombre.contains(q, ignoreCase = true) || u.email.contains(q, ignoreCase = true)
            }
        UsuariosUiState.Success(filtrados, f, q) as UsuariosUiState
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UsuariosUiState.Loading)

    fun onFiltro(f: FiltroUsuario) { filtro.value = f }
    fun onBusqueda(q: String) { busqueda.value = q }

    fun invitar(nombre: String, email: String, rol: Rol) {
        viewModelScope.launch {
            val nuevoUsuario = Usuario(
                id = "u${System.currentTimeMillis()}",
                nombre = nombre,
                email = email,
                rol = rol,
                numeroEmpresa = "123",
                telefono = null,
                fotoUrl = null,
            )
            usuariosRepository.crear(nuevoUsuario)
            permisosRepository.actualizar(nuevoUsuario.id, rol.permisosDefault)
        }
    }
}

// ── Container ────────────────────────────────────────────────────────────────

@Composable
fun UsuariosScreen(
    onEditarPermisos: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: UsuariosViewModel = hiltViewModel(),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    UsuariosContent(
        estado = estado,
        onFiltro = viewModel::onFiltro,
        onBusqueda = viewModel::onBusqueda,
        onEditarPermisos = onEditarPermisos,
        onInvitar = viewModel::invitar,
        onBack = onBack,
        modifier = modifier,
    )
}

// ── Stateless content ────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UsuariosContent(
    estado: UsuariosUiState,
    onFiltro: (FiltroUsuario) -> Unit,
    onBusqueda: (String) -> Unit,
    onEditarPermisos: (String) -> Unit,
    onInvitar: (String, String, Rol) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var mostrarSheet by remember { mutableStateOf(false) }
    var mostrarBuscador by remember { mutableStateOf(false) }
    val opcionesFiltro = remember { FiltroUsuario.entries.map { it.label } }
    val busqueda = (estado as? UsuariosUiState.Success)?.busqueda ?: ""

    Scaffold(
        topBar = {
            if (mostrarBuscador) {
                ElevaProSearchBar(
                    query = busqueda,
                    onQueryChange = onBusqueda,
                    onClose = {
                        mostrarBuscador = false
                        onBusqueda("")
                    },
                    placeholder = "Buscar por nombre o email",
                )
            } else {
                ElevaProTopAppBar(
                    titulo = "Usuarios",
                    onBack = onBack,
                    acciones = {
                        IconButton(onClick = { mostrarBuscador = true }) {
                            Icon(Icons.Outlined.Search, contentDescription = "Buscar usuario")
                        }
                    },
                )
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { mostrarSheet = true },
                icon = { Icon(Icons.Outlined.Add, contentDescription = null) },
                text = { Text("Invitar usuario") },
            )
        },
        modifier = modifier,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            Spacer(Modifier.height(8.dp))
            FilterChipBar(
                opciones = opcionesFiltro,
                seleccionada = (estado as? UsuariosUiState.Success)?.filtro?.label
                    ?: FiltroUsuario.TODOS.label,
                onSeleccion = { label ->
                    onFiltro(FiltroUsuario.entries.first { it.label == label })
                },
            )
            Spacer(Modifier.height(8.dp))
            when (estado) {
                UsuariosUiState.Loading -> Box(
                    Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) { CircularProgressIndicator() }

                is UsuariosUiState.Error -> Text(
                    text = estado.mensaje,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(16.dp),
                )

                is UsuariosUiState.Success -> LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(estado.usuarios, key = { it.id }) { usuario ->
                        UsuarioCard(
                            usuario = usuario,
                            onEditar = { onEditarPermisos(usuario.id) },
                        )
                    }
                }
            }
        }
    }

    if (mostrarSheet) {
        InvitarUsuarioSheet(
            onDismiss = { mostrarSheet = false },
            onInvitar = { nombre, email, rol ->
                onInvitar(nombre, email, rol)
                mostrarSheet = false
            },
        )
    }
}

// ── UsuarioCard ───────────────────────────────────────────────────────────────

@Composable
private fun UsuarioCard(
    usuario: Usuario,
    onEditar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ElevatedCard(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = usuario.nombre.first().uppercaseChar().toString(),
                    style = MaterialTheme.typography.titleMedium,
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
            IconButton(onClick = onEditar) {
                Icon(
                    imageVector = Icons.Outlined.Edit,
                    contentDescription = "Editar permisos de ${usuario.nombre}",
                )
            }
        }
    }
}

// ── InvitarUsuarioSheet ───────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InvitarUsuarioSheet(
    onDismiss: () -> Unit,
    onInvitar: (String, String, Rol) -> Unit,
    modifier: Modifier = Modifier,
) {
    var nombre by remember { mutableStateOf("") }
    var email  by remember { mutableStateOf("") }
    var rol    by remember { mutableStateOf(Rol.OPERATIVO) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("Invitar usuario", style = MaterialTheme.typography.titleLarge)

            ElevaProTextField(
                value = nombre,
                onValueChange = { nombre = it },
                label = "Nombre completo",
            )
            ElevaProTextField(
                value = email,
                onValueChange = { email = it },
                label = "Email",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            )

            Text("Rol", style = MaterialTheme.typography.labelLarge)
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = rol == Rol.OPERATIVO,
                    onClick = { rol = Rol.OPERATIVO },
                )
                Text(
                    text = "Operativo",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(end = 24.dp),
                )
                RadioButton(
                    selected = rol == Rol.ADMINISTRADOR,
                    onClick = { rol = Rol.ADMINISTRADOR },
                )
                Text("Administrador", style = MaterialTheme.typography.bodyMedium)
            }

            FilledPrimaryButton(
                text = "Invitar usuario",
                onClick = { onInvitar(nombre.trim(), email.trim(), rol) },
                enabled = nombre.isNotBlank() && email.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
