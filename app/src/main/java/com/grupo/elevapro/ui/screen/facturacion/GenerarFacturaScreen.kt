package com.grupo.elevapro.ui.screen.facturacion

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.grupo.elevapro.data.model.domain.Articulo
import com.grupo.elevapro.data.model.domain.Cliente
import com.grupo.elevapro.data.model.domain.EstadoFactura
import com.grupo.elevapro.data.model.domain.Factura
import com.grupo.elevapro.data.model.domain.Orden
import com.grupo.elevapro.data.repository.ClienteRepository
import com.grupo.elevapro.data.repository.FacturacionRepository
import com.grupo.elevapro.data.repository.FakeMockData
import com.grupo.elevapro.data.repository.OrdenesRepository
import com.grupo.elevapro.ui.components.ElevaProTopAppBar
import com.grupo.elevapro.ui.components.FilledPrimaryButton
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
    val ordenesSeleccionadas: Set<String> = emptySet(),
    val articulosConCantidad: Map<String, Int> = emptyMap(),
    val generando: Boolean = false,
    val error: String? = null,
) {
    val isFormValid: Boolean
        get() = clienteId.isNotBlank() && tipo.isNotBlank() && fechaDesde.isNotBlank()
}

// ─── UiState ─────────────────────────────────────────────────────────────────

sealed interface GenerarFacturaUiState {
    data object Loading : GenerarFacturaUiState
    data class Formulario(
        val form: GenerarFacturaFormState,
        val clientes: List<Cliente>,
        val ordenesPendientes: List<Orden>,
        val articulos: List<Articulo>,
    ) : GenerarFacturaUiState {
        val montoNeto: Double
            get() = form.articulosConCantidad.entries.sumOf { (artId, cant) ->
                articulos.find { it.id == artId }?.precio?.times(cant) ?: 0.0
            }
        val montoIva: Double get() = montoNeto * 0.21
        val montoTotal: Double get() = montoNeto + montoIva
    }
    data object Generado : GenerarFacturaUiState
}

// ─── ViewModel ───────────────────────────────────────────────────────────────

@HiltViewModel
class GenerarFacturaViewModel @Inject constructor(
    private val clienteRepository: ClienteRepository,
    private val ordenesRepository: OrdenesRepository,
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
            val pendientes = if (form.clienteId.isBlank()) emptyList()
            else ordenes.filter { it.clienteId == form.clienteId && !it.facturado }
            GenerarFacturaUiState.Formulario(
                form = form,
                clientes = clientes,
                ordenesPendientes = pendientes,
                articulos = FakeMockData.articulos,
            )
        }
            .onEach { nuevo ->
                if (_estado.value !is GenerarFacturaUiState.Generado) {
                    _estado.value = nuevo
                }
            }
            .launchIn(viewModelScope)
    }

    fun onCliente(id: String, nombre: String) = updateForm {
        copy(clienteId = id, clienteNombre = nombre, ordenesSeleccionadas = emptySet())
    }
    fun onTipo(t: String) = updateForm { copy(tipo = t) }
    fun onFechaDesde(f: String) = updateForm { copy(fechaDesde = f) }
    fun onFechaHasta(f: String) = updateForm { copy(fechaHasta = f) }

    fun onToggleOrden(ordenId: String) = updateForm {
        val nuevas = if (ordenId in ordenesSeleccionadas)
            ordenesSeleccionadas - ordenId
        else
            ordenesSeleccionadas + ordenId
        copy(ordenesSeleccionadas = nuevas)
    }

    fun onCantidadArticulo(articuloId: String, delta: Int) = updateForm {
        val actual = articulosConCantidad.getOrDefault(articuloId, 0)
        val nueva = (actual + delta).coerceAtLeast(0)
        val nuevoMapa = if (nueva == 0) articulosConCantidad - articuloId
        else articulosConCantidad + (articuloId to nueva)
        copy(articulosConCantidad = nuevoMapa)
    }

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
                        monto = formulario.montoTotal,
                        estado = EstadoFactura.PENDIENTE,
                        ordenesIds = form.ordenesSeleccionadas.toList(),
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

    LaunchedEffect(estado) {
        if (estado is GenerarFacturaUiState.Generado) onBack()
    }

    val formulario = estado as? GenerarFacturaUiState.Formulario

    GenerarFacturaContent(
        formulario = formulario,
        onBack = onBack,
        onCliente = viewModel::onCliente,
        onTipo = viewModel::onTipo,
        onFechaDesde = viewModel::onFechaDesde,
        onFechaHasta = viewModel::onFechaHasta,
        onToggleOrden = viewModel::onToggleOrden,
        onCantidadArticulo = viewModel::onCantidadArticulo,
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
    onToggleOrden: (String) -> Unit,
    onCantidadArticulo: (String, Int) -> Unit,
    onGenerar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        topBar = {
            ElevaProTopAppBar(
                titulo = "Generar factura",
                onBack = onBack,
                acciones = {
                    Icon(
                        Icons.Outlined.Receipt,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                },
            )
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // ── Período ──
            SeccionLabel("Período de facturación")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                DatePickerField(
                    label = "Fecha desde *",
                    valor = form.fechaDesde,
                    onFecha = onFechaDesde,
                    modifier = Modifier.weight(1f),
                )
                DatePickerField(
                    label = "Fecha hasta",
                    valor = form.fechaHasta,
                    onFecha = onFechaHasta,
                    modifier = Modifier.weight(1f),
                )
            }

            // ── Datos ──
            SeccionLabel("Datos de la factura")
            ClienteDropdown(
                clientes = formulario.clientes,
                seleccionadoNombre = form.clienteNombre,
                onSeleccion = onCliente,
                modifier = Modifier.fillMaxWidth(),
            )
            TipoDropdown(
                seleccionado = form.tipo,
                onSeleccion = onTipo,
                modifier = Modifier.fillMaxWidth(),
            )

            // ── Órdenes pendientes ──
            if (form.clienteId.isNotBlank()) {
                SeccionLabel("Órdenes pendientes de facturar")
                if (formulario.ordenesPendientes.isEmpty()) {
                    Text(
                        "Sin órdenes pendientes para este cliente",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column {
                            formulario.ordenesPendientes.forEachIndexed { i, orden ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Checkbox(
                                        checked = orden.id in form.ordenesSeleccionadas,
                                        onCheckedChange = { onToggleOrden(orden.id) },
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(orden.numero, style = MaterialTheme.typography.bodyMedium)
                                        Text(
                                            "${orden.tipo} · ${orden.fecha}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                                if (i < formulario.ordenesPendientes.lastIndex) {
                                    HorizontalDivider(modifier = Modifier.padding(horizontal = 12.dp))
                                }
                            }
                        }
                    }
                }
            }

            // ── Artículos ──
            SeccionLabel("Artículos")
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column {
                    formulario.articulos.forEachIndexed { i, articulo ->
                        val cantidad = form.articulosConCantidad.getOrDefault(articulo.id, 0)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(articulo.nombre, style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    formatearMonto(articulo.precio),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                IconButton(
                                    onClick = { onCantidadArticulo(articulo.id, -1) },
                                    enabled = cantidad > 0,
                                    modifier = Modifier.size(32.dp),
                                ) {
                                    Icon(Icons.Outlined.Remove, contentDescription = "Quitar uno")
                                }
                                Text(
                                    text = "$cantidad",
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.padding(horizontal = 4.dp),
                                )
                                IconButton(
                                    onClick = { onCantidadArticulo(articulo.id, +1) },
                                    modifier = Modifier.size(32.dp),
                                ) {
                                    Icon(Icons.Outlined.Add, contentDescription = "Agregar uno")
                                }
                            }
                        }
                        if (i < formulario.articulos.lastIndex) {
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 12.dp))
                        }
                    }
                }
            }

            // ── Totales calculados ──
            if (formulario.montoNeto > 0) {
                SeccionLabel("Resumen")
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilaImporte("Neto", formatearMonto(formulario.montoNeto))
                        HorizontalDivider()
                        FilaImporte("IVA 21%", formatearMonto(formulario.montoIva))
                        HorizontalDivider()
                        FilaImporte(
                            "Total",
                            formatearMonto(formulario.montoTotal),
                            negrita = true,
                            colorValor = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }

            if (form.error != null) {
                Text(
                    text = form.error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            FilledPrimaryButton(
                text = if (form.generando) "Generando…" else "Generar factura",
                onClick = onGenerar,
                enabled = form.isFormValid && !form.generando,
                icon = Icons.Outlined.Receipt,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(8.dp))
        }
    }
}

