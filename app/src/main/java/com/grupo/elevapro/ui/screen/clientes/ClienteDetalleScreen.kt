package com.grupo.elevapro.ui.screen.clientes

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material3.AssistChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.grupo.elevapro.data.model.domain.Cliente
import com.grupo.elevapro.data.model.domain.Orden
import com.grupo.elevapro.data.repository.ClienteRepository
import com.grupo.elevapro.data.repository.FakeMockData
import com.grupo.elevapro.data.repository.OrdenesRepository
import com.grupo.elevapro.ui.components.ElevaProTopAppBar
import com.grupo.elevapro.ui.navigation.Screen
import com.grupo.elevapro.ui.theme.Primary
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

sealed interface ClienteDetalleUiState {
    data object Loading : ClienteDetalleUiState
    data object NotFound : ClienteDetalleUiState
    data class Success(
        val cliente: Cliente,
        val ordenesRecientes: List<Orden>,
        val supervisorNombre: String?,
    ) : ClienteDetalleUiState
}

@HiltViewModel
class ClienteDetalleViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    clienteRepository: ClienteRepository,
    ordenesRepository: OrdenesRepository,
) : ViewModel() {

    private val clienteId: String = checkNotNull(savedStateHandle[Screen.ClienteDetalle.ARG_ID])

    val estado: StateFlow<ClienteDetalleUiState> = combine(
        clienteRepository.observarClientes(),
        ordenesRepository.observarOrdenes(),
    ) { clientes, ordenes ->
        val cliente = clientes.find { it.id == clienteId }
            ?: return@combine ClienteDetalleUiState.NotFound
        val ordenesRecientes = ordenes
            .filter { it.clienteId == clienteId }
            .sortedByDescending { it.fecha }
            .take(5)
        val supervisorNombre = cliente.supervisorId
            ?.let { sid -> FakeMockData.supervisores.find { it.id == sid }?.nombre }
        ClienteDetalleUiState.Success(
            cliente = cliente,
            ordenesRecientes = ordenesRecientes,
            supervisorNombre = supervisorNombre,
        ) as ClienteDetalleUiState
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ClienteDetalleUiState.Loading,
    )
}

@Composable
fun ClienteDetalleScreen(
    onBack: () -> Unit,
    onEditar: (String) -> Unit,
    onOrdenClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ClienteDetalleViewModel = hiltViewModel(),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    ClienteDetalleContent(
        estado = estado,
        onBack = onBack,
        onEditar = onEditar,
        onOrdenClick = onOrdenClick,
        modifier = modifier,
    )
}

