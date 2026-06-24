package com.grupo.elevapro.ui.screen.ordenes

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.outlined.WifiOff
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.grupo.elevapro.data.model.domain.Orden
import com.grupo.elevapro.data.model.domain.Supervisor
import com.grupo.elevapro.data.repository.ClienteRepository
import com.grupo.elevapro.data.repository.OrdenesRepository
import com.grupo.elevapro.data.repository.SupervisoresRepository
import com.grupo.elevapro.ui.components.ElevaProTopAppBar
import com.grupo.elevapro.ui.components.FilterChipBar
import com.grupo.elevapro.ui.components.FilledPrimaryButton
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
import com.grupo.elevapro.data.util.NetworkMonitor
import javax.inject.Inject

enum class FiltroOrden(val label: String) { TODAS("Todas"), FIRMADAS("Firmadas"), SIN_FIRMAR("Sin Firmar") }

data class FiltroAvanzado(
    val fechaInicio: String = "",
    val fechaFin: String = "",
    val clienteId: String = "",
    val supervisorId: String = "",
) {
    val activos: Int get() = listOf(fechaInicio, fechaFin, clienteId, supervisorId).count { it.isNotBlank() }
    val vacio: Boolean get() = activos == 0
}

sealed interface OrdenesUiState {
    data object Loading : OrdenesUiState
    data class Success(
        val ordenes: List<Orden>,
        val filtro: FiltroOrden,
        val busqueda: String,
        val filtroAvanzado: FiltroAvanzado,
        val totalFirmadas: Int,
        val totalSinFirmar: Int,
        val clientes: List<Cliente>,
        val supervisores: List<Supervisor>,
        val isOnline: Boolean = true,
    ) : OrdenesUiState
    data object SinConexion : OrdenesUiState
    data class Error(val mensaje: String) : OrdenesUiState
}

