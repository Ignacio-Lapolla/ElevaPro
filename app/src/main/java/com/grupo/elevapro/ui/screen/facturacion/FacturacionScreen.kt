package com.grupo.elevapro.ui.screen.facturacion

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
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
import com.grupo.elevapro.ui.components.FilterChipBar
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

import javax.inject.Inject

sealed interface FacturacionUiState {
    data object Loading : FacturacionUiState
    data object SinPermiso : FacturacionUiState
    data class Success(val facturas: List<Factura>, val filtro: EstadoFactura?) : FacturacionUiState
    data class Error(val msg: String) : FacturacionUiState
}

private val OPCIONES_FILTRO: List<Pair<EstadoFactura?, String>> = listOf(
    null to "Todas",
    EstadoFactura.PENDIENTE to "Pendientes",
    EstadoFactura.APROBADA to "Aprobadas",
    EstadoFactura.RECHAZADA to "Rechazadas",
)

@HiltViewModel
class FacturacionViewModel @Inject constructor(
    private val facturasRepo: FacturacionRepository,
    private val authRepo: AuthRepository,
) : ViewModel() {

    private val filtro = MutableStateFlow<EstadoFactura?>(null)

    val estado: StateFlow<FacturacionUiState> = combine(
        facturasRepo.observarFacturas(),
        filtro,
        authRepo.usuarioActual,
    ) { lista, f, user ->
        when {
            user?.rol != Rol.ADMINISTRADOR -> FacturacionUiState.SinPermiso
            else -> FacturacionUiState.Success(
                facturas = lista.filter { f == null || it.estado == f },
                filtro = f,
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = FacturacionUiState.Loading,
    )

    fun onFiltro(f: EstadoFactura?) { filtro.value = f }
}

@Composable
fun FacturacionScreen(
    onFacturaClick: (String) -> Unit,
    onGenerarFactura: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FacturacionViewModel = hiltViewModel(),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    FacturacionContent(
        estado = estado,
        onFiltro = viewModel::onFiltro,
        onFacturaClick = onFacturaClick,
        onGenerarFactura = onGenerarFactura,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FacturacionContent(
    estado: FacturacionUiState,
    onFiltro: (EstadoFactura?) -> Unit,
    onFacturaClick: (String) -> Unit,
    onGenerarFactura: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        topBar = {
            ElevaProTopAppBar(
                titulo = "Facturación",
                acciones = {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        shape = MaterialTheme.shapes.small,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        ) {
                            Icon(
                                Icons.Outlined.Lock,
                                contentDescription = null,
                                modifier = Modifier.padding(end = 2.dp),
                            )
                            Text("Solo Admin", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                },
            )
        },
        floatingActionButton = {
            if (estado is FacturacionUiState.Success) {
                FloatingActionButton(
                    onClick = onGenerarFactura,
                    containerColor = MaterialTheme.colorScheme.tertiary,
                    contentColor = MaterialTheme.colorScheme.onTertiary,
                ) {
                    Icon(Icons.Outlined.Add, contentDescription = "Generar factura")
                }
            }
        },
        modifier = modifier,
    ) { padding ->
        when (estado) {
            FacturacionUiState.Loading -> Text(
                "Cargando…",
                modifier = Modifier.padding(padding).padding(16.dp),
            )
            FacturacionUiState.SinPermiso -> SoloAdminContent(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            )
            is FacturacionUiState.Error -> Text(
                text = estado.msg,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(padding).padding(16.dp),
            )
            is FacturacionUiState.Success -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            ) {
                // Banner informativo
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = MaterialTheme.shapes.medium,
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Icon(
                            Icons.Outlined.Shield,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                        Text(
                            "Información financiera confidencial — acceso restringido al rol Administrador.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }

                FilterChipBar(
                    opciones = OPCIONES_FILTRO.map { it.second },
                    seleccionada = OPCIONES_FILTRO.first { it.first == estado.filtro }.second,
                    onSeleccion = { label ->
                        onFiltro(OPCIONES_FILTRO.first { it.second == label }.first)
                    },
                )

                Spacer(Modifier.height(8.dp))

                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(estado.facturas, key = { it.id }) { factura ->
                        FacturaCard(
                            factura = factura,
                            onClick = { onFacturaClick(factura.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FacturaCard(
    factura: Factura,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
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

    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    Icons.Outlined.Receipt,
                    contentDescription = "Factura",
                    tint = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = factura.clienteNombre,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                StatusChip(text = chipLabel, tipo = chipTipo)
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Icon(
                    Icons.Outlined.CalendarMonth,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 2.dp),
                )
                Text(
                    text = "${factura.fecha} · Factura ${factura.tipo}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = formatearMonto(factura.monto),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                ),
            )
            if (factura.ordenesIds.isNotEmpty()) {
                HorizontalDivider()
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        Icons.Outlined.Receipt,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 2.dp),
                    )
                    Text(
                        text = "Orden #${factura.ordenesIds.first()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (factura.vencimientoCae != null) {
                        Spacer(Modifier.weight(1f))
                        Text(
                            text = factura.vencimientoCae,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SoloAdminContent(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            Icons.Outlined.Lock,
            contentDescription = "Acceso restringido",
            modifier = Modifier
                .padding(bottom = 16.dp)
                .run { this },
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = "Acceso restringido",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "La sección de Facturación es exclusiva para el rol Administrador.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// ─── Previews ───────────────────────────────────────────────────────────────

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, name = "Facturación – lista admin")
@Composable
private fun FacturacionSuccessPreview() {
    com.grupo.elevapro.ui.theme.ElevaProTheme {
        FacturacionContent(
            estado = FacturacionUiState.Success(
                facturas = com.grupo.elevapro.data.repository.FakeMockData.facturas,
                filtro = null,
            ),
            onFiltro = {},
            onFacturaClick = {},
            onGenerarFactura = {},
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, name = "Facturación – solo admin (operativo)")
@Composable
private fun FacturacionSoloAdminPreview() {
    com.grupo.elevapro.ui.theme.ElevaProTheme {
        FacturacionContent(
            estado = FacturacionUiState.SinPermiso,
            onFiltro = {},
            onFacturaClick = {},
            onGenerarFactura = {},
        )
    }
}

