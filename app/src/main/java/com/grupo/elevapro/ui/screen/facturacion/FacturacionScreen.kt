package com.grupo.elevapro.ui.screen.facturacion

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Business
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.unit.sp
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
import com.grupo.elevapro.ui.theme.Success
import com.grupo.elevapro.ui.theme.Warning
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
    EstadoFactura.APROBADA to "Facturadas",
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
    onOpenDrawer: () -> Unit = {},
    viewModel: FacturacionViewModel = hiltViewModel(),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    FacturacionContent(
        estado = estado,
        onFiltro = viewModel::onFiltro,
        onFacturaClick = onFacturaClick,
        onGenerarFactura = onGenerarFactura,
        onOpenDrawer = onOpenDrawer,
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
    onOpenDrawer: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Scaffold(
        topBar = {
            ElevaProTopAppBar(
                titulo = "Facturas",
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Filled.Menu, contentDescription = "Abrir menú")
                    }
                },
                acciones = {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        shape = RoundedCornerShape(50),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        ) {
                            Icon(
                                Icons.Outlined.Lock,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
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
                    color = MaterialTheme.colorScheme.secondaryContainer,
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
                            Icons.Outlined.Security,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                        Text(
                            "Información financiera confidencial — acceso restringido al rol Administrador.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                    }
                }

                // Resumen financiero
                ResumenFinanciero(
                    facturas = estado.facturas,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )

                FilterChipBar(
                    opciones = OPCIONES_FILTRO.map { it.second },
                    seleccionada = OPCIONES_FILTRO.first { it.first == estado.filtro }.second,
                    onSeleccion = { label ->
                        onFiltro(OPCIONES_FILTRO.first { it.second == label }.first)
                    },
                )

                Spacer(Modifier.height(4.dp))

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
                    if (estado.facturas.isEmpty()) {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 64.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(96.dp)
                                        .background(
                                            MaterialTheme.colorScheme.surfaceVariant,
                                            RoundedCornerShape(50),
                                        ),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        Icons.Outlined.Receipt,
                                        contentDescription = null,
                                        modifier = Modifier.size(48.dp),
                                        tint = MaterialTheme.colorScheme.outline,
                                    )
                                }
                                Text(
                                    "No hay facturas",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ResumenFinanciero(
    facturas: List<Factura>,
    modifier: Modifier = Modifier,
) {
    val totalAprobado = facturas.filter { it.estado == EstadoFactura.APROBADA }.sumOf { it.monto }
    val totalPendiente = facturas.filter { it.estado == EstadoFactura.PENDIENTE }.sumOf { it.monto }
    val countAprobadas = facturas.count { it.estado == EstadoFactura.APROBADA }
    val countPendientes = facturas.count { it.estado == EstadoFactura.PENDIENTE }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Card(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Success.copy(alpha = 0.15f)),
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    "FACTURADO",
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.5.sp),
                    color = Success,
                )
                Text(
                    formatearMonto(totalAprobado),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 4.dp),
                )
                Text(
                    "$countAprobadas aprobadas",
                    style = MaterialTheme.typography.labelSmall,
                    color = Success,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
        Card(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Warning.copy(alpha = 0.15f)),
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    "PENDIENTE",
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.5.sp),
                    color = Warning,
                )
                Text(
                    formatearMonto(totalPendiente),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 4.dp),
                )
                Text(
                    "$countPendientes pendientes",
                    style = MaterialTheme.typography.labelSmall,
                    color = Warning,
                    modifier = Modifier.padding(top = 2.dp),
                )
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
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                // Cliente + estado
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        Icons.Outlined.Business,
                        contentDescription = "Factura",
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(15.dp),
                    )
                    Text(
                        text = factura.clienteNombre,
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.weight(1f),
                    )
                    StatusChip(text = chipLabel, tipo = chipTipo)
                }
                // Fecha + tipo
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(
                        Icons.Outlined.CalendarMonth,
                        contentDescription = null,
                        modifier = Modifier.size(13.dp),
                        tint = MaterialTheme.colorScheme.outline,
                    )
                    Text(
                        text = "${factura.fecha} · ${factura.tipo}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                // Monto + chevron
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = formatearMonto(factura.monto),
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        ),
                        modifier = Modifier.weight(1f),
                    )
                    Icon(
                        Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outline,
                    )
                }
            }

            // Órdenes asociadas
            if (factura.ordenesIds.isNotEmpty()) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column {
                        factura.ordenesIds.forEachIndexed { idx, ordenId ->
                            if (idx > 0) {
                                androidx.compose.material3.HorizontalDivider()
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Icon(
                                    Icons.Outlined.Receipt,
                                    contentDescription = null,
                                    modifier = Modifier.size(13.dp),
                                    tint = MaterialTheme.colorScheme.secondary,
                                )
                                Text(
                                    text = "Orden #$ordenId",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                    color = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.weight(1f),
                                )
                                if (factura.vencimientoCae != null && idx == 0) {
                                    Text(
                                        text = factura.vencimientoCae,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.outline,
                                    )
                                }
                            }
                        }
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
        Box(
            modifier = Modifier
                .size(88.dp)
                .background(
                    MaterialTheme.colorScheme.errorContainer,
                    RoundedCornerShape(24.dp),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Outlined.Lock,
                contentDescription = "Acceso restringido",
                modifier = Modifier.size(44.dp),
                tint = MaterialTheme.colorScheme.error,
            )
        }
        Spacer(Modifier.height(24.dp))
        Text(
            text = "Acceso restringido",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = "La sección de Facturas es exclusiva para administradores. Contactá a tu administrador si necesitás acceder a esta información.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(24.dp))
        Surface(
            color = MaterialTheme.colorScheme.errorContainer,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Icon(
                    Icons.Outlined.Security,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    "Tu rol actual es Operativo. Solo los Administradores pueden ver y gestionar facturas.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
        }
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