// ─── Componentes locales ────────────────────────────────────────────────────

@Composable
private fun SeccionLabel(texto: String) {
    Text(
        text = texto.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

// ─── Previews ────────────────────────────────────────────────────────────────

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, name = "GenerarFactura – vacío")
@Composable
private fun GenerarFacturaVacioPreview() {
    com.grupo.elevapro.ui.theme.ElevaProTheme {
        GenerarFacturaContent(
            formulario = GenerarFacturaUiState.Formulario(
                form = GenerarFacturaFormState(),
                clientes = com.grupo.elevapro.data.repository.FakeMockData.clientes,
                ordenesPendientes = emptyList(),
                articulos = com.grupo.elevapro.data.repository.FakeMockData.articulos,
            ),
            onBack = {},
            onCliente = { _, _ -> },
            onTipo = {},
            onFechaDesde = {},
            onFechaHasta = {},
            onToggleOrden = {},
            onCantidadArticulo = { _, _ -> },
            onGenerar = {},
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, name = "GenerarFactura – con datos y totales")
@Composable
private fun GenerarFacturaConDatosPreview() {
    com.grupo.elevapro.ui.theme.ElevaProTheme {
        GenerarFacturaContent(
            formulario = GenerarFacturaUiState.Formulario(
                form = GenerarFacturaFormState(
                    clienteId = "11",
                    clienteNombre = "Cons. Prop. Valentín Gomez 2711",
                    tipo = "A",
                    fechaDesde = "01 may 2026",
                    fechaHasta = "31 may 2026",
                    articulosConCantidad = mapOf("a1" to 2, "a5" to 4),
                ),
                clientes = com.grupo.elevapro.data.repository.FakeMockData.clientes,
                ordenesPendientes = com.grupo.elevapro.data.repository.FakeMockData.ordenes.take(2),
                articulos = com.grupo.elevapro.data.repository.FakeMockData.articulos,
            ),
            onBack = {},
            onCliente = { _, _ -> },
            onTipo = {},
            onFechaDesde = {},
            onFechaHasta = {},
            onToggleOrden = {},
            onCantidadArticulo = { _, _ -> },
            onGenerar = {},
        )
    }
}

