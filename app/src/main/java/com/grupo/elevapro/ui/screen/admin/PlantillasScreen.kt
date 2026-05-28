package com.grupo.elevapro.ui.screen.admin

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.grupo.elevapro.data.model.domain.Plantilla
import com.grupo.elevapro.data.repository.PlantillasRepository
import com.grupo.elevapro.ui.components.ElevaProTextField
import com.grupo.elevapro.ui.components.ElevaProTopAppBar
import com.grupo.elevapro.ui.components.FilledPrimaryButton
import com.grupo.elevapro.ui.components.StatusChip
import com.grupo.elevapro.ui.components.TipoEstado
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

private fun String.tipoEstadoCategoria(): TipoEstado = when (lowercase()) {
    "preventivo"   -> TipoEstado.SUCCESS
    "emergencia"   -> TipoEstado.ERROR
    "instalaciones" -> TipoEstado.NEUTRAL
    else           -> TipoEstado.WARNING
}

sealed interface PlantillasUiState {
    data object Loading : PlantillasUiState
    data class Success(val plantillas: List<Plantilla>) : PlantillasUiState
    data class Error(val mensaje: String) : PlantillasUiState
}

@HiltViewModel
class PlantillasViewModel @Inject constructor(
    private val plantillasRepository: PlantillasRepository,
) : ViewModel() {

    val estado: StateFlow<PlantillasUiState> = plantillasRepository.observarPlantillas()
        .map { PlantillasUiState.Success(it) as PlantillasUiState }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PlantillasUiState.Loading)

    init {
        // El repositorio reactivo provee las plantillas al suscribirse.
        // Para H2: aquí se llamaría a sincronizar() contra el backend.
    }

    fun crear(nombre: String, descripcion: String, categoria: String, tiempoMinStr: String, tareasTexto: String) {
        viewModelScope.launch {
            plantillasRepository.crear(
                Plantilla(
                    id = "p${System.currentTimeMillis()}",
                    nombre = nombre.trim(),
                    descripcion = descripcion.trim(),
                    categoria = categoria.trim(),
                    tareas = tareasTexto.lines().map { it.trim() }.filter { it.isNotBlank() },
                    tiempoEstimadoMin = tiempoMinStr.toIntOrNull() ?: 0,
                )
            )
        }
    }
}

@Composable
fun PlantillasScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PlantillasViewModel = hiltViewModel(),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    PlantillasContent(
        estado  = estado,
        onCrear = viewModel::crear,
        onBack  = onBack,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlantillasContent(
    estado: PlantillasUiState,
    onCrear: (String, String, String, String, String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var plantillaDetalle  by remember { mutableStateOf<Plantilla?>(null) }
    var mostrarNuevaSheet by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { ElevaProTopAppBar(titulo = "Plantillas de mantenimiento", onBack = onBack) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { mostrarNuevaSheet = true },
                icon = { Icon(Icons.Outlined.Add, contentDescription = null) },
                text = { Text("Nueva plantilla") },
            )
        },
        modifier = modifier,
    ) { padding ->
        when (estado) {
            PlantillasUiState.Loading -> Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }

            is PlantillasUiState.Error -> Text(
                text = estado.mensaje,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(padding).padding(16.dp),
            )

            is PlantillasUiState.Success -> LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize().padding(padding),
            ) {
                items(estado.plantillas, key = { it.id }) { plantilla ->
                    PlantillaCard(
                        plantilla = plantilla,
                        onClick = { plantillaDetalle = plantilla },
                    )
                }
            }
        }
    }

    plantillaDetalle?.let { plantilla ->
        PlantillaDetalleSheet(
            plantilla = plantilla,
            onDismiss = { plantillaDetalle = null },
        )
    }

    if (mostrarNuevaSheet) {
        NuevaPlantillaSheet(
            onDismiss = { mostrarNuevaSheet = false },
            onCrear = { nombre, desc, cat, tiempo, tareas ->
                onCrear(nombre, desc, cat, tiempo, tareas)
                mostrarNuevaSheet = false
            },
        )
    }
}

@Composable
private fun PlantillaCard(
    plantilla: Plantilla,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ElevatedCard(onClick = onClick, modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(plantilla.nombre, style = MaterialTheme.typography.titleMedium)
            Text(
                text = plantilla.descripcion,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StatusChip(
                    text = plantilla.categoria,
                    tipo = plantilla.categoria.tipoEstadoCategoria(),
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Schedule,
                        contentDescription = "Duración estimada",
                        modifier = Modifier
                            .height(16.dp)
                            .padding(0.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = "${plantilla.tiempoEstimadoMin} min",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlantillaDetalleSheet(
    plantilla: Plantilla,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val checked = remember(plantilla.id) {
        mutableStateListOf(*BooleanArray(plantilla.tareas.size) { false }.toTypedArray())
    }
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
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(plantilla.nombre, style = MaterialTheme.typography.titleLarge)
            Text(
                text = plantilla.descripcion,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StatusChip(text = plantilla.categoria, tipo = plantilla.categoria.tipoEstadoCategoria())
                Icon(
                    Icons.Outlined.Schedule,
                    contentDescription = "Duración",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    "${plantilla.tiempoEstimadoMin} min",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            Text("Tareas", style = MaterialTheme.typography.titleMedium)
            plantilla.tareas.forEachIndexed { i, tarea ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics(mergeDescendants = true) {},
                ) {
                    Checkbox(
                        checked = checked[i],
                        onCheckedChange = { checked[i] = it },
                        modifier = Modifier.semantics {
                            contentDescription = if (checked[i]) "$tarea: completada" else tarea
                        },
                    )
                    Text(tarea, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NuevaPlantillaSheet(
    onDismiss: () -> Unit,
    onCrear: (String, String, String, String, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var nombre    by remember { mutableStateOf("") }
    var descripcion by remember { mutableStateOf("") }
    var categoria by remember { mutableStateOf("") }
    var tiempoMin by remember { mutableStateOf("") }
    var tareasTexto by remember { mutableStateOf("") }
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
            Text("Nueva plantilla", style = MaterialTheme.typography.titleLarge)
            ElevaProTextField(value = nombre, onValueChange = { nombre = it }, label = "Nombre")
            ElevaProTextField(value = descripcion, onValueChange = { descripcion = it }, label = "Descripción")
            ElevaProTextField(value = categoria, onValueChange = { categoria = it }, label = "Categoría")
            ElevaProTextField(
                value = tiempoMin, onValueChange = { tiempoMin = it.filter { c -> c.isDigit() } },
                label = "Tiempo estimado (min)",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
            ElevaProTextField(
                value = tareasTexto,
                onValueChange = { tareasTexto = it },
                label = "Tareas (una por línea)",
                singleLine = false,
            )
            FilledPrimaryButton(
                text = "Crear plantilla",
                onClick = { onCrear(nombre, descripcion, categoria, tiempoMin, tareasTexto) },
                enabled = nombre.isNotBlank() && categoria.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
