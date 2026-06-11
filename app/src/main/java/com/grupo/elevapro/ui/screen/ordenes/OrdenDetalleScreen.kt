package com.grupo.elevapro.ui.screen.ordenes

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.grupo.elevapro.data.model.domain.Orden
import com.grupo.elevapro.data.repository.OrdenesRepository
import com.grupo.elevapro.ui.components.ElevaProTopAppBar
import com.grupo.elevapro.ui.components.FilledPrimaryButton
import com.grupo.elevapro.ui.components.StatusChip
import com.grupo.elevapro.ui.components.TipoEstado
import com.grupo.elevapro.ui.navigation.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
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

    Scaffold(
        topBar = { ElevaProTopAppBar(titulo = titulo, onBack = onBack) },
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

            is DetalleUiState.Success -> OrdenDetalleBody(
                orden    = s.orden,
                onFirmar = onFirmar,
                modifier = Modifier.fillMaxSize().padding(padding),
            )
        }
    }
}

@Composable
private fun OrdenDetalleBody(
    orden: Orden,
    onFirmar: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
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

        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
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
                    FilledPrimaryButton(
                        text = "Capturar firma",
                        onClick = { onFirmar(orden.id) },
                        modifier = Modifier.fillMaxWidth(),
                    )
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
