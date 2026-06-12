package com.grupo.elevapro.ui.screen.ordenes

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.grupo.elevapro.data.repository.OrdenesRepository
import com.grupo.elevapro.ui.components.ElevaProTextField
import com.grupo.elevapro.ui.components.ElevaProTopAppBar
import com.grupo.elevapro.ui.components.FilledPrimaryButton
import com.grupo.elevapro.ui.navigation.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// ── State ──────────────────────────────────────────────────────────────────

data class FirmaUiState(
    val ordenNumero: String = "",
    val clienteNombre: String = "",
    val nombre: String = "",
    val hayTrazos: Boolean = false,
    val confirmado: Boolean = false,
)

// ── ViewModel ──────────────────────────────────────────────────────────────

@HiltViewModel
class FirmaViewModel @Inject constructor(
    private val repo: OrdenesRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val ordenId: String = checkNotNull(savedStateHandle[Screen.Firma.ARG_ID])

    private val _estado = MutableStateFlow(FirmaUiState())
    val estado: StateFlow<FirmaUiState> = _estado.asStateFlow()

    init {
        viewModelScope.launch {
            val orden = repo.obtenerPorId(ordenId)
            if (orden != null) {
                _estado.update { it.copy(ordenNumero = orden.numero, clienteNombre = orden.clienteNombre) }
            }
        }
    }

    fun onNombreChange(nombre: String) = _estado.update { it.copy(nombre = nombre) }

    fun onHayTrazosChange(hayTrazos: Boolean) = _estado.update { it.copy(hayTrazos = hayTrazos) }

    fun confirmar(firmaBase64: String) {
        viewModelScope.launch {
            repo.firmar(ordenId, firmaBase64, _estado.value.nombre)
            _estado.update { it.copy(confirmado = true) }
        }
    }
}

// ── Canvas composable ─────────────────────────────────────────────────────

@Composable
fun FirmaCanvas(
    paths: List<Path>,
    currentPath: Path,
    onStart: (Offset) -> Unit,
    onDrag: (Offset) -> Unit,
    onEnd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Canvas(
        modifier = modifier.pointerInput(Unit) {
            detectDragGestures(
                onDragStart = { onStart(it) },
                onDrag = { change, _ -> onDrag(change.position) },
                onDragEnd = { onEnd() },
            )
        },
    ) {
        paths.forEach { drawPath(it, Color.Black, style = Stroke(width = 4f)) }
        drawPath(currentPath, Color.Black, style = Stroke(width = 4f))
    }
}

// ── Screen entry point ────────────────────────────────────────────────────

@Composable
fun FirmaScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FirmaViewModel = hiltViewModel(),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()

    val paths = remember { mutableStateListOf<Path>() }
    var currentPath by remember { mutableStateOf(Path()) }
    // Plain list, not state — only currentPath (state) drives recomposition
    val currentPoints = remember { mutableListOf<Offset>() }

    LaunchedEffect(estado.confirmado) {
        if (estado.confirmado) onBack()
    }

    FirmaContent(
        estado = estado,
        paths = paths,
        currentPath = currentPath,
        onBack = onBack,
        onStart = { offset ->
            currentPoints.clear()
            currentPoints.add(offset)
            currentPath = Path().apply { moveTo(offset.x, offset.y) }
        },
        onDrag = { offset ->
            currentPoints.add(offset)
            currentPath = buildPathFromPoints(currentPoints)
        },
        onEnd = {
            if (currentPoints.size > 1) paths.add(buildPathFromPoints(currentPoints))
            currentPoints.clear()
            currentPath = Path()
            viewModel.onHayTrazosChange(true)
        },
        onLimpiar = {
            paths.clear()
            currentPoints.clear()
            currentPath = Path()
            viewModel.onHayTrazosChange(false)
        },
        onNombreChange = viewModel::onNombreChange,
        onConfirmar = { viewModel.confirmar("FIRMA_MOCK_${System.currentTimeMillis()}") },
        modifier = modifier,
    )
}

private fun buildPathFromPoints(points: List<Offset>): Path = Path().apply {
    if (points.isEmpty()) return@apply
    moveTo(points.first().x, points.first().y)
    points.drop(1).forEach { lineTo(it.x, it.y) }
}

// ── Stateless content ─────────────────────────────────────────────────────

@Composable
private fun FirmaContent(
    estado: FirmaUiState,
    paths: List<Path>,
    currentPath: Path,
    onBack: () -> Unit,
    onStart: (Offset) -> Unit,
    onDrag: (Offset) -> Unit,
    onEnd: () -> Unit,
    onLimpiar: () -> Unit,
    onNombreChange: (String) -> Unit,
    onConfirmar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        topBar = { ElevaProTopAppBar(titulo = "Firma de conformidad", onBack = onBack) },
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
            Text(
                text = "Cliente: ${estado.clienteNombre}",
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = "Orden N° ${estado.ordenNumero}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .border(1.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.medium),
            ) {
                FirmaCanvas(
                    paths = paths,
                    currentPath = currentPath,
                    onStart = onStart,
                    onDrag = onDrag,
                    onEnd = onEnd,
                    modifier = Modifier.fillMaxSize(),
                )
                IconButton(
                    onClick = onLimpiar,
                    modifier = Modifier.align(Alignment.TopEnd),
                ) {
                    Icon(
                        Icons.Outlined.Refresh,
                        contentDescription = "Limpiar firma",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            ElevaProTextField(
                value = estado.nombre,
                onValueChange = onNombreChange,
                label = "Nombre del firmante",
            )

            FilledPrimaryButton(
                text = "Confirmar firma",
                onClick = onConfirmar,
                enabled = estado.hayTrazos && estado.nombre.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
