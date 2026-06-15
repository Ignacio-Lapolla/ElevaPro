package com.grupo.elevapro.ui.screen.ordenes

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.RemoveRedEye
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.grupo.elevapro.data.model.domain.Orden
import com.grupo.elevapro.data.repository.OrdenesRepository
import com.grupo.elevapro.ui.components.ElevaProTopAppBar
import com.grupo.elevapro.ui.components.StatusChip
import com.grupo.elevapro.ui.components.TipoEstado
import com.grupo.elevapro.ui.theme.SuccessContainer
import com.grupo.elevapro.ui.navigation.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface DetalleUiState {
    data object Loading : DetalleUiState
    data class Success(val orden: Orden) : DetalleUiState
    data class Error(val mensaje: String) : DetalleUiState
}

@HiltViewModel
class OrdenDetalleViewModel @Inject constructor(
    private val repo: OrdenesRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val id: String = checkNotNull(savedStateHandle[Screen.OrdenDetalle.ARG_ID])

    val estado: StateFlow<DetalleUiState> = repo.observarOrdenes()
        .map { ordenes ->
            val orden = ordenes.find { it.id == id }
            if (orden != null) DetalleUiState.Success(orden) else DetalleUiState.Error("Orden no encontrada")
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DetalleUiState.Loading)
}

// Fotos mock para la demo
private data class FotoMock(val label: String, val bg: androidx.compose.ui.graphics.Color, val tint: androidx.compose.ui.graphics.Color)

@Composable
fun OrdenDetalleScreen(
    onBack: () -> Unit,
    onFirmar: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: OrdenDetalleViewModel = hiltViewModel(),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    OrdenDetalleContent(
        estado   = estado,
        onBack   = onBack,
        onFirmar = onFirmar,
        modifier = modifier,
    )
}