@HiltViewModel
class OrdenesViewModel @Inject constructor(
    private val ordenesRepo: OrdenesRepository,
    private val clientesRepo: ClienteRepository,
    private val supervisoresRepo: SupervisoresRepository,
    private val networkMonitor: NetworkMonitor,
) : ViewModel() {

    private val filtro = MutableStateFlow(FiltroOrden.TODAS)
    private val busqueda = MutableStateFlow("")
    private val filtroAvanzado = MutableStateFlow(FiltroAvanzado())

    private val supervisores: StateFlow<List<Supervisor>> = supervisoresRepo.observarSupervisores()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val isOnline = networkMonitor.isOnline
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val estado: StateFlow<OrdenesUiState> = combine(
        ordenesRepo.observarOrdenes(),
        clientesRepo.observarClientes(),
        filtro,
        busqueda,
        combine(filtroAvanzado, isOnline) { fa, online -> fa to online },
    ) { lista, clientes, filtroActual, q, (fa, online) ->
        val filtradas = lista.filter { orden ->
            val matchBusqueda = q.isBlank() ||
                orden.clienteNombre.contains(q, ignoreCase = true) ||
                orden.numero.contains(q, ignoreCase = true) ||
                orden.tipo.contains(q, ignoreCase = true)
            val matchEstado = when (filtroActual) {
                FiltroOrden.TODAS -> true
                FiltroOrden.FIRMADAS -> orden.firmada
                FiltroOrden.SIN_FIRMAR -> !orden.firmada
            }
            val matchFechaInicio = fa.fechaInicio.isBlank() || orden.fecha >= fa.fechaInicio
            val matchFechaFin = fa.fechaFin.isBlank() || orden.fecha <= fa.fechaFin
            val matchCliente = fa.clienteId.isBlank() || orden.clienteId == fa.clienteId
            matchBusqueda && matchEstado && matchFechaInicio && matchFechaFin && matchCliente
        }
        OrdenesUiState.Success(
            ordenes = filtradas,
            filtro = filtroActual,
            busqueda = q,
            filtroAvanzado = fa,
            totalFirmadas = lista.count { it.firmada },
            totalSinFirmar = lista.count { !it.firmada },
            clientes = clientes,
            supervisores = supervisores.value,
            isOnline = online,
        ) as OrdenesUiState
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = OrdenesUiState.Loading,
    )

    fun onFiltro(f: FiltroOrden) { filtro.value = f }
    fun onBusqueda(q: String) { busqueda.value = q }
    fun onFiltroAvanzado(fa: FiltroAvanzado) { filtroAvanzado.value = fa }
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
        onFiltroAvanzado = viewModel::onFiltroAvanzado,
        onOrdenClick = onOrdenClick,
        onNuevaOrden = onNuevaOrden,
        onOpenDrawer = onOpenDrawer,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OrdenesContent(
    estado: OrdenesUiState,
    onFiltro: (FiltroOrden) -> Unit,
    onBusqueda: (String) -> Unit,
    onFiltroAvanzado: (FiltroAvanzado) -> Unit,
    onOrdenClick: (String) -> Unit,
    onNuevaOrden: () -> Unit,
    onOpenDrawer: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val busqueda = (estado as? OrdenesUiState.Success)?.busqueda ?: ""
    val fa = (estado as? OrdenesUiState.Success)?.filtroAvanzado ?: FiltroAvanzado()
    var mostrarFiltros by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            ElevaProTopAppBar(
                titulo = "Órdenes",
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Filled.Menu, contentDescription = "Abrir menú")
                    }
                },
                acciones = {
                    BadgedBox(
                        badge = {
                            if (fa.activos > 0) Badge { Text(fa.activos.toString()) }
                        }
                    ) {
                        IconButton(onClick = { mostrarFiltros = true }) {
                            Icon(
                                Icons.Outlined.Tune,
                                contentDescription = "Filtros avanzados",
                                tint = if (fa.activos > 0) MaterialTheme.colorScheme.primary
                                       else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
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
                    Icon(Icons.Outlined.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                },
                shape = RoundedCornerShape(28.dp),
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    unfocusedBorderColor = Color.Transparent,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                ),
            )

            FilterChipBar(
                opciones = FiltroOrden.entries.map { it.label },
                seleccionada = (estado as? OrdenesUiState.Success)?.filtro?.label ?: FiltroOrden.TODAS.label,
                onSeleccion = { label -> onFiltro(FiltroOrden.entries.first { it.label == label }) },
            )

            if (fa.activos > 0 && estado is OrdenesUiState.Success) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (fa.fechaInicio.isNotBlank()) {
                        InputChip(
                            selected = true,
                            onClick = { onFiltroAvanzado(fa.copy(fechaInicio = "")) },
                            label = { Text("Desde ${fa.fechaInicio}") },
                            trailingIcon = { Icon(Icons.Outlined.Close, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        )
                    }
                    if (fa.fechaFin.isNotBlank()) {
                        InputChip(
                            selected = true,
                            onClick = { onFiltroAvanzado(fa.copy(fechaFin = "")) },
                            label = { Text("Hasta ${fa.fechaFin}") },
                            trailingIcon = { Icon(Icons.Outlined.Close, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        )
                    }
                    if (fa.clienteId.isNotBlank()) {
                        val nombre = estado.clientes.find { it.id == fa.clienteId }?.nombre ?: fa.clienteId
                        InputChip(
                            selected = true,
                            onClick = { onFiltroAvanzado(fa.copy(clienteId = "")) },
                            label = { Text(nombre, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            trailingIcon = { Icon(Icons.Outlined.Close, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        )
                    }
                }
            }

            if (estado is OrdenesUiState.Success) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
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

            if (estado is OrdenesUiState.Success && !estado.isOnline) {
                BannerSinConexion()
            }

            when (estado) {
                OrdenesUiState.Loading -> Text("Cargando…", modifier = Modifier.padding(16.dp))
                OrdenesUiState.SinConexion -> BannerSinConexion()
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

    if (mostrarFiltros && estado is OrdenesUiState.Success) {
        FiltroAvanzadoSheet(
            filtroActual = fa,
            clientes = estado.clientes,
            supervisores = estado.supervisores,
            onAplicar = { onFiltroAvanzado(it); mostrarFiltros = false },
            onDismiss = { mostrarFiltros = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FiltroAvanzadoSheet(
    filtroActual: FiltroAvanzado,
    clientes: List<Cliente>,
    supervisores: List<Supervisor>,
    onAplicar: (FiltroAvanzado) -> Unit,
    onDismiss: () -> Unit,
) {
    var temp by remember { mutableStateOf(filtroActual) }
    var expandirClientes by remember { mutableStateOf(false) }
    var expandirSupervisores by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Filtros", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                if (!temp.vacio) {
                    IconButton(onClick = { temp = FiltroAvanzado() }) {
                        Icon(Icons.Outlined.Close, contentDescription = "Limpiar filtros")
                    }
                }
            }

            HorizontalDivider()

            // Fecha inicio
            OutlinedTextField(
                value = temp.fechaInicio,
                onValueChange = { temp = temp.copy(fechaInicio = it) },
                label = { Text("Fecha desde (AAAA-MM-DD)") },
                singleLine = true,
                leadingIcon = { Icon(Icons.Outlined.CalendarMonth, contentDescription = null) },
                trailingIcon = {
                    if (temp.fechaInicio.isNotBlank()) {
                        IconButton(onClick = { temp = temp.copy(fechaInicio = "") }) {
                            Icon(Icons.Outlined.Close, contentDescription = "Limpiar", modifier = Modifier.size(18.dp))
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )

            // Fecha fin
            OutlinedTextField(
                value = temp.fechaFin,
                onValueChange = { temp = temp.copy(fechaFin = it) },
                label = { Text("Fecha hasta (AAAA-MM-DD)") },
                singleLine = true,
                leadingIcon = { Icon(Icons.Outlined.CalendarMonth, contentDescription = null) },
                trailingIcon = {
                    if (temp.fechaFin.isNotBlank()) {
                        IconButton(onClick = { temp = temp.copy(fechaFin = "") }) {
                            Icon(Icons.Outlined.Close, contentDescription = "Limpiar", modifier = Modifier.size(18.dp))
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )

            HorizontalDivider()

            // Selector cliente
            val clienteNombre = clientes.find { it.id == temp.clienteId }?.nombre ?: "Todos los clientes"
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { expandirClientes = !expandirClientes }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Cliente", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(clienteNombre, style = MaterialTheme.typography.bodyMedium)
                    }
                    if (temp.clienteId.isNotBlank()) {
                        IconButton(onClick = { temp = temp.copy(clienteId = "") }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Outlined.Close, contentDescription = "Limpiar", modifier = Modifier.size(16.dp))
                        }
                    }
                }
                DropdownMenu(expanded = expandirClientes, onDismissRequest = { expandirClientes = false }) {
                    DropdownMenuItem(text = { Text("Todos") }, onClick = { temp = temp.copy(clienteId = ""); expandirClientes = false })
                    clientes.forEach { c ->
                        DropdownMenuItem(
                            text = { Text(c.nombre, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            onClick = { temp = temp.copy(clienteId = c.id); expandirClientes = false },
                        )
                    }
                }
            }

            // Selector supervisor
            val supervisorNombre = supervisores.find { it.id == temp.supervisorId }?.nombre ?: "Todos los supervisores"
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { expandirSupervisores = !expandirSupervisores }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Supervisor", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(supervisorNombre, style = MaterialTheme.typography.bodyMedium)
                    }
                    if (temp.supervisorId.isNotBlank()) {
                        IconButton(onClick = { temp = temp.copy(supervisorId = "") }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Outlined.Close, contentDescription = "Limpiar", modifier = Modifier.size(16.dp))
                        }
                    }
                }
                DropdownMenu(expanded = expandirSupervisores, onDismissRequest = { expandirSupervisores = false }) {
                    DropdownMenuItem(text = { Text("Todos") }, onClick = { temp = temp.copy(supervisorId = ""); expandirSupervisores = false })
                    supervisores.forEach { s ->
                        DropdownMenuItem(
                            text = { Text(s.nombre) },
                            onClick = { temp = temp.copy(supervisorId = s.id); expandirSupervisores = false },
                        )
                    }
                }
            }

            FilledPrimaryButton(
                text = "Aplicar filtros",
                onClick = { onAplicar(temp) },
                modifier = Modifier.fillMaxWidth(),
            )
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

@Composable
private fun BannerSinConexion(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.errorContainer)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            Icons.Outlined.WifiOff,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.size(16.dp),
        )
        Text(
            text = "Sin conexión · Mostrando datos guardados",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onErrorContainer,
        )
    }
}
