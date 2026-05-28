package com.grupo.elevapro.ui.screen.clientes

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.grupo.elevapro.data.model.domain.Cliente
import com.grupo.elevapro.data.repository.ClienteRepository
import com.grupo.elevapro.ui.components.ElevaProTopAppBar
import com.grupo.elevapro.ui.theme.Primary
import com.grupo.elevapro.ui.theme.SurfaceLight
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject


sealed interface ClientesUiState {
    data object Loading : ClientesUiState
    data class Success(val clientes: List<Cliente>, val busqueda: String) : ClientesUiState
    data class Error(val msg: String) : ClientesUiState
}

@HiltViewModel
class ClientesViewModel @Inject constructor(
    repository: ClienteRepository,
) : ViewModel() {

    private val _busqueda = MutableStateFlow("")

    val estado: StateFlow<ClientesUiState> = combine(
        repository.observarClientes(),
        _busqueda,
    ) { lista, busqueda ->
        val filtrados = if (busqueda.isBlank()) lista
        else lista.filter {
            it.nombre.contains(busqueda, ignoreCase = true) ||
                it.direccion.contains(busqueda, ignoreCase = true)
        }
        ClientesUiState.Success(
            clientes = filtrados,
            busqueda = busqueda,
        ) as ClientesUiState
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ClientesUiState.Loading,
    )

    fun onBusqueda(q: String) { _busqueda.value = q }
}

@Composable
fun ClientesScreen(
    onClienteClick: (String) -> Unit,
    onAgregarCliente: () -> Unit,
    modifier: Modifier = Modifier,
    onOpenDrawer: () -> Unit = {},
    viewModel: ClientesViewModel = hiltViewModel(),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    ClientesContent(
        estado = estado,
        onBusqueda = viewModel::onBusqueda,
        onClienteClick = onClienteClick,
        onAgregarCliente = onAgregarCliente,
        onOpenDrawer = onOpenDrawer,
        modifier = modifier,
    )
}

@Composable
private fun ClientesContent(
    estado: ClientesUiState,
    onBusqueda: (String) -> Unit,
    onClienteClick: (String) -> Unit,
    onAgregarCliente: () -> Unit,
    onOpenDrawer: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val busqueda = if (estado is ClientesUiState.Success) estado.busqueda else ""

    Scaffold(
        topBar = {
            ElevaProTopAppBar(
                titulo = "Clientes",
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Default.Menu, contentDescription = "Abrir menú")
                    }
                },
                acciones = {
                    IconButton(onClick = {}) {
                        Icon(Icons.Outlined.Search, contentDescription = "Buscar")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAgregarCliente,
                containerColor = MaterialTheme.colorScheme.tertiary,
            ) {
                Icon(Icons.Outlined.Add, contentDescription = "Agregar cliente", tint = MaterialTheme.colorScheme.onTertiary)
            }
        },
        modifier = modifier,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(SurfaceLight),
        ) {
            OutlinedTextField(
                value = busqueda,
                onValueChange = onBusqueda,
                placeholder = {
                    Text(
                        "Buscar por nombre o dirección…",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                leadingIcon = {
                    Icon(
                        Icons.Outlined.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                shape = RoundedCornerShape(28.dp),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    unfocusedBorderColor = Color.Transparent,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                ),
            )

            when (estado) {
                ClientesUiState.Loading -> Text(
                    "Cargando…",
                    modifier = Modifier.padding(16.dp),
                )
                is ClientesUiState.Error -> Text(
                    text = estado.msg,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(16.dp),
                )
                is ClientesUiState.Success -> {
                    Text(
                        text = "${estado.clientes.size} clientes",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp),
                    )
                    if (estado.clientes.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(96.dp)
                                    .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    Icons.Outlined.People,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.outline,
                                )
                            }
                            Spacer(Modifier.height(16.dp))
                            Text(
                                text = "No se encontraron clientes",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            if (estado.busqueda.isNotBlank()) {
                                Spacer(Modifier.height(16.dp))
                                Button(
                                    onClick = { onBusqueda("") },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    ),
                                ) {
                                    Text("Limpiar búsqueda")
                                }
                            }
                        }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(7.dp),
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            items(estado.clientes, key = { it.id }) { cliente ->
                                ClienteCard(
                                    cliente = cliente,
                                    onClick = { onClienteClick(cliente.id) },
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
private fun ClienteCard(
    cliente: Cliente,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(91.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(Primary, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = cliente.iniciales(),
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    text = cliente.nombre,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = cliente.direccion,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Phone,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = cliente.telefono,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
            }

            Icon(
                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = "Ver detalle",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// ─── Previews ───────────────────────────────────────────────────────────────

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, name = "Clientes – lista")
@Composable
private fun ClientesSuccessPreview() {
    com.grupo.elevapro.ui.theme.ElevaProTheme {
        ClientesContent(
            estado = ClientesUiState.Success(
                clientes = com.grupo.elevapro.data.repository.FakeMockData.clientes,
                busqueda = "",
            ),
            onBusqueda = {},
            onClienteClick = {},
            onAgregarCliente = {},
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, name = "Clientes – búsqueda activa")
@Composable
private fun ClientesBusquedaPreview() {
    com.grupo.elevapro.ui.theme.ElevaProTheme {
        ClientesContent(
            estado = ClientesUiState.Success(
                clientes = com.grupo.elevapro.data.repository.FakeMockData.clientes.take(3),
                busqueda = "Lafinur",
            ),
            onBusqueda = {},
            onClienteClick = {},
            onAgregarCliente = {},
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, name = "Clientes – loading")
@Composable
private fun ClientesLoadingPreview() {
    com.grupo.elevapro.ui.theme.ElevaProTheme {
        ClientesContent(
            estado = ClientesUiState.Loading,
            onBusqueda = {},
            onClienteClick = {},
            onAgregarCliente = {},
        )
    }
}

private fun Cliente.iniciales(): String {
    val palabras = nombre.trim().split(" ").filter { it.isNotBlank() }
    return when {
        palabras.size >= 2 -> "${palabras[0].first().uppercaseChar()}${palabras[1].first().uppercaseChar()}"
        palabras.size == 1 -> palabras[0].take(2).uppercase()
        else -> "??"
    }
}
