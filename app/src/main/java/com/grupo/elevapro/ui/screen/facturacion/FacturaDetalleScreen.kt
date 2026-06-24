package com.grupo.elevapro.ui.screen.facturacion

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Business
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.grupo.elevapro.ui.navigation.Screen
import com.grupo.elevapro.ui.theme.ErrorContainer
import kotlinx.coroutines.launch
import com.grupo.elevapro.ui.theme.ErrorRed
import com.grupo.elevapro.ui.theme.OnErrorContainer
import com.grupo.elevapro.ui.theme.OnSuccessContainer
import com.grupo.elevapro.ui.theme.OnTertiaryContainer
import com.grupo.elevapro.ui.theme.Success
import com.grupo.elevapro.ui.theme.SuccessContainer
import com.grupo.elevapro.ui.theme.TertiaryContainer
import com.grupo.elevapro.ui.theme.Warning
import com.grupo.elevapro.ui.theme.WarningContainer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import com.grupo.elevapro.ui.util.PdfGenerator
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

// ── Colores por estado ────────────────────────────────────────────────────────
private data class EstadoConfig(
    val heroBg: Color,
    val heroText: Color,
    val iconBg: Color,
    val badgeBg: Color,
    val badgeText: Color,
)

@Composable
private fun estadoConfig(estado: EstadoFactura): EstadoConfig = when (estado) {
    EstadoFactura.APROBADA -> EstadoConfig(
        heroBg = TertiaryContainer,
        heroText = OnTertiaryContainer,
        iconBg = com.grupo.elevapro.ui.theme.Tertiary,
        badgeBg = TertiaryContainer,
        badgeText = OnTertiaryContainer,
    )
    EstadoFactura.PENDIENTE -> EstadoConfig(
        heroBg = Color(0xFFFEF3C7),
        heroText = Color(0xFF78350F),
        iconBg = Color(0xFF92400E),
        badgeBg = Color(0xFFFEF3C7),
        badgeText = Color(0xFF78350F),
    )
    EstadoFactura.RECHAZADA -> EstadoConfig(
        heroBg = ErrorContainer,
        heroText = OnErrorContainer,
        iconBg = ErrorRed,
        badgeBg = ErrorContainer,
        badgeText = OnErrorContainer,
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
    val context = LocalContext.current
    val snackbarHost = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHost) },
        topBar = {
            when (estado) {
                is FacturaDetalleUiState.Success -> {
                    val cfg = estadoConfig(estado.factura.estado)
                    val chipLabel = when (estado.factura.estado) {
                        EstadoFactura.APROBADA -> "Aprobada"
                        EstadoFactura.PENDIENTE -> "Pendiente"
                        EstadoFactura.RECHAZADA -> "Rechazada"
                    }
                    ElevaProTopAppBar(
                        titulo = "Detalle de factura",
                        subtitulo = estado.factura.numero,
                        onBack = onBack,
                        acciones = {
                            Surface(
                                color = cfg.badgeBg,
                                shape = CircleShape,
                                modifier = Modifier.padding(end = 8.dp),
                            ) {
                                Text(
                                    text = chipLabel,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                                    color = cfg.badgeText,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                )
                            }
                        },
                    )
                }
                else -> ElevaProTopAppBar(titulo = "Detalle de factura", onBack = onBack)
            }
        },
        bottomBar = {
            if (estado is FacturaDetalleUiState.Success) {
                Surface(
                    shadowElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (estado.factura.estado == EstadoFactura.PENDIENTE && estado.esAdmin) {
                            androidx.compose.material3.Button(
                                onClick = onAprobar,
                                modifier = Modifier.fillMaxWidth().height(52.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.tertiary,
                                    contentColor = MaterialTheme.colorScheme.onTertiary,
                                ),
                                shape = RoundedCornerShape(16.dp),
                            ) {
                                Icon(Icons.Outlined.Send, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                                Text("Confirmar y enviar a ARCA", style = MaterialTheme.typography.labelLarge)
                            }
                        }
                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    runCatching {
                                        val uri = PdfGenerator.generarFactura(context, estado.factura)
                                        PdfGenerator.abrir(context, uri)
                                    }.onFailure {
                                        snackbarHost.showSnackbar("Error al generar PDF")
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            shape = RoundedCornerShape(16.dp),
                        ) {
                            Icon(Icons.Outlined.Download, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                            Text("Descargar PDF", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
        },
        modifier = modifier,
    ) { padding ->
        when (estado) {
            FacturaDetalleUiState.Loading -> Text("Cargando…", modifier = Modifier.padding(padding).padding(16.dp))
            FacturaDetalleUiState.NotFound -> Text("Factura no encontrada", color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(padding).padding(16.dp))
            is FacturaDetalleUiState.Success -> FacturaDetalleBody(
                estado = estado,
                onOrdenClick = onOrdenClick,
                onRechazar = onRechazar,
                modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()),
            )
        }
    }
}

@Composable
private fun FacturaDetalleBody(
    estado: FacturaDetalleUiState.Success,
    onOrdenClick: (String) -> Unit,
    onRechazar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val factura = estado.factura
    val cfg = estadoConfig(factura.estado)
    val isConIVA = factura.tipo.contains("A") || factura.tipo.contains("B")
    val montoNeto = if (isConIVA) factura.monto / 1.21 else factura.monto
    val montoIVA = if (isConIVA) factura.monto - montoNeto else 0.0

    Column(modifier = modifier) {

        // ── Hero banner ───────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(cfg.heroBg)
                .padding(horizontal = 16.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(cfg.iconBg, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Outlined.Receipt, contentDescription = null, tint = Color.White, modifier = Modifier.size(26.dp))
            }
            Column {
                Text(
                    text = formatearMonto(factura.monto),
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = cfg.heroText,
                )
                Text(
                    text = "${factura.tipo} · ${factura.fecha}",
                    style = MaterialTheme.typography.bodySmall,
                    color = cfg.iconBg,
                )
            }
        }

        HorizontalDivider()

        Column(modifier = Modifier.padding(vertical = 20.dp), verticalArrangement = Arrangement.spacedBy(0.dp)) {

            // ── Cliente ───────────────────────────────────────────────────────
            Column(modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionLabel("Cliente")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(12.dp))
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Outlined.Business, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    Column {
                        Text(factura.clienteNombre, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium), color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Text("ID Cliente: ${factura.clienteId}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            HorizontalDivider()

            // ── Datos de facturación ──────────────────────────────────────────
            Column(modifier = Modifier.padding(horizontal = 16.dp).padding(top = 20.dp, bottom = 20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionLabel("Datos de facturación")
                Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), modifier = Modifier.fillMaxWidth()) {
                    Column {
                        InfoRow(icon = Icons.Outlined.Receipt, label = "Número de comprobante", value = factura.numero)
                        HorizontalDivider()
                        InfoRow(icon = Icons.Outlined.CreditCard, label = "Tipo de comprobante", value = "Factura ${factura.tipo}")
                        HorizontalDivider()
                        InfoRow(icon = Icons.Outlined.CalendarMonth, label = "Fecha de emisión", value = factura.fecha)
                    }
                }
            }

            HorizontalDivider()

            // ── Detalle económico ─────────────────────────────────────────────
            Column(modifier = Modifier.padding(horizontal = 16.dp).padding(top = 20.dp, bottom = 20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionLabel("Detalle económico")
                Surface(shape = RoundedCornerShape(12.dp), color = Color.White, shadowElevation = 1.dp, modifier = Modifier.fillMaxWidth()) {
                    Column {
                        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Monto neto", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(formatearMonto(montoNeto), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium))
                        }
                        if (isConIVA) {
                            HorizontalDivider()
                            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("IVA 21%", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(formatearMonto(montoIVA), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium))
                            }
                        }
                        HorizontalDivider()
                        Row(
                            modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)).padding(horizontal = 16.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("Total", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold))
                            Text(formatearMonto(factura.monto), style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary))
                        }
                    }
                }
            }

            // ── CAE (solo si Aprobada) ────────────────────────────────────────
            if (factura.estado == EstadoFactura.APROBADA && factura.cae != null) {
                HorizontalDivider()
                Column(modifier = Modifier.padding(horizontal = 16.dp).padding(top = 20.dp, bottom = 20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionLabel("Autorización AFIP / ARCA")
                    Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), modifier = Modifier.fillMaxWidth()) {
                        Column {
                            InfoRow(icon = Icons.Outlined.CreditCard, label = "CAE", value = factura.cae, mono = true)
                            if (factura.vencimientoCae != null) {
                                HorizontalDivider()
                                InfoRow(icon = Icons.Outlined.CalendarMonth, label = "Vencimiento CAE", value = factura.vencimientoCae)
                            }
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().background(TertiaryContainer, RoundedCornerShape(12.dp)).padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = com.grupo.elevapro.ui.theme.Tertiary, modifier = Modifier.size(16.dp))
                        Text("Factura electrónica autorizada por AFIP / ARCA. CAE válido.", style = MaterialTheme.typography.bodySmall, color = OnTertiaryContainer)
                    }
                }
            }

            // ── Mensaje Pendiente ─────────────────────────────────────────────
            if (factura.estado == EstadoFactura.PENDIENTE) {
                HorizontalDivider()
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp).background(Color(0xFFFEF3C7), RoundedCornerShape(12.dp)).padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Icon(Icons.Outlined.CalendarMonth, contentDescription = null, tint = Color(0xFF92400E), modifier = Modifier.size(18.dp))
                    Text("Esta factura está pendiente de envío a AFIP / ARCA. Confirmá los datos antes de emitirla.", style = MaterialTheme.typography.bodySmall, color = Color(0xFF78350F))
                }
            }

            // ── Órdenes asociadas ─────────────────────────────────────────────
            if (factura.ordenesIds.isNotEmpty()) {
                HorizontalDivider()
                Column(modifier = Modifier.padding(horizontal = 16.dp).padding(top = 20.dp, bottom = 20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionLabel("Órdenes de trabajo asociadas (${factura.ordenesIds.size})")
                    factura.ordenesIds.forEach { ordenId ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.White, RoundedCornerShape(12.dp))
                                .clickable { onOrdenClick(ordenId) }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = Modifier.size(36.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(Icons.Outlined.Link, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("#$ordenId", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.primary))
                            }
                            Text("Ver orden →", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                        }
                    }
                }
            }

            // ── Botón Rechazar (solo admin + pendiente) ───────────────────────
            if (estado.esAdmin && factura.estado == EstadoFactura.PENDIENTE) {
                HorizontalDivider()
                OutlinedButton(
                    onClick = onRechazar,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(top = 16.dp).height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) {
                    Text("Rechazar factura", style = MaterialTheme.typography.labelLarge)
                }
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun SectionLabel(label: String) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.5.sp),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun InfoRow(icon: ImageVector, label: String, value: String, mono: Boolean = false) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 3.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
            Text(
                text = value,
                style = if (mono) MaterialTheme.typography.bodyMedium.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                        else MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

// ─── Previews ───────────────────────────────────────────────────────────────

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, name = "FacturaDetalle – aprobada")
@Composable
private fun FacturaDetalleAprobadaPreview() {
    com.grupo.elevapro.ui.theme.ElevaProTheme {
        FacturaDetalleContent(
            estado = FacturaDetalleUiState.Success(factura = com.grupo.elevapro.data.repository.FakeMockData.facturas.first(), esAdmin = true),
            onBack = {}, onOrdenClick = {}, onAprobar = {}, onRechazar = {},
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, name = "FacturaDetalle – pendiente admin")
@Composable
private fun FacturaDetallePendientePreview() {
    com.grupo.elevapro.ui.theme.ElevaProTheme {
        FacturaDetalleContent(
            estado = FacturaDetalleUiState.Success(factura = com.grupo.elevapro.data.repository.FakeMockData.facturas[1], esAdmin = true),
            onBack = {}, onOrdenClick = {}, onAprobar = {}, onRechazar = {},
        )
    }
}
