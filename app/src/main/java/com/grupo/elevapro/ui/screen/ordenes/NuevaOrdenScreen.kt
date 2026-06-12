package com.grupo.elevapro.ui.screen.ordenes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.grupo.elevapro.data.model.domain.Cliente
import com.grupo.elevapro.data.model.domain.Orden
import com.grupo.elevapro.data.model.domain.Plantilla
import com.grupo.elevapro.data.repository.ClienteRepository
import com.grupo.elevapro.data.repository.OrdenesRepository
import com.grupo.elevapro.data.repository.PlantillasRepository
import com.grupo.elevapro.ui.components.ElevaProTopAppBar
import com.grupo.elevapro.ui.components.FilledPrimaryButton
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import javax.inject.Inject

sealed interface EstadoGuardado {
    data object Idle : EstadoGuardado
    data object Guardando : EstadoGuardado
    data object Guardado : EstadoGuardado
    data class Error(val mensaje: String) : EstadoGuardado
}

private val TIPOS_ORDEN = listOf(
    "Mantenimiento Preventivo",
    "Reparación de Emergencia",
    "Inspección Anual Reglamentaria",
    "Modernización de Cabina",
    "Limpieza de Foso",
    "Instalación Nueva",
)

data class NuevaOrdenUiState(
    val clienteId: String = "",
    val tipo: String = "",
    val plantillaId: String? = null,
    val observaciones: String = "",
    val estadoGuardado: EstadoGuardado = EstadoGuardado.Idle,
)

@HiltViewModel
class NuevaOrdenViewModel @Inject constructor(
    private val ordenesRepository: OrdenesRepository,
    private val clienteRepository: ClienteRepository,
    private val plantillasRepository: PlantillasRepository,
) : ViewModel() {

    private val _estado = MutableStateFlow(NuevaOrdenUiState())
    val estado: StateFlow<NuevaOrdenUiState> = _estado.asStateFlow()

    val clientes: StateFlow<List<Cliente>> = clienteRepository.observarClientes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val plantillas: StateFlow<List<Plantilla>> = plantillasRepository.observarPlantillas()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun onClienteSeleccionado(id: String)    { _estado.update { it.copy(clienteId = id) } }
    fun onTipoSeleccionado(tipo: String)     { _estado.update { it.copy(tipo = tipo) } }
    fun onPlantillaSeleccionada(id: String?) { _estado.update { it.copy(plantillaId = id) } }
    fun onObservacionesChanged(obs: String)  { _estado.update { it.copy(observaciones = obs) } }

    fun onGuardar() {
        val s = _estado.value
        if (s.clienteId.isBlank() || s.tipo.isBlank()) {
            _estado.update { it.copy(estadoGuardado = EstadoGuardado.Error("Seleccioná cliente y tipo de orden")) }
            return
        }
        viewModelScope.launch {
            _estado.update { it.copy(estadoGuardado = EstadoGuardado.Guardando) }
            val cliente = clienteRepository.obtenerPorId(s.clienteId)
            val fecha = SimpleDateFormat("dd MMM yyyy", Locale("es")).format(Date())
                .replace(".", "")
            ordenesRepository.crear(
                Orden(
                    id            = UUID.randomUUID().toString(),
                    numero        = "OT-${System.currentTimeMillis().toString().takeLast(4)}",
                    clienteId     = s.clienteId,
                    clienteNombre = cliente?.nombre ?: s.clienteId,
                    fecha         = fecha,
                    tipo          = s.tipo,
                    plantillaId   = s.plantillaId,
                    observaciones = s.observaciones,
                )
            )
            _estado.update { it.copy(estadoGuardado = EstadoGuardado.Guardado) }
        }
    }

    fun onGuardadoHandled() { _estado.update { it.copy(estadoGuardado = EstadoGuardado.Idle) } }
    fun onErrorHandled()    { _estado.update { it.copy(estadoGuardado = EstadoGuardado.Idle) } }
}