@Composable
private fun ClienteDetalleContent(
    estado: ClienteDetalleUiState,
    onBack: () -> Unit,
    onEditar: (String) -> Unit,
    onOrdenClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val titulo = if (estado is ClienteDetalleUiState.Success) estado.cliente.nombre else "Detalle del cliente"

    Scaffold(
        topBar = {
            ElevaProTopAppBar(
                titulo = titulo,
                onBack = onBack,
                acciones = {
                    if (estado is ClienteDetalleUiState.Success) {
                        IconButton(onClick = { onEditar(estado.cliente.id) }) {
                            Icon(Icons.Outlined.Edit, contentDescription = "Editar cliente")
                        }
                    }
                },
            )
        },
        modifier = modifier,
    ) { padding ->
        when (estado) {
            ClienteDetalleUiState.Loading -> Text(
                "Cargando…",
                modifier = Modifier
                    .padding(padding)
                    .padding(16.dp),
            )
            ClienteDetalleUiState.NotFound -> Text(
                "Cliente no encontrado",
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier
                    .padding(padding)
                    .padding(16.dp),
            )
            is ClienteDetalleUiState.Success -> ClienteDetalleBody(
                estado = estado,
                onOrdenClick = onOrdenClick,
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
private fun ClienteDetalleBody(
    estado: ClienteDetalleUiState.Success,
    onOrdenClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val cliente = estado.cliente

    Column(modifier = modifier) {
        // Hero
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(Primary, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = cliente.inicialesDetalle(),
                        color = Color.White,
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = cliente.nombre,
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (cliente.cuit.isNotBlank()) {
                            Badge(label = cliente.cuit)
                        }
                        if (estado.supervisorNombre != null) {
                            Badge(label = estado.supervisorNombre)
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // Sección Contacto
        if (cliente.telefono.isNotBlank() || cliente.email.isNotBlank() || cliente.direccion.isNotBlank()) {
            SeccionTitulo("Contacto")
            if (cliente.telefono.isNotBlank()) {
                ListItem(
                    headlineContent = { Text(cliente.telefono) },
                    supportingContent = { Text("Teléfono") },
                    leadingContent = {
                        Icon(
                            Icons.Outlined.Phone,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            context.startActivity(
                                Intent(Intent.ACTION_DIAL, Uri.parse("tel:${cliente.telefono}"))
                            )
                        },
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            }
            if (cliente.email.isNotBlank()) {
                ListItem(
                    headlineContent = { Text(cliente.email) },
                    supportingContent = { Text("E-mail") },
                    leadingContent = {
                        Icon(
                            Icons.Outlined.Email,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            context.startActivity(
                                Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${cliente.email}"))
                            )
                        },
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            }
            if (cliente.direccion.isNotBlank()) {
                ListItem(
                    headlineContent = { Text(cliente.direccion) },
                    supportingContent = { Text("Dirección") },
                    leadingContent = {
                        Icon(
                            Icons.Outlined.LocationOn,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val encoded = Uri.encode(cliente.direccion)
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=$encoded"))
                            )
                        },
                )
            }
            Spacer(Modifier.height(8.dp))
        }

        // Sección Notas
        if (!cliente.notas.isNullOrBlank()) {
            SeccionTitulo("Notas")
            Text(
                text = cliente.notas,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            )
            Spacer(Modifier.height(8.dp))
        }

        // Sección Órdenes recientes
        if (estado.ordenesRecientes.isNotEmpty()) {
            SeccionTitulo("Órdenes recientes")
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                estado.ordenesRecientes.forEach { orden ->
                    AssistChip(
                        onClick = { onOrdenClick(orden.id) },
                        label = { Text("${orden.numero} · ${orden.tipo}") },
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun SeccionTitulo(titulo: String) {
    Text(
        text = titulo.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

@Composable
private fun Badge(label: String, modifier: Modifier = Modifier) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        shape = RoundedCornerShape(4.dp),
        modifier = modifier,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
        )
    }
}

// ─── Previews ───────────────────────────────────────────────────────────────

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, name = "ClienteDetalle – con datos")
@Composable
private fun ClienteDetalleSuccessPreview() {
    com.grupo.elevapro.ui.theme.ElevaProTheme {
        ClienteDetalleContent(
            estado = ClienteDetalleUiState.Success(
                cliente = com.grupo.elevapro.data.repository.FakeMockData.clientes.first(),
                ordenesRecientes = com.grupo.elevapro.data.repository.FakeMockData.ordenes.take(3),
                supervisorNombre = "Carlos Méndez",
            ),
            onBack = {},
            onEditar = {},
            onOrdenClick = {},
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, name = "ClienteDetalle – sin órdenes ni notas")
@Composable
private fun ClienteDetalleSinOrdenesPreview() {
    com.grupo.elevapro.ui.theme.ElevaProTheme {
        ClienteDetalleContent(
            estado = ClienteDetalleUiState.Success(
                cliente = com.grupo.elevapro.data.repository.FakeMockData.clientes[4],
                ordenesRecientes = emptyList(),
                supervisorNombre = null,
            ),
            onBack = {},
            onEditar = {},
            onOrdenClick = {},
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, name = "ClienteDetalle – loading")
@Composable
private fun ClienteDetalleLoadingPreview() {
    com.grupo.elevapro.ui.theme.ElevaProTheme {
        ClienteDetalleContent(
            estado = ClienteDetalleUiState.Loading,
            onBack = {},
            onEditar = {},
            onOrdenClick = {},
        )
    }
}

private fun Cliente.inicialesDetalle(): String {
    val palabras = nombre.trim().split(" ").filter { it.isNotBlank() }
    return when {
        palabras.size >= 2 -> "${palabras[0].first().uppercaseChar()}${palabras[1].first().uppercaseChar()}"
        palabras.size == 1 -> palabras[0].take(2).uppercase()
        else -> "??"
    }
}
