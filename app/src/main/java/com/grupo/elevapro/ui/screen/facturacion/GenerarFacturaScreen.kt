package com.grupo.elevapro.ui.screen.facturacion

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.FileOpen
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.grupo.elevapro.data.model.domain.Cliente
import com.grupo.elevapro.data.model.domain.EstadoFactura
import com.grupo.elevapro.data.model.domain.Factura
import com.grupo.elevapro.data.model.domain.Orden
import com.grupo.elevapro.data.repository.ClienteRepository
import com.grupo.elevapro.data.repository.FacturacionRepository
import com.grupo.elevapro.data.repository.FakeMockData
import com.grupo.elevapro.data.repository.OrdenesRepository
import com.grupo.elevapro.ui.components.ElevaProTextField
import com.grupo.elevapro.ui.components.ElevaProTopAppBar
import com.grupo.elevapro.ui.components.FilledPrimaryButton
import com.grupo.elevapro.ui.theme.OnTertiaryContainer
import com.grupo.elevapro.ui.theme.TertiaryContainer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

// ─── Form state ──────────────────────────────────────────────────────────────

data class GenerarFacturaFormState(
    val clienteId: String = "",
    val clienteNombre: String = "",
    val tipo: String = "",
    val fechaDesde: String = "",
    val fechaHasta: String = "",
    val cuit: String = "",
    val tipoPago: String = "",
    val descripcion: String = "",
    val montoNetoStr: String = "",
    val ordenVinculadaId: String = "",
    // legacy — keep for ViewModel compatibility
    val ordenesSeleccionadas: Set<String> = emptySet(),
    val articulosConCantidad: Map<String, Int> = emptyMap(),
    val generando: Boolean = false,
    val error: String? = null,
) {
    val montoNeto: Double get() = montoNetoStr.replace(",", ".").toDoubleOrNull() ?: 0.0
    val isConIVA: Boolean get() = tipo == "A" || tipo == "B"
    val montoIVA: Double get() = if (isConIVA) montoNeto * 0.21 else 0.0
    val montoTotal: Double get() = montoNeto + montoIVA
    val isFormValid: Boolean
        get() = clienteId.isNotBlank() && tipo.isNotBlank() && fechaDesde.isNotBlank() &&
                fechaHasta.isNotBlank() && cuit.isNotBlank() && tipoPago.isNotBlank() &&
                descripcion.isNotBlank() && montoNeto > 0
}

// ─── UiState ─────────────────────────────────────────────────────────────────

sealed interface GenerarFacturaUiState {
    data object Loading : GenerarFacturaUiState
    data class Formulario(
        val form: GenerarFacturaFormState,
        val clientes: List<Cliente>,
        val ordenesFirmadas: List<Orden>,
    ) : GenerarFacturaUiState
    data object Generado : GenerarFacturaUiState
}

// ─── ViewModel ───────────────────────────────────────────────────────────────

@HiltViewModel
class GenerarFacturaViewModel @Inject constructor(
    private val clienteRepository: ClienteRepository,
    ordenesRepository: OrdenesRepository,
    private val facturacionRepository: FacturacionRepository,
) : ViewModel() {

    private val _form = MutableStateFlow(GenerarFacturaFormState())
    private val _estado = MutableStateFlow<GenerarFacturaUiState>(GenerarFacturaUiState.Loading)
    val estado: StateFlow<GenerarFacturaUiState> = _estado.asStateFlow()

    init {
        combine(
            clienteRepository.observarClientes(),
            ordenesRepository.observarOrdenes(),
            _form,
        ) { clientes, ordenes, form ->
            GenerarFacturaUiState.Formulario(
                form = form,
                clientes = clientes,
                ordenesFirmadas = ordenes.filter { it.firmada && !it.facturado },
            )
        }.onEach { nuevo ->
            if (_estado.value !is GenerarFacturaUiState.Generado) _estado.value = nuevo
        }.launchIn(viewModelScope)
    }

    fun onCliente(id: String, nombre: String) {
        val cliente = FakeMockData.clientes.find { it.id == id }
        updateForm { copy(clienteId = id, clienteNombre = nombre, cuit = cliente?.cuit ?: "") }
    }
    fun onTipo(t: String) = updateForm { copy(tipo = t) }
    fun onFechaDesde(f: String) = updateForm { copy(fechaDesde = f) }
    fun onFechaHasta(f: String) = updateForm { copy(fechaHasta = f) }
    fun onCuit(v: String) = updateForm { copy(cuit = v) }
    fun onTipoPago(v: String) = updateForm { copy(tipoPago = v) }
    fun onDescripcion(v: String) = updateForm { copy(descripcion = v) }
    fun onMontoNeto(v: String) = updateForm { copy(montoNetoStr = v.filter { it.isDigit() || it == ',' || it == '.' }) }
    fun onOrdenVinculada(id: String) = updateForm { copy(ordenVinculadaId = id) }

    fun generar() {
        val formulario = (_estado.value as? GenerarFacturaUiState.Formulario) ?: return
        if (!formulario.form.isFormValid) return
        viewModelScope.launch {
            updateForm { copy(generando = true, error = null) }
            try {
                val form = formulario.form
                facturacionRepository.agregar(
                    Factura(
                        id = UUID.randomUUID().toString(),
                        numero = "0001-${(10000000..99999999).random()}",
                        tipo = form.tipo,
                        clienteId = form.clienteId,
                        clienteNombre = form.clienteNombre,
                        fecha = form.fechaDesde,
                        monto = form.montoTotal,
                        estado = EstadoFactura.PENDIENTE,
                        ordenesIds = if (form.ordenVinculadaId.isNotBlank()) listOf(form.ordenVinculadaId) else emptyList(),
                    )
                )
                _estado.value = GenerarFacturaUiState.Generado
            } catch (e: Exception) {
                updateForm { copy(generando = false, error = "No se pudo generar la factura") }
            }
        }
    }

    private fun updateForm(block: GenerarFacturaFormState.() -> GenerarFacturaFormState) {
        _form.update { it.block() }
    }
}

