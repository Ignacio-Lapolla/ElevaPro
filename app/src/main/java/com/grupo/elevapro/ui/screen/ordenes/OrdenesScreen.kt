package com.grupo.elevapro.ui.screen.ordenes

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
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grupo.elevapro.data.model.domain.Orden
import com.grupo.elevapro.data.repository.OrdenesRepository
import com.grupo.elevapro.ui.components.ElevaProTopAppBar
import com.grupo.elevapro.ui.components.FilterChipBar
import com.grupo.elevapro.ui.components.StatusChip
import com.grupo.elevapro.ui.components.TipoEstado
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

    val estado: StateFlow<OrdenesUiState> = combine(
        repository.observarOrdenes(),
        filtro,
    ) { lista, filtroActual ->
        val filtradas = when (filtroActual) {
            FiltroOrden.TODAS -> lista
            FiltroOrden.FIRMADAS -> lista.filter { it.firmada }
            FiltroOrden.SIN_FIRMAR -> lista.filter { !it.firmada }
        }
        OrdenesUiState.Success(
            ordenes = filtradas,
            filtro = filtroActual,
            totalFirmadas = lista.count { it.firmada },
            totalSinFirmar = lista.count { !it.firmada },
        ) as OrdenesUiState
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = OrdenesUiState.Loading,
    )

    fun onFiltro(f: FiltroOrden) {
        filtro.value = f
    }
}

@Composable
fun OrdenesScreen(
    onOrdenClick: (String) -> Unit,
    onNuevaOrden: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: OrdenesViewModel = hiltViewModel(),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    OrdenesContent(
        estado = estado,
        onFiltro = viewModel::onFiltro,
        onOrdenClick = onOrdenClick,
        onNuevaOrden = onNuevaOrden,
        modifier = modifier,
    )
}

@Composable
private fun OrdenesContent(
    estado: OrdenesUiState,
    onFiltro: (FiltroOrden) -> Unit,
    onOrdenClick: (String) -> Unit,
    onNuevaOrden: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        topBar = { ElevaProTopAppBar("Órdenes") },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNuevaOrden,
                icon = { Icon(Icons.Outlined.Add, contentDescription = "Nueva orden") },
                text = { Text("Nueva orden") },
            )
        },
        modifier = modifier,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            Spacer(Modifier.height(8.dp))
            FilterChipBar(
                opciones = FiltroOrden.entries.map { it.label },
                seleccionada = when (estado) {
                    is OrdenesUiState.Success -> estado.filtro.label
                    else -> FiltroOrden.TODAS.label
                },
                onSeleccion = { label -> onFiltro(FiltroOrden.entries.first { it.label == label }) },
            )
            Spacer(Modifier.height(8.dp))
            when (estado) {
                OrdenesUiState.Loading -> Text("Cargando…", modifier = Modifier.padding(16.dp))
                is OrdenesUiState.Error -> Text(
                    text = estado.mensaje,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(16.dp),
                )
                is OrdenesUiState.Success -> LazyColumn(
                    contentPadding = PaddingValues(16.dp),
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
    ElevatedCard(onClick = onClick, modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.Build,
                contentDescription = "Orden de trabajo",
                tint = MaterialTheme.colorScheme.primary,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(orden.clienteNombre, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "${orden.numero} · ${orden.tipo} · ${orden.fecha}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            StatusChip(
                text = if (orden.firmada) "Firmada" else "Sin firmar",
                tipo = if (orden.firmada) TipoEstado.SUCCESS else TipoEstado.ERROR,
            )
        }
    }
}
