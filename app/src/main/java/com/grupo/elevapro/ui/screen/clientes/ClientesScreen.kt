package com.grupo.elevapro.ui.screen.clientes

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Search
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

private val SearchBarBg = Color(0xFFD5D5D5)
private val TextSecondary = Color(0xFF3D3D3D)
private val CardTitle = Color(0xFF1A1C1E)
private val CardBorder = Color(0xFFBDBDBD)

sealed interface ClientesUiState {
    data object Loading : ClientesUiState
    data class Success(
        val clientes: List<Cliente>,
        val total: Int,
        val query: String,
    ) : ClientesUiState
    data class Error(val mensaje: String) : ClientesUiState
}

@HiltViewModel
class ClientesViewModel @Inject constructor(
    repository: ClienteRepository,
) : ViewModel() {

    private val _query = MutableStateFlow("")

    val estado: StateFlow<ClientesUiState> = combine(
        repository.observarClientes(),
        _query,
    ) { lista, query ->
        val filtrados = if (query.isBlank()) lista
        else lista.filter {
            it.nombre.contains(query, ignoreCase = true) ||
                it.direccion.contains(query, ignoreCase = true)
        }
        ClientesUiState.Success(
            clientes = filtrados,
            total = lista.size,
            query = query,
        ) as ClientesUiState
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ClientesUiState.Loading,
    )

    fun onQuery(q: String) { _query.value = q }
}

@Composable
fun ClientesScreen(
    onClienteClick: (String) -> Unit,
    onAgregarCliente: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ClientesViewModel = hiltViewModel(),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    ClientesContent(
        estado = estado,
        onQuery = viewModel::onQuery,
        onClienteClick = onClienteClick,
        onAgregarCliente = onAgregarCliente,
        modifier = modifier,
    )
}

@Composable
private fun ClientesContent(
    estado: ClientesUiState,
    onQuery: (String) -> Unit,
    onClienteClick: (String) -> Unit,
    onAgregarCliente: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val query = if (estado is ClientesUiState.Success) estado.query else ""

    Scaffold(
        topBar = {
            ElevaProTopAppBar(
                titulo = "Clientes",
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
                containerColor = MaterialTheme.colorScheme.primary,
            ) {
                Icon(Icons.Outlined.Add, contentDescription = "Agregar cliente", tint = Color.White)
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
                value = query,
                onValueChange = onQuery,
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
                    unfocusedContainerColor = SearchBarBg,
                    focusedContainerColor = SearchBarBg,
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
                    text = estado.mensaje,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(16.dp),
                )
                is ClientesUiState.Success -> {
                    Text(
                        text = "${estado.total} clientes",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp),
                    )
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
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, CardBorder),
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
                    color = CardTitle,
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
                        tint = TextSecondary,
                    )
                    Text(
                        text = cliente.direccion,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (cliente.telefono.isNotBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Phone,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = TextSecondary,
                        )
                        Text(
                            text = cliente.telefono,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            maxLines = 1,
                        )
                    }
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

private fun Cliente.iniciales(): String {
    val palabras = nombre.trim().split(" ").filter { it.isNotBlank() }
    return when {
        palabras.size >= 2 -> "${palabras[0].first().uppercaseChar()}${palabras[1].first().uppercaseChar()}"
        palabras.size == 1 -> palabras[0].take(2).uppercase()
        else -> "??"
    }
}