// ─── Screen ──────────────────────────────────────────────────────────────────

@Composable
fun GenerarFacturaScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: GenerarFacturaViewModel = hiltViewModel(),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    LaunchedEffect(estado) { if (estado is GenerarFacturaUiState.Generado) onBack() }
    val formulario = estado as? GenerarFacturaUiState.Formulario
    GenerarFacturaContent(
        formulario = formulario,
        onBack = onBack,
        onCliente = viewModel::onCliente,
        onTipo = viewModel::onTipo,
        onFechaDesde = viewModel::onFechaDesde,
        onFechaHasta = viewModel::onFechaHasta,
        onCuit = viewModel::onCuit,
        onTipoPago = viewModel::onTipoPago,
        onDescripcion = viewModel::onDescripcion,
        onMontoNeto = viewModel::onMontoNeto,
        onOrdenVinculada = viewModel::onOrdenVinculada,
        onGenerar = viewModel::generar,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GenerarFacturaContent(
    formulario: GenerarFacturaUiState.Formulario?,
    onBack: () -> Unit,
    onCliente: (String, String) -> Unit,
    onTipo: (String) -> Unit,
    onFechaDesde: (String) -> Unit,
    onFechaHasta: (String) -> Unit,
    onCuit: (String) -> Unit,
    onTipoPago: (String) -> Unit,
    onDescripcion: (String) -> Unit,
    onMontoNeto: (String) -> Unit,
    onOrdenVinculada: (String) -> Unit,
    onGenerar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        topBar = {
            ElevaProTopAppBar(
                titulo = "Generar Factura",
                onBack = onBack,
                acciones = {
                    Icon(Icons.Outlined.Receipt, contentDescription = null, tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(end = 12.dp))
                },
            )
        },
        bottomBar = {
            Surface(shadowElevation = 8.dp, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (formulario?.form?.error != null) {
                        Text(formulario.form.error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                    FilledPrimaryButton(
                        text = if (formulario?.form?.generando == true) "Generando…" else "Generar vista previa",
                        onClick = onGenerar,
                        enabled = formulario?.form?.isFormValid == true && formulario.form.generando == false,
                        icon = Icons.Outlined.Receipt,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        },
        modifier = modifier,
    ) { padding ->
        if (formulario == null) {
            Text("Cargando…", modifier = Modifier.padding(padding).padding(16.dp))
            return@Scaffold
        }
        val form = formulario.form

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {

            // ── Período de facturación ────────────────────────────────────────
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SeccionHeader("Período de facturación")
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    DatePickerField(
                        label = "Fecha desde *",
                        valor = form.fechaDesde,
                        onFecha = onFechaDesde,
                        modifier = Modifier.weight(1f),
                    )
                    DatePickerField(
                        label = "Fecha hasta *",
                        valor = form.fechaHasta,
                        onFecha = onFechaHasta,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            // ── Datos de la factura ───────────────────────────────────────────
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SeccionHeader("Datos de la factura")
                TipoDropdown(
                    seleccionado = form.tipo,
                    onSeleccion = onTipo,
                    modifier = Modifier.fillMaxWidth(),
                )
                ClienteDropdown(
                    clientes = formulario.clientes,
                    seleccionadoNombre = form.clienteNombre,
                    onSeleccion = onCliente,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (form.clienteNombre.isNotBlank()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Outlined.Description, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(end = 4.dp))
                        Text(form.clienteNombre, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
                ElevaProTextField(
                    value = form.cuit,
                    onValueChange = onCuit,
                    label = "CUIT *",
                    leadingIcon = Icons.Outlined.CreditCard,
                    modifier = Modifier.fillMaxWidth(),
                )
                // Tipo de pago dropdown
                TipoPagoDropdown(
                    seleccionado = form.tipoPago,
                    onSeleccion = onTipoPago,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            // ── Montos ────────────────────────────────────────────────────────
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SeccionHeader("Montos")
                ElevaProTextField(
                    value = form.descripcion,
                    onValueChange = onDescripcion,
                    label = "Descripción del servicio *",
                    leadingIcon = Icons.Outlined.FileOpen,
                    singleLine = false,
                    modifier = Modifier.fillMaxWidth(),
                )
                ElevaProTextField(
                    value = form.montoNetoStr,
                    onValueChange = onMontoNeto,
                    label = if (form.tipo == "C") "Monto total *" else "Monto neto *",
                    leadingIcon = Icons.Outlined.Receipt,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
                // IVA breakdown — visible si hay monto y tipo
                if (form.montoNetoStr.isNotBlank() && form.tipo.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Subtotal", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(formatearMonto(form.montoNeto), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium))
                            }
                            if (form.isConIVA) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("IVA 21%", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(formatearMonto(form.montoIVA), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium))
                                }
                            }
                            HorizontalDivider()
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text("Total", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold))
                                Text(formatearMonto(form.montoTotal), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary))
                            }
                        }
                    }
                }
            }

            // ── Vincular orden (opcional) ─────────────────────────────────────
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SeccionHeader("Vincular orden (opcional)")
                OrdenVinculadaDropdown(
                    ordenes = formulario.ordenesFirmadas,
                    seleccionadoId = form.ordenVinculadaId,
                    onSeleccion = onOrdenVinculada,
                    modifier = Modifier.fillMaxWidth(),
                )
                val ordenSel = formulario.ordenesFirmadas.find { it.id == form.ordenVinculadaId }
                if (ordenSel != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(TertiaryContainer, RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = com.grupo.elevapro.ui.theme.Tertiary, modifier = Modifier.padding(top = 2.dp))
                        Column {
                            Text("Orden vinculada con firma de conformidad", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium), color = OnTertiaryContainer)
                            Text("${ordenSel.clienteNombre} · ${ordenSel.fecha}", style = MaterialTheme.typography.labelSmall, color = com.grupo.elevapro.ui.theme.Tertiary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SeccionHeader(label: String) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    HorizontalDivider(modifier = Modifier.padding(top = 4.dp))
}

// ── OrdenVinculadaDropdown ────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OrdenVinculadaDropdown(
    ordenes: List<Orden>,
    seleccionadoId: String,
    onSeleccion: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expandido by remember { mutableStateOf(false) }
    val selLabel = ordenes.find { it.id == seleccionadoId }?.let { "${it.numero} · ${it.clienteNombre}" } ?: "Sin vincular"

    ExposedDropdownMenuBox(
        expanded = expandido,
        onExpandedChange = { expandido = it },
        modifier = modifier,
    ) {
        OutlinedTextField(
            value = selLabel,
            onValueChange = {},
            readOnly = true,
            label = { Text("Orden de trabajo firmada") },
            leadingIcon = { Icon(Icons.Outlined.Link, contentDescription = null) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandido) },
            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(expanded = expandido, onDismissRequest = { expandido = false }) {
            DropdownMenuItem(
                text = { Text("Sin vincular") },
                onClick = { onSeleccion(""); expandido = false },
            )
            ordenes.forEach { orden ->
                DropdownMenuItem(
                    text = { Text("${orden.numero} · ${orden.clienteNombre} · ${orden.fecha}") },
                    onClick = { onSeleccion(orden.id); expandido = false },
                )
            }
        }
    }
}

// ── TipoPagoDropdown ─────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TipoPagoDropdown(
    seleccionado: String,
    onSeleccion: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val opciones = listOf("Efectivo", "Transferencia Bancaria", "Cheque", "Tarjeta de Crédito", "Tarjeta de Débito", "Cuenta Corriente")
    var expandido by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expandido,
        onExpandedChange = { expandido = it },
        modifier = modifier,
    ) {
        OutlinedTextField(
            value = seleccionado,
            onValueChange = {},
            readOnly = true,
            label = { Text("Tipo de pago *") },
            leadingIcon = { Icon(Icons.Outlined.CreditCard, contentDescription = null) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandido) },
            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(expanded = expandido, onDismissRequest = { expandido = false }) {
            opciones.forEach { op ->
                DropdownMenuItem(
                    text = { Text(op) },
                    onClick = { onSeleccion(op); expandido = false },
                )
            }
        }
    }
}
