package com.grupo.elevapro.ui.screen.facturacion

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.grupo.elevapro.data.model.domain.EstadoFactura
import com.grupo.elevapro.data.model.domain.Factura
import com.grupo.elevapro.data.model.domain.Rol
import com.grupo.elevapro.data.repository.AuthRepository
import com.grupo.elevapro.data.repository.FacturacionRepository
import com.grupo.elevapro.ui.components.ElevaProTopAppBar
import com.grupo.elevapro.ui.components.StatusChip
import com.grupo.elevapro.ui.components.TipoEstado
import com.grupo.elevapro.ui.navigation.Screen
import com.grupo.elevapro.ui.theme.ErrorRed
import com.grupo.elevapro.ui.theme.Success
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

import javax.inject.Inject

sealed interface FacturaDetalleUiState {
    data object Loading : FacturaDetalleUiState
    data object NotFound : FacturaDetalleUiState
    data class Success(
        val factura: Factura,
        val esAdmin: Boolean,
        val procesando: Boolean = false,
    ) : FacturaDetalleUiState {
        val montoNeto: Double get() = factura.monto / 1.21
        val montoIva: Double get() = factura.monto - montoNeto
    }
}

@HiltViewModel
class FacturaDetalleViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val facturacionRepository: FacturacionRepository,
    authRepository: AuthRepository,
) : ViewModel() {

    private val facturaId: String = checkNotNull(savedStateHandle[Screen.FacturaDetalle.ARG_ID])

    val estado: StateFlow<FacturaDetalleUiState> = combine(
        facturacionRepository.observarFacturas(),
        authRepository.usuarioActual,
    ) { facturas, usuario ->
        val factura = facturas.find { it.id == facturaId }
            ?: return@combine FacturaDetalleUiState.NotFound
        FacturaDetalleUiState.Success(
            factura = factura,
            esAdmin = usuario?.rol == Rol.ADMINISTRADOR,
        ) as FacturaDetalleUiState
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = FacturaDetalleUiState.Loading,
    )

    fun aprobar() = cambiarEstado(EstadoFactura.APROBADA)
    fun rechazar() = cambiarEstado(EstadoFactura.RECHAZADA)

    private fun cambiarEstado(nuevoEstado: EstadoFactura) {
        viewModelScope.launch {
            facturacionRepository.actualizarEstado(facturaId, nuevoEstado)
        }
    }
}

@Composable
fun FacturaDetalleScreen(
    onBack: () -> Unit,
    onOrdenClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FacturaDetalleViewModel = hiltViewModel(),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    FacturaDetalleContent(
        estado = estado,
        onBack = onBack,
        onOrdenClick = onOrdenClick,
        onAprobar = viewModel::aprobar,
        onRechazar = viewModel::rechazar,
        modifier = modifier,
    )
}