@Composable
private fun OrdenDetalleContent(
    estado: DetalleUiState,
    onBack: () -> Unit,
    onFirmar: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val titulo = (estado as? DetalleUiState.Success)?.let { "Orden ${it.orden.numero}" } ?: "Detalle"
    val snackbarHost = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var mostrarVistaPRevia by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { ElevaProTopAppBar(titulo = titulo, onBack = onBack) },
        snackbarHost = { SnackbarHost(snackbarHost) },
        modifier = modifier,
    ) { padding ->
        when (val s = estado) {
            DetalleUiState.Loading -> Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }

            is DetalleUiState.Error -> Text(
                text = s.mensaje,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(padding).padding(16.dp),
            )

            is DetalleUiState.Success -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                ) {
                    // Cuerpo scrollable
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        OrdenDetalleBody(orden = s.orden)
                    }

                    // Barra de acciones fija abajo
                    HorizontalDivider()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        // Vista previa
                        FilledTonalButton(
                            onClick = { mostrarVistaPRevia = true },
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(Icons.Outlined.RemoveRedEye, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.size(6.dp))
                            Text("Vista previa", style = MaterialTheme.typography.labelMedium)
                        }

                        // Capturar firma
                        FilledTonalButton(
                            onClick = { if (!s.orden.firmada) onFirmar(s.orden.id) },
                            enabled = !s.orden.firmada,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = if (s.orden.firmada) SuccessContainer
                                                 else MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = if (s.orden.firmada) MaterialTheme.colorScheme.tertiary
                                               else MaterialTheme.colorScheme.onSecondaryContainer,
                            ),
                        ) {
                            Icon(Icons.Outlined.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.size(6.dp))
                            Text(
                                if (s.orden.firmada) "Firmada" else "Capturar firma",
                                style = MaterialTheme.typography.labelMedium,
                            )
                        }

                        // Descargar PDF
                        Button(
                            onClick = {
                                scope.launch { snackbarHost.showSnackbar("Descarga de PDF próximamente") }
                            },
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(Icons.Outlined.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.size(6.dp))
                            Text("Descargar", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }

                // Vista previa overlay
                if (mostrarVistaPRevia) {
                    VistaPreviaOverlay(
                        orden = s.orden,
                        onBack = { mostrarVistaPRevia = false },
                        onDescargar = {
                            scope.launch { snackbarHost.showSnackbar("Descarga de PDF próximamente") }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun OrdenDetalleBody(
    orden: Orden,
    modifier: Modifier = Modifier,
) {
    // Info general
    ElevatedCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            InfoRow(label = "Cliente",       value = orden.clienteNombre)
            InfoRow(label = "Tipo",          value = orden.tipo)
            InfoRow(label = "Fecha",         value = orden.fecha)
            InfoRow(label = "Nº de orden",   value = orden.numero)
            if (orden.observaciones.isNotBlank()) {
                InfoRow(label = "Observaciones", value = orden.observaciones)
            }
        }
    }

    // Imágenes adjuntas (mock para H1)
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    Icons.Outlined.Image,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    "IMÁGENES ADJUNTAS",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    "3 fotos",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                )
            }

            val fotos = listOf(
                "Cuarto de máquinas" to MaterialTheme.colorScheme.tertiaryContainer,
                "Panel eléctrico"    to MaterialTheme.colorScheme.primaryContainer,
                "Cabina interior"    to MaterialTheme.colorScheme.secondaryContainer,
            )

            // Grid 3 columnas con height fija para evitar infinite constraints
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                fotos.forEach { (label, color) ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(color),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Icon(
                                Icons.Outlined.CameraAlt,
                                contentDescription = label,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(22.dp),
                            )
                            Text(
                                label,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 4.dp),
                            )
                        }
                    }
                }

                // Agregar
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .border(2.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(
                            Icons.Outlined.Add,
                            contentDescription = "Agregar foto",
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(22.dp),
                        )
                        Text(
                            "Agregar",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline,
                        )
                    }
                }
            }
        }
    }

    // Firma
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Firma", style = MaterialTheme.typography.titleMedium)

            if (orden.firmada) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    StatusChip(text = "Firmada", tipo = TipoEstado.SUCCESS)
                    if (!orden.nombreFirmante.isNullOrBlank()) {
                        Text(
                            text = "por ${orden.nombreFirmante}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                val imageBitmap = remember(orden.firmaBase64) {
                    orden.firmaBase64?.let { b64 ->
                        runCatching {
                            val bytes = Base64.decode(b64, Base64.DEFAULT)
                            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
                        }.getOrNull()
                    }
                }
                if (imageBitmap != null) {
                    Image(
                        bitmap = imageBitmap,
                        contentDescription = "Firma de ${orden.nombreFirmante}",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .clip(MaterialTheme.shapes.medium),
                    )
                }
            } else {
                Text(
                    text = "Esta orden aún no tiene firma.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun VistaPreviaOverlay(
    orden: Orden,
    onBack: () -> Unit,
    onDescargar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column {
            // TopAppBar del overlay
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Volver")
                    }
                    Column(modifier = Modifier.weight(1f).padding(start = 4.dp)) {
                        Text("Vista previa", style = MaterialTheme.typography.titleLarge)
                        Text(
                            orden.numero,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = onDescargar) {
                        Icon(
                            Icons.Outlined.Download,
                            contentDescription = "Descargar PDF",
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
                HorizontalDivider()
            }

            // Documento
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp),
            ) {
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    // Cabecera del documento
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.onPrimaryContainer)
                            .padding(16.dp),
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                "DOCUMENTO — NO VÁLIDO COMO FACTURA",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.6f),
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(
                                    "ORDEN DE TRABAJO",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    fontWeight = FontWeight.Bold,
                                )
                                Text(
                                    "N° ${orden.numero}",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
                                )
                            }
                        }
                    }

                    HorizontalDivider()

                    // Datos de la orden
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            "DATOS DE LA ORDEN",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary,
                            fontWeight = FontWeight.Bold,
                        )
                        InfoRow(label = "Cliente",     value = orden.clienteNombre)
                        InfoRow(label = "Fecha",       value = orden.fecha)
                        InfoRow(label = "Tipo",        value = orden.tipo)
                        InfoRow(label = "Nº de orden", value = orden.numero)
                        if (orden.observaciones.isNotBlank()) {
                            InfoRow(label = "Observaciones", value = orden.observaciones)
                        }
                    }

                    HorizontalDivider()

                    // Estado firma
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "FIRMA DEL CLIENTE",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary,
                            fontWeight = FontWeight.Bold,
                        )
                        if (orden.firmada) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                StatusChip(text = "Firmada", tipo = TipoEstado.SUCCESS)
                                if (!orden.nombreFirmante.isNullOrBlank()) {
                                    Text(
                                        "por ${orden.nombreFirmante}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        } else {
                            StatusChip(text = "Sin firmar", tipo = TipoEstado.WARNING)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}
