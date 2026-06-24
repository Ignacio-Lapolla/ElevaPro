package com.grupo.elevapro.ui.screen.facturacion

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AttachMoney
import androidx.compose.material.icons.outlined.Notes
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.grupo.elevapro.data.model.domain.Cliente
import com.grupo.elevapro.data.model.domain.EstadoFactura
import com.grupo.elevapro.data.model.domain.Factura
import com.grupo.elevapro.data.repository.ClienteRepository
import com.grupo.elevapro.data.repository.FacturacionRepository
import com.grupo.elevapro.ui.components.ElevaProTextField
import com.grupo.elevapro.ui.components.ElevaProTopAppBar
import com.grupo.elevapro.ui.components.FilledPrimaryButton
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

// ─── Form & UiState ──────────────────────────────────────────────────────────

data class NuevaFacturaFormState(
    val clienteId: String = "",
    val clienteNombre: String = "",
    val tipo: String = "",
    val fecha: String = "",
    val descripcion: String = "",
    val montoStr: String = "",
    val guardando: Boolean = false,
    val error: String? = null,
) {
    val monto: Double get() = montoStr.replace(",", ".").toDoubleOrNull() ?: 0.0
    val isFormValid: Boolean
        get() = clienteId.isNotBlank() && tipo.isNotBlank() && fecha.isNotBlank() && monto > 0
}

sealed interface NuevaFacturaUiState {
    data object Loading : NuevaFacturaUiState
    data class Formulario(
        val form: NuevaFacturaFormState,
        val clientes: List<Cliente>,
    ) : NuevaFacturaUiState
}

// ─── ViewModel ───────────────────────────────────────────────────────────────

@HiltViewModel
class NuevaFacturaViewModel @Inject constructor(
    private val clienteRepository: ClienteRepository,
    private val facturacionRepository: FacturacionRepository,
) : ViewModel() {

    private val _form = MutableStateFlow(NuevaFacturaFormState())

    val estado: StateFlow<NuevaFacturaUiState> = combine(
        clienteRepository.observarClientes(),
        _form,
    ) { clientes, form ->
        NuevaFacturaUiState.Formulario(form = form, clientes = clientes)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = NuevaFacturaUiState.Loading,
    )

    fun onCliente(id: String, nombre: String) = update { copy(clienteId = id, clienteNombre = nombre) }
    fun onTipo(t: String) = update { copy(tipo = t) }
    fun onFecha(f: String) = update { copy(fecha = f) }
    fun onDescripcion(d: String) = update { copy(descripcion = d) }
    fun onMonto(m: String) = update { copy(montoStr = m.filter { it.isDigit() || it == ',' || it == '.' }) }

    fun guardar() {
        val formulario = (estado.value as? NuevaFacturaUiState.Formulario) ?: return
        if (!formulario.form.isFormValid) return
        viewModelScope.launch {
            update { copy(guardando = true, error = null) }
            try {
                val form = formulario.form
                facturacionRepository.agregar(
                    Factura(
                        id = UUID.randomUUID().toString(),
                        numero = "0001-${(10000000..99999999).random()}",
                        tipo = form.tipo,
                        clienteId = form.clienteId,
                        clienteNombre = form.clienteNombre,
                        fecha = form.fecha,
                        monto = form.monto,
                        estado = EstadoFactura.PENDIENTE,
                    )
                )
                _guardado.send(Unit)
            } catch (e: Exception) {
                update { copy(guardando = false, error = "No se pudo guardar la factura") }
            }
        }
    }

    private val _guardado = Channel<Unit>(Channel.CONFLATED)
    val guardado = _guardado.receiveAsFlow()

    private fun update(block: NuevaFacturaFormState.() -> NuevaFacturaFormState) =
        _form.update { it.block() }
}

// ─── Screen ──────────────────────────────────────────────────────────────────

@Composable
fun NuevaFacturaScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: NuevaFacturaViewModel = hiltViewModel(),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.guardado.collect { onBack() } }

    val formulario = estado as? NuevaFacturaUiState.Formulario

    NuevaFacturaContent(
        formulario = formulario,
        onBack = onBack,
        onCliente = viewModel::onCliente,
        onTipo = viewModel::onTipo,
        onFecha = viewModel::onFecha,
        onDescripcion = viewModel::onDescripcion,
        onMonto = viewModel::onMonto,
        onGuardar = viewModel::guardar,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NuevaFacturaContent(
    formulario: NuevaFacturaUiState.Formulario?,
    onBack: () -> Unit,
    onCliente: (String, String) -> Unit,
    onTipo: (String) -> Unit,
    onFecha: (String) -> Unit,
    onDescripcion: (String) -> Unit,
    onMonto: (String) -> Unit,
    onGuardar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        topBar = { ElevaProTopAppBar(titulo = "Nueva factura", onBack = onBack) },
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

            // Fecha — DatePickerField de GenerarFactura
            DatePickerField(
                label = "Fecha de emisión *",
                valor = form.fecha,
                onFecha = onFecha,
                modifier = Modifier.fillMaxWidth(),
            )

            // Monto manual
            ElevaProTextField(
                value = form.montoStr,
                onValueChange = onMonto,
                label = "Monto total *",
                leadingIcon = Icons.Outlined.AttachMoney,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            )

            // Descripción del servicio
            ElevaProTextField(
                value = form.descripcion,
                onValueChange = onDescripcion,
                label = "Descripción del servicio",
                leadingIcon = Icons.Outlined.Notes,
                singleLine = false,
                modifier = Modifier.fillMaxWidth(),
            )

            if (form.error != null) {
                Text(
                    text = form.error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            FilledPrimaryButton(
                text = if (form.guardando) "Guardando…" else "Guardar factura",
                onClick = onGuardar,
                enabled = form.isFormValid && !form.guardando,
                icon = Icons.Outlined.Receipt,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

// ─── Previews ────────────────────────────────────────────────────────────────

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, name = "NuevaFactura – vacío")
@Composable
private fun NuevaFacturaVacioPreview() {
    com.grupo.elevapro.ui.theme.ElevaProTheme {
        NuevaFacturaContent(
            formulario = NuevaFacturaUiState.Formulario(
                form = NuevaFacturaFormState(),
                clientes = com.grupo.elevapro.data.repository.FakeMockData.clientes,
            ),
            onBack = {},
            onCliente = { _, _ -> },
            onTipo = {},
            onFecha = {},
            onDescripcion = {},
            onMonto = {},
            onGuardar = {},
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, name = "NuevaFactura – con datos")
@Composable
private fun NuevaFacturaConDatosPreview() {
    com.grupo.elevapro.ui.theme.ElevaProTheme {
        NuevaFacturaContent(
            formulario = NuevaFacturaUiState.Formulario(
                form = NuevaFacturaFormState(
                    clienteId = "1",
                    clienteNombre = "Cons. Prop. Lafinur 3060",
                    tipo = "A",
                    fecha = "01 may 2026",
                    descripcion = "Mantenimiento preventivo mensual",
                    montoStr = "45000",
                ),
                clientes = com.grupo.elevapro.data.repository.FakeMockData.clientes,
            ),
            onBack = {},
            onCliente = { _, _ -> },
            onTipo = {},
            onFecha = {},
            onDescripcion = {},
            onMonto = {},
            onGuardar = {},
        )
    }
}
