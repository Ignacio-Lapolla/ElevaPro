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
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
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
import com.grupo.elevapro.data.model.domain.Especialidad
import com.grupo.elevapro.data.model.domain.Supervisor
import com.grupo.elevapro.data.repository.SupervisoresRepository
import com.grupo.elevapro.ui.components.ElevaProTextField
import com.grupo.elevapro.ui.components.ElevaProTopAppBar
import com.grupo.elevapro.ui.components.FilledPrimaryButton
import com.grupo.elevapro.ui.components.FilterChipBar
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

private fun Especialidad.label(): String = when (this) {
    Especialidad.PREVENTIVO    -> "Preventivo"
    Especialidad.EMERGENCIA    -> "Emergencia"
    Especialidad.INSTALACIONES -> "Instalaciones"
    Especialidad.MODERNIZACION -> "Modernización"
}

private fun Especialidad.tipoEstado(): TipoEstado = when (this) {
    Especialidad.PREVENTIVO    -> TipoEstado.SUCCESS
    Especialidad.EMERGENCIA    -> TipoEstado.ERROR
    Especialidad.INSTALACIONES -> TipoEstado.WARNING
    Especialidad.MODERNIZACION -> TipoEstado.NEUTRAL
}

enum class FiltroSupervisor(val label: String) {
    TODOS("Todos"),
    PREVENTIVO("Preventivo"),
    EMERGENCIA("Emergencia"),
    INSTALACIONES("Instalaciones"),
    MODERNIZACION("Modernización"),
}

sealed interface SupervisoresUiState {
    data object Loading : SupervisoresUiState
    data class Success(
        val supervisores: List<Supervisor>,
        val filtro: FiltroSupervisor,
    ) : SupervisoresUiState
    data class Error(val mensaje: String) : SupervisoresUiState
}

@HiltViewModel
class SupervisoresViewModel @Inject constructor(
    private val supervisoresRepository: SupervisoresRepository,
) : ViewModel() {

    private val filtro = MutableStateFlow(FiltroSupervisor.TODOS)

    val estado: StateFlow<SupervisoresUiState> = combine(
        supervisoresRepository.observarSupervisores(),
        filtro,
    ) { lista, f ->
        val filtrados = when (f) {
            FiltroSupervisor.TODOS          -> lista
            FiltroSupervisor.PREVENTIVO    -> lista.filter { it.especialidad == Especialidad.PREVENTIVO }
            FiltroSupervisor.EMERGENCIA    -> lista.filter { it.especialidad == Especialidad.EMERGENCIA }
            FiltroSupervisor.INSTALACIONES -> lista.filter { it.especialidad == Especialidad.INSTALACIONES }
            FiltroSupervisor.MODERNIZACION -> lista.filter { it.especialidad == Especialidad.MODERNIZACION }
        }
        SupervisoresUiState.Success(filtrados, f) as SupervisoresUiState
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SupervisoresUiState.Loading)

    fun onFiltro(f: FiltroSupervisor) { filtro.value = f }

    fun agregar(nombre: String, telefono: String, email: String, especialidad: Especialidad) {
        viewModelScope.launch {
            supervisoresRepository.crear(
                Supervisor(
                    id = "s${System.currentTimeMillis()}",
                    nombre = nombre.trim(),
                    telefono = telefono.trim(),
                    email = email.trim(),
                    especialidad = especialidad,
                )
            )
        }
    }
}

@Composable
fun SupervisoresScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SupervisoresViewModel = hiltViewModel(),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    SupervisoresContent(
        estado   = estado,
        onFiltro = viewModel::onFiltro,
        onAgregar = viewModel::agregar,
        onBack   = onBack,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SupervisoresContent(
    estado: SupervisoresUiState,
    onFiltro: (FiltroSupervisor) -> Unit,
    onAgregar: (String, String, String, Especialidad) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var mostrarSheet by remember { mutableStateOf(false) }
    val opciones = remember { FiltroSupervisor.entries.map { it.label } }

    Scaffold(
        topBar = { ElevaProTopAppBar(titulo = "Supervisores", onBack = onBack) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { mostrarSheet = true },
                icon = { Icon(Icons.Outlined.Add, contentDescription = null) },
                text = { Text("Agregar supervisor") },
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
                opciones = opciones,
                seleccionada = (estado as? SupervisoresUiState.Success)?.filtro?.label
                    ?: FiltroSupervisor.TODOS.label,
                onSeleccion = { label ->
                    onFiltro(FiltroSupervisor.entries.first { it.label == label })
                },
            )
            Spacer(Modifier.height(8.dp))
            when (estado) {
                SupervisoresUiState.Loading -> Box(
                    Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) { CircularProgressIndicator() }

                is SupervisoresUiState.Error -> Text(
                    text = estado.mensaje,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(16.dp),
                )

                is SupervisoresUiState.Success -> LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(estado.supervisores, key = { it.id }) { supervisor ->
                        SupervisorCard(
                            supervisor = supervisor,
                            onClick = {},
                        )
                    }
                }
            }
        }
    }

    if (mostrarSheet) {
        AgregarSupervisorSheet(
            onDismiss = { mostrarSheet = false },
            onAgregar = { nombre, telefono, email, especialidad ->
                onAgregar(nombre, telefono, email, especialidad)
                mostrarSheet = false
            },
        )
    }
}

@Composable
private fun SupervisorCard(
    supervisor: Supervisor,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ElevatedCard(onClick = onClick, modifier = modifier.fillMaxWidth()) {
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
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = supervisor.nombre.first().uppercaseChar().toString(),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(supervisor.nombre, style = MaterialTheme.typography.titleMedium)
                    StatusChip(
                        text = supervisor.especialidad.label(),
                        tipo = supervisor.especialidad.tipoEstado(),
                    )
                }
                Text(
                    text = supervisor.telefono,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = supervisor.email,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = "Ver detalle de ${supervisor.nombre}",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AgregarSupervisorSheet(
    onDismiss: () -> Unit,
    onAgregar: (String, String, String, Especialidad) -> Unit,
    modifier: Modifier = Modifier,
) {
    var nombre      by remember { mutableStateOf("") }
    var telefono    by remember { mutableStateOf("") }
    var email       by remember { mutableStateOf("") }
    var especialidad by remember { mutableStateOf(Especialidad.PREVENTIVO) }
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
            Text("Agregar supervisor", style = MaterialTheme.typography.titleLarge)

            ElevaProTextField(value = nombre, onValueChange = { nombre = it }, label = "Nombre completo")
            ElevaProTextField(
                value = telefono, onValueChange = { telefono = it }, label = "Teléfono",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            )
            ElevaProTextField(
                value = email, onValueChange = { email = it }, label = "Email",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            )

            Text("Especialidad", style = MaterialTheme.typography.labelLarge)
            Especialidad.entries.forEach { opcion ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = especialidad == opcion,
                        onClick = { especialidad = opcion },
                    )
                    Text(opcion.label(), style = MaterialTheme.typography.bodyMedium)
                }
            }

            FilledPrimaryButton(
                text = "Agregar supervisor",
                onClick = { onAgregar(nombre, telefono, email, especialidad) },
                enabled = nombre.isNotBlank() && telefono.isNotBlank() && email.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