@Composable
private fun FacturaDetalleContent(
    estado: FacturaDetalleUiState,
    onBack: () -> Unit,
    onOrdenClick: (String) -> Unit,
    onAprobar: () -> Unit,
    onRechazar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val titulo = if (estado is FacturaDetalleUiState.Success)
        "Factura ${estado.factura.numero}"
    else
        "Detalle de factura"

    Scaffold(
        topBar = { ElevaProTopAppBar(titulo = titulo, onBack = onBack) },
        modifier = modifier,
    ) { padding ->
        when (estado) {
            FacturaDetalleUiState.Loading -> Text(
                "Cargando…",
                modifier = Modifier.padding(padding).padding(16.dp),
            )
            FacturaDetalleUiState.NotFound -> Text(
                "Factura no encontrada",
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(padding).padding(16.dp),
            )
            is FacturaDetalleUiState.Success -> FacturaDetalleBody(
                estado = estado,
                onOrdenClick = onOrdenClick,
                onAprobar = onAprobar,
                onRechazar = onRechazar,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState()),
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FacturaDetalleBody(
    estado: FacturaDetalleUiState.Success,
    onOrdenClick: (String) -> Unit,
    onAprobar: () -> Unit,
    onRechazar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val factura = estado.factura
    val chipTipo = when (factura.estado) {
        EstadoFactura.PENDIENTE -> TipoEstado.WARNING
        EstadoFactura.APROBADA -> TipoEstado.SUCCESS
        EstadoFactura.RECHAZADA -> TipoEstado.ERROR
    }
    val chipLabel = when (factura.estado) {
        EstadoFactura.PENDIENTE -> "Pendiente"
        EstadoFactura.APROBADA -> "Aprobada"
        EstadoFactura.RECHAZADA -> "Rechazada"
    }
    val heroColor = when (factura.estado) {
        EstadoFactura.APROBADA -> Success.copy(alpha = 0.15f)
        EstadoFactura.RECHAZADA -> ErrorRed.copy(alpha = 0.10f)
        EstadoFactura.PENDIENTE -> MaterialTheme.colorScheme.tertiaryContainer
    }

    Column(modifier = modifier) {
        // Hero card
        Surface(
            color = heroColor,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Outlined.Receipt,
                            contentDescription = "Factura",
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            text = factura.numero,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    StatusChip(text = chipLabel, tipo = chipTipo)
                }
                Text(
                    text = formatearMonto(factura.monto),
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary,
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Outlined.CalendarMonth,
                        contentDescription = null,
                    )
                    Text(
                        text = "${factura.fecha} · Factura ${factura.tipo}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = factura.clienteNombre,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        // Detalle económico
        SeccionTituloFactura("Detalle económico")
        val neto = estado.montoNeto
        val iva = estado.montoIva
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                FilaImporte(label = "Monto neto", valor = formatearMonto(neto))
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                FilaImporte(label = "IVA 21%", valor = formatearMonto(iva))
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                FilaImporte(
                    label = "Total",
                    valor = formatearMonto(factura.monto),
                    negrita = true,
                    colorValor = MaterialTheme.colorScheme.primary,
                )
            }
        }

        // Sección CAE (solo si aprobada)
        if (factura.estado == EstadoFactura.APROBADA && factura.cae != null) {
            Spacer(Modifier.height(8.dp))
            SeccionTituloFactura("CAE")
            ListItem(
                headlineContent = { Text(factura.cae) },
                supportingContent = { Text("Número de CAE") },
                leadingContent = {
                    Icon(
                        Icons.Outlined.Key,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            )
            if (factura.vencimientoCae != null) {
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                ListItem(
                    headlineContent = { Text(factura.vencimientoCae) },
                    supportingContent = { Text("Vencimiento CAE") },
                    leadingContent = {
                        Icon(
                            Icons.Outlined.CalendarMonth,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                )
            }
        }

        // Sección Órdenes asociadas
        if (factura.ordenesIds.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            SeccionTituloFactura("Órdenes asociadas")
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
            ) {
                factura.ordenesIds.forEach { ordenId ->
                    AssistChip(
                        onClick = { onOrdenClick(ordenId) },
                        label = { Text("Orden #$ordenId") },
                    )
                }
            }
        }

        // Botones Aprobar / Rechazar (solo admin + pendiente)
        if (estado.esAdmin && factura.estado == EstadoFactura.PENDIENTE) {
            Spacer(Modifier.height(16.dp))
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(
                    onClick = onRechazar,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    Text("Rechazar")
                }
                Button(
                    onClick = onAprobar,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Success,
                        contentColor = Color.White,
                    ),
                ) {
                    Icon(
                        Icons.Outlined.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 6.dp),
                    )
                    Text("Aprobar")
                }
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun SeccionTituloFactura(titulo: String) {
    Text(
        text = titulo.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

// ─── Previews ───────────────────────────────────────────────────────────────

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, name = "FacturaDetalle – aprobada con CAE")
@Composable
private fun FacturaDetalleAprobadaPreview() {
    com.grupo.elevapro.ui.theme.ElevaProTheme {
        FacturaDetalleContent(
            estado = FacturaDetalleUiState.Success(
                factura = com.grupo.elevapro.data.repository.FakeMockData.facturas.first(),
                esAdmin = true,
            ),
            onBack = {},
            onOrdenClick = {},
            onAprobar = {},
            onRechazar = {},
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, name = "FacturaDetalle – pendiente (botones admin)")
@Composable
private fun FacturaDetallePendientePreview() {
    com.grupo.elevapro.ui.theme.ElevaProTheme {
        FacturaDetalleContent(
            estado = FacturaDetalleUiState.Success(
                factura = com.grupo.elevapro.data.repository.FakeMockData.facturas[1],
                esAdmin = true,
            ),
            onBack = {},
            onOrdenClick = {},
            onAprobar = {},
            onRechazar = {},
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, name = "FacturaDetalle – pendiente (operativo, sin botones)")
@Composable
private fun FacturaDetallePendienteOperativoPreview() {
    com.grupo.elevapro.ui.theme.ElevaProTheme {
        FacturaDetalleContent(
            estado = FacturaDetalleUiState.Success(
                factura = com.grupo.elevapro.data.repository.FakeMockData.facturas[1],
                esAdmin = false,
            ),
            onBack = {},
            onOrdenClick = {},
            onAprobar = {},
            onRechazar = {},
        )
    }
}

