package com.grupo.elevapro.ui.screen.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.grupo.elevapro.data.model.domain.Permiso
import com.grupo.elevapro.data.model.domain.Rol
import com.grupo.elevapro.data.repository.PermisosRepository
import com.grupo.elevapro.ui.components.ElevaProTopAppBar
import com.grupo.elevapro.ui.components.FilledPrimaryButton
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface RolesPermisosUiState {
    data object Loading : RolesPermisosUiState
    data class Success(
        val tabSeleccionado: Rol,
        val permisosEditados: Map<Rol, Set<Permiso>>,
    ) : RolesPermisosUiState
    data class Error(val mensaje: String) : RolesPermisosUiState
}

@HiltViewModel
class RolesPermisosViewModel @Inject constructor(
    private val permisosRepository: PermisosRepository,
) : ViewModel() {

    private val _tabSeleccionado  = MutableStateFlow(Rol.ADMINISTRADOR)
    private val _permisosEditados = MutableStateFlow<Map<Rol, Set<Permiso>>?>(null)

    val estado: StateFlow<RolesPermisosUiState> = combine(
        permisosRepository.observarDefaultsRol(Rol.ADMINISTRADOR),
        permisosRepository.observarDefaultsRol(Rol.OPERATIVO),
        _tabSeleccionado,
        _permisosEditados,
    ) { admin, operativo, tab, editados ->
        val base = mapOf(Rol.ADMINISTRADOR to admin, Rol.OPERATIVO to operativo)
        RolesPermisosUiState.Success(
            tabSeleccionado  = tab,
            permisosEditados = editados ?: base,
        ) as RolesPermisosUiState
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RolesPermisosUiState.Loading)

    private val _plantillaGuardada = MutableStateFlow(false)
    val plantillaGuardada: StateFlow<Boolean> = _plantillaGuardada.asStateFlow()

    fun onTab(rol: Rol) { _tabSeleccionado.value = rol }

    fun togglePermiso(rol: Rol, permiso: Permiso) {
        val s = estado.value as? RolesPermisosUiState.Success ?: return
        val actual = s.permisosEditados[rol] ?: emptySet()
        val nuevo  = if (permiso in actual) actual - permiso else actual + permiso
        _permisosEditados.value = s.permisosEditados + (rol to nuevo)
    }

    fun guardarPlantilla() {
        val editados = _permisosEditados.value
        viewModelScope.launch {
            editados?.forEach { (rol, permisos) ->
                permisosRepository.actualizarDefaultsRol(rol, permisos)
            }
            _permisosEditados.value = null
            _plantillaGuardada.value = true
        }
    }

    fun onGuardadoHandled() { _plantillaGuardada.value = false }
}

@Composable
fun RolesPermisosScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RolesPermisosViewModel = hiltViewModel(),
) {
    val estado            by viewModel.estado.collectAsStateWithLifecycle()
    val plantillaGuardada by viewModel.plantillaGuardada.collectAsStateWithLifecycle()
    RolesPermisosContent(
        estado            = estado,
        plantillaGuardada = plantillaGuardada,
        onGuardadoHandled = viewModel::onGuardadoHandled,
        onTab             = viewModel::onTab,
        onToggle          = viewModel::togglePermiso,
        onGuardar         = viewModel::guardarPlantilla,
        onBack            = onBack,
        modifier          = modifier,
    )
}

@Composable
private fun RolesPermisosContent(
    estado: RolesPermisosUiState,
    plantillaGuardada: Boolean,
    onGuardadoHandled: () -> Unit,
    onTab: (Rol) -> Unit,
    onToggle: (Rol, Permiso) -> Unit,
    onGuardar: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(plantillaGuardada) {
        if (plantillaGuardada) {
            snackbarHostState.showSnackbar("Plantilla guardada")
            onGuardadoHandled()
        }
    }

    Scaffold(
        topBar = { ElevaProTopAppBar(titulo = "Roles y permisos", onBack = onBack) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            Surface(shadowElevation = 8.dp) {
                FilledPrimaryButton(
                    text = "Guardar plantilla",
                    onClick = onGuardar,
                    enabled = estado is RolesPermisosUiState.Success,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                )
            }
        },
        modifier = modifier,
    ) { padding ->
        when (val s = estado) {
            RolesPermisosUiState.Loading -> Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }

            is RolesPermisosUiState.Error -> Text(
                text = s.mensaje,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(padding).padding(16.dp),
            )

            is RolesPermisosUiState.Success -> RolesPermisosSuccess(
                success  = s,
                onTab    = onTab,
                onToggle = onToggle,
                modifier = Modifier.fillMaxSize().padding(padding),
            )
        }
    }
}

@Composable
private fun RolesPermisosSuccess(
    success: RolesPermisosUiState.Success,
    onTab: (Rol) -> Unit,
    onToggle: (Rol, Permiso) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tabIndex    = if (success.tabSeleccionado == Rol.ADMINISTRADOR) 0 else 1
    val rolActivo   = success.tabSeleccionado
    val permisosRol = success.permisosEditados[rolActivo] ?: emptySet()

    Column(modifier = modifier) {
        TabRow(selectedTabIndex = tabIndex) {
            Tab(
                selected = tabIndex == 0,
                onClick  = { onTab(Rol.ADMINISTRADOR) },
                text     = { Text("Administrador") },
            )
            Tab(
                selected = tabIndex == 1,
                onClick  = { onTab(Rol.OPERATIVO) },
                text     = { Text("Operativo") },
            )
        }

        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
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
                    RolPermisoSwitchItem(
                        label    = permiso.label(),
                        activado = permiso in permisosRol,
                        onToggle = { onToggle(rolActivo, permiso) },
                    )
                }
            }
        }
    }
}

@Composable
private fun RolPermisoSwitchItem(
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