@Composable
fun NuevaOrdenScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: NuevaOrdenViewModel = hiltViewModel(),
) {
    val estado    by viewModel.estado.collectAsStateWithLifecycle()
    val clientes  by viewModel.clientes.collectAsStateWithLifecycle()
    val plantillas by viewModel.plantillas.collectAsStateWithLifecycle()

    LaunchedEffect(estado.estadoGuardado) {
        if (estado.estadoGuardado is EstadoGuardado.Guardado) {
            viewModel.onGuardadoHandled()
            onBack()
        }
    }

    NuevaOrdenContent(
        estado                = estado,
        clientes              = clientes,
        plantillas            = plantillas,
        onClienteSeleccionado = viewModel::onClienteSeleccionado,
        onTipoSeleccionado    = viewModel::onTipoSeleccionado,
        onPlantillaSeleccionada = viewModel::onPlantillaSeleccionada,
        onObservacionesChanged = viewModel::onObservacionesChanged,
        onGuardar             = viewModel::onGuardar,
        onErrorHandled        = viewModel::onErrorHandled,
        onBack                = onBack,
        modifier              = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NuevaOrdenContent(
    estado: NuevaOrdenUiState,
    clientes: List<Cliente>,
    plantillas: List<Plantilla>,
    onClienteSeleccionado: (String) -> Unit,
    onTipoSeleccionado: (String) -> Unit,
    onPlantillaSeleccionada: (String?) -> Unit,
    onObservacionesChanged: (String) -> Unit,
    onGuardar: () -> Unit,
    onErrorHandled: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(estado.estadoGuardado) {
        val s = estado.estadoGuardado
        if (s is EstadoGuardado.Error) {
            snackbarHostState.showSnackbar(s.mensaje)
            onErrorHandled()
        }
    }

    var clienteExpanded   by remember { mutableStateOf(false) }
    var tipoExpanded      by remember { mutableStateOf(false) }
    var plantillaExpanded by remember { mutableStateOf(false) }

    val clienteNombre  = remember(estado.clienteId, clientes) {
        clientes.find { it.id == estado.clienteId }?.nombre ?: ""
    }
    val plantillaNombre = remember(estado.plantillaId, plantillas) {
        plantillas.find { it.id == estado.plantillaId }?.nombre ?: "Sin plantilla"
    }

    Scaffold(
        topBar = {
            ElevaProTopAppBar(
                titulo = "Nueva orden",
                onBack = onBack,
                acciones = {
                    TextButton(
                        onClick = onGuardar,
                        enabled = estado.estadoGuardado !is EstadoGuardado.Guardando,
                    ) {
                        Text("Guardar")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            ExposedDropdownMenuBox(
                expanded = clienteExpanded,
                onExpandedChange = { clienteExpanded = !clienteExpanded },
            ) {
                OutlinedTextField(
                    value = clienteNombre,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Cliente *") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = clienteExpanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                )
                ExposedDropdownMenu(
                    expanded = clienteExpanded,
                    onDismissRequest = { clienteExpanded = false },
                ) {
                    clientes.forEach { cliente ->
                        DropdownMenuItem(
                            text = { Text(cliente.nombre) },
                            onClick = {
                                onClienteSeleccionado(cliente.id)
                                clienteExpanded = false
                            },
                        )
                    }
                }
            }

            ExposedDropdownMenuBox(
                expanded = tipoExpanded,
                onExpandedChange = { tipoExpanded = !tipoExpanded },
            ) {
                OutlinedTextField(
                    value = estado.tipo,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Tipo *") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = tipoExpanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                )
                ExposedDropdownMenu(
                    expanded = tipoExpanded,
                    onDismissRequest = { tipoExpanded = false },
                ) {
                    TIPOS_ORDEN.forEach { tipo ->
                        DropdownMenuItem(
                            text = { Text(tipo) },
                            onClick = {
                                onTipoSeleccionado(tipo)
                                tipoExpanded = false
                            },
                        )
                    }
                }
            }

            ExposedDropdownMenuBox(
                expanded = plantillaExpanded,
                onExpandedChange = { plantillaExpanded = !plantillaExpanded },
            ) {
                OutlinedTextField(
                    value = plantillaNombre,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Plantilla (opcional)") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = plantillaExpanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                )
                ExposedDropdownMenu(
                    expanded = plantillaExpanded,
                    onDismissRequest = { plantillaExpanded = false },
                ) {
                    DropdownMenuItem(
                        text = { Text("Sin plantilla") },
                        onClick = {
                            onPlantillaSeleccionada(null)
                            plantillaExpanded = false
                        },
                    )
                    plantillas.forEach { plantilla ->
                        DropdownMenuItem(
                            text = { Text(plantilla.nombre) },
                            onClick = {
                                onPlantillaSeleccionada(plantilla.id)
                                plantillaExpanded = false
                            },
                        )
                    }
                }
            }

            OutlinedTextField(
                value = estado.observaciones,
                onValueChange = onObservacionesChanged,
                label = { Text("Observaciones") },
                minLines = 3,
                maxLines = 6,
                modifier = Modifier.fillMaxWidth(),
            )

            FilledPrimaryButton(
                text = "Crear orden",
                onClick = onGuardar,
                enabled = estado.estadoGuardado !is EstadoGuardado.Guardando,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
