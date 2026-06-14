package com.grupo.elevapro.ui.screen.ordenes

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
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.CalendarMonth
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
import com.grupo.elevapro.data.model.domain.Orden
import com.grupo.elevapro.data.repository.OrdenesRepository
import com.grupo.elevapro.ui.components.ElevaProTopAppBar
import com.grupo.elevapro.ui.components.FilterChipBar
import com.grupo.elevapro.ui.components.StatusChip
import com.grupo.elevapro.ui.components.TipoEstado
import com.grupo.elevapro.ui.theme.ErrorContainer
import com.grupo.elevapro.ui.theme.ErrorRed
import com.grupo.elevapro.ui.theme.Success
import com.grupo.elevapro.ui.theme.SuccessContainer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

enum class FiltroOrden(val label: String) { TODAS("Todas"), FIRMADAS("Firmadas"), SIN_FIRMAR("Sin Firmar") }

sealed interface OrdenesUiState {
    data object Loading : OrdenesUiState
    data class Success(
        val ordenes: List<Orden>,
        val filtro: FiltroOrden,
        val busqueda: String,
        val totalFirmadas: Int,
        val totalSinFirmar: Int,
    ) : OrdenesUiState
    data class Error(val mensaje: String) : OrdenesUiState
}

@HiltViewModel
class OrdenesViewModel @Inject constructor(
    repository: OrdenesRepository,
) : ViewModel() {

    private val filtro = MutableStateFlow(FiltroOrden.TODAS)
    private val busqueda = MutableStateFlow("")

    val estado: StateFlow<OrdenesUiState> = combine(
        repository.observarOrdenes(),
        filtro,
        busqueda,
    ) { lista, filtroActual, q ->
        val filtradas = lista
            .filter { orden ->
                val matchBusqueda = q.isBlank() ||
                    orden.clienteNombre.contains(q, ignoreCase = true) ||
                    orden.numero.contains(q, ignoreCase = true) ||
                    orden.tipo.contains(q, ignoreCase = true)
                val matchEstado = when (filtroActual) {
                    FiltroOrden.TODAS -> true
                    FiltroOrden.FIRMADAS -> orden.firmada
                    FiltroOrden.SIN_FIRMAR -> !orden.firmada
                }
                matchBusqueda && matchEstado
            }
        OrdenesUiState.Success(
            ordenes = filtradas,
            filtro = filtroActual,
            busqueda = q,
            totalFirmadas = lista.count { it.firmada },
            totalSinFirmar = lista.count { !it.firmada },
        ) as OrdenesUiState
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = OrdenesUiState.Loading,
    )

    fun onFiltro(f: FiltroOrden) { filtro.value = f }
    fun onBusqueda(q: String) { busqueda.value = q }
}

@Composable
fun OrdenesScreen(
    onOrdenClick: (String) -> Unit,
    onNuevaOrden: () -> Unit,
    modifier: Modifier = Modifier,
    onOpenDrawer: () -> Unit = {},
    viewModel: OrdenesViewModel = hiltViewModel(),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    OrdenesContent(
        estado = estado,
        onFiltro = viewModel::onFiltro,
        onBusqueda = viewModel::onBusqueda,
        onOrdenClick = onOrdenClick,
        onNuevaOrden = onNuevaOrden,
        onOpenDrawer = onOpenDrawer,
        modifier = modifier,
    )
}

@Composable
private fun OrdenesContent(
    estado: OrdenesUiState,
    onFiltro: (FiltroOrden) -> Unit,
    onBusqueda: (String) -> Unit,
    onOrdenClick: (String) -> Unit,
    onNuevaOrden: () -> Unit,
    onOpenDrawer: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val busqueda = if (estado is OrdenesUiState.Success) estado.busqueda else ""

    Scaffold(
        topBar = {
            ElevaProTopAppBar(
                titulo = "Órdenes",
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Filled.Menu, contentDescription = "Abrir menú")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNuevaOrden,
                containerColor = MaterialTheme.colorScheme.tertiary,
                contentColor = MaterialTheme.colorScheme.onTertiary,
            ) {
                Icon(Icons.Outlined.Add, contentDescription = "Nueva orden")
            }
        },
        modifier = modifier,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.surface),
        ) {
            // Search bar
            OutlinedTextField(
                value = busqueda,
                onValueChange = onBusqueda,
                placeholder = {
                    Text(
                        "Buscar por cliente, número, tipo…",
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

            // Chips filtro estado
            FilterChipBar(
                opciones = FiltroOrden.entries.map { it.label },
                seleccionada = when (estado) {
                    is OrdenesUiState.Success -> estado.filtro.label
                    else -> FiltroOrden.TODAS.label
                },
                onSeleccion = { label -> onFiltro(FiltroOrden.entries.first { it.label == label }) },
            )

            // Summary row
            if (estado is OrdenesUiState.Success) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    StatusChip(text = "${estado.totalFirmadas} firmadas", tipo = TipoEstado.SUCCESS)
                    StatusChip(text = "${estado.totalSinFirmar} sin firma", tipo = TipoEstado.ERROR)
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = "${estado.ordenes.size} resultado${if (estado.ordenes.size != 1) "s" else ""}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            when (estado) {
                OrdenesUiState.Loading -> Text("Cargando…", modifier = Modifier.padding(16.dp))
                is OrdenesUiState.Error -> Text(
                    text = estado.mensaje,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(16.dp),
                )
                is OrdenesUiState.Success -> LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(estado.ordenes, key = { it.id }) { orden ->
                        OrdenCard(orden = orden, onClick = { onOrdenClick(orden.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun OrdenCard(orden: Orden, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val iconBg = if (orden.firmada) SuccessContainer else ErrorContainer
    val iconTint = if (orden.firmada) Success else ErrorRed

    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .background(iconBg, RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Outlined.Build,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = iconTint,
                )
            }

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = orden.clienteNombre,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = orden.tipo,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(
                        Icons.Outlined.CalendarMonth,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = MaterialTheme.colorScheme.outline,
                    )
                    Text(
                        text = orden.fecha,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Icon(
                Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline,
            )
        }
    }
}
