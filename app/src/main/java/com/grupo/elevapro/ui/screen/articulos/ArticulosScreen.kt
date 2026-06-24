package com.grupo.elevapro.ui.screen.articulos

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.grupo.elevapro.data.model.domain.Articulo
import com.grupo.elevapro.data.repository.ArticulosRepository
import com.grupo.elevapro.ui.components.ElevaProTopAppBar
import com.grupo.elevapro.ui.components.FilledPrimaryButton
import com.grupo.elevapro.ui.components.FilterChipBar
import com.grupo.elevapro.ui.components.StatusChip
import com.grupo.elevapro.ui.components.TipoEstado
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale
import javax.inject.Inject

// ── Form State ────────────────────────────────────────────────────────────────

data class NuevoArticuloForm(
    val nombre: String = "",
    val codigo: String = "",
    val precioStr: String = "",
    val categoria: String = "",
    val stockStr: String = "",
) {
    val esValido: Boolean
        get() = nombre.isNotBlank() && codigo.isNotBlank() &&
                precioStr.toDoubleOrNull() != null && categoria.isNotBlank() &&
                stockStr.toIntOrNull() != null
}

// ── UiState ──────────────────────────────────────────────────────────────────

sealed interface ArticulosUiState {
    data object Loading : ArticulosUiState
    data class Success(
        val articulos: List<Articulo>,
        val totalArticulos: Int,
        val valorTotal: Double,
        val categorias: List<String>,
        val busqueda: String,
        val categoriaSeleccionada: String?,
    ) : ArticulosUiState
    data class Error(val mensaje: String) : ArticulosUiState
}

// ── ViewModel ─────────────────────────────────────────────────────────────────

@HiltViewModel
class ArticulosViewModel @Inject constructor(
    private val repo: ArticulosRepository,
) : ViewModel() {

    private val busqueda = MutableStateFlow("")
    private val categoria = MutableStateFlow<String?>(null)

    private val _form = MutableStateFlow(NuevoArticuloForm())
    val form: StateFlow<NuevoArticuloForm> = _form.asStateFlow()

    val estado: StateFlow<ArticulosUiState> = combine(
        repo.observarArticulos(),
        busqueda,
        categoria,
    ) { lista, q, cat ->
        val filtrados = lista.filter {
            (cat == null || it.categoria == cat) &&
                    (q.isBlank() || it.nombre.contains(q, ignoreCase = true) || it.codigo.contains(q, ignoreCase = true))
        }
        ArticulosUiState.Success(
            articulos = filtrados,
            totalArticulos = lista.size,
            valorTotal = lista.sumOf { it.precio },
            categorias = listOf("Todas") + lista.map { it.categoria }.distinct(),
            busqueda = q,
            categoriaSeleccionada = cat,
        ) as ArticulosUiState
    }.catch { e ->
        emit(ArticulosUiState.Error(e.message ?: "Error inesperado"))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ArticulosUiState.Loading)

    init {
        // El repositorio reactivo provee los artículos al suscribirse.
        // Para H2: aquí se llamaría a sincronizar() contra el backend.
    }

    fun onBusqueda(q: String) { busqueda.value = q }
    fun onCategoria(c: String?) { categoria.value = c }
    fun onFormChange(form: NuevoArticuloForm) { _form.value = form }
    fun onDismissForm() { _form.value = NuevoArticuloForm() }

    fun onAgregar() {
        val f = _form.value
        val precio = f.precioStr.toDoubleOrNull() ?: return
        val stock = f.stockStr.toIntOrNull() ?: return
        viewModelScope.launch {
            repo.agregar(
                Articulo(
                    id = "a-${System.currentTimeMillis()}",
                    codigo = f.codigo.trim(),
                    nombre = f.nombre.trim(),
                    precio = precio,
                    categoria = f.categoria.trim(),
                    stock = stock,
                )
            )
            _form.value = NuevoArticuloForm()
        }
    }
}

// ── Screen ────────────────────────────────────────────────────────────────────

@Composable
fun ArticulosScreen(
    onBack: (() -> Unit)? = null,
    onArticuloClick: ((Articulo) -> Unit)? = null,
    modifier: Modifier = Modifier,
    viewModel: ArticulosViewModel = hiltViewModel(),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val form by viewModel.form.collectAsStateWithLifecycle()
    ArticulosContent(
        estado = estado,
        form = form,
        onBack = onBack,
        onArticuloClick = onArticuloClick,
        onBusqueda = viewModel::onBusqueda,
        onCategoria = viewModel::onCategoria,
        onFormChange = viewModel::onFormChange,
        onDismissForm = viewModel::onDismissForm,
        onAgregar = viewModel::onAgregar,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ArticulosContent(
    estado: ArticulosUiState,
    form: NuevoArticuloForm,
    onBack: (() -> Unit)?,
    onArticuloClick: ((Articulo) -> Unit)?,
    onBusqueda: (String) -> Unit,
    onCategoria: (String?) -> Unit,
    onFormChange: (NuevoArticuloForm) -> Unit,
    onDismissForm: () -> Unit,
    onAgregar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var mostrarSheet by remember { mutableStateOf(false) }
    var articuloDetalle by remember { mutableStateOf<Articulo?>(null) }

    Scaffold(
        topBar = { ElevaProTopAppBar("Artículos", onBack = onBack) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { mostrarSheet = true },
                icon = { Icon(Icons.Outlined.Add, contentDescription = "Agregar artículo") },
                text = { Text("Agregar artículo") },
            )
        },
        modifier = modifier,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            when (estado) {
                ArticulosUiState.Loading -> Box(
                    Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) { CircularProgressIndicator() }

                is ArticulosUiState.Error -> Text(
                    text = estado.mensaje,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(16.dp),
                )

                is ArticulosUiState.Success -> {
                    TotalRegistradoHeader(
                        total = estado.valorTotal,
                        cantidad = estado.totalArticulos,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    )
                    ArticulosSearchBar(
                        busqueda = estado.busqueda,
                        onBusqueda = onBusqueda,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                    Spacer(Modifier.height(8.dp))
                    FilterChipBar(
                        opciones = estado.categorias,
                        seleccionada = estado.categoriaSeleccionada ?: "Todas",
                        onSeleccion = { label -> onCategoria(if (label == "Todas") null else label) },
                    )
                    Spacer(Modifier.height(4.dp))
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        items(estado.articulos, key = { it.id }) { articulo ->
                            ArticuloCard(
                                articulo = articulo,
                                onClick = {
                                    if (onArticuloClick != null) onArticuloClick(articulo)
                                    else articuloDetalle = articulo
                                },
                            )
                        }
                    }
                }
            }
        }
    }

    articuloDetalle?.let { articulo ->
        ArticuloDetalleSheet(
            articulo = articulo,
            onDismiss = { articuloDetalle = null },
        )
    }

    if (mostrarSheet) {
        AgregarArticuloSheet(
            form = form,
            onFormChange = onFormChange,
            onGuardar = {
                onAgregar()
                mostrarSheet = false
            },
            onDismiss = {
                onDismissForm()
                mostrarSheet = false
            },
        )
    }
}

// ── Search Bar ────────────────────────────────────────────────────────────────

@Composable
private fun ArticulosSearchBar(
    busqueda: String,
    onBusqueda: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = busqueda,
        onValueChange = onBusqueda,
        modifier = modifier.fillMaxWidth(),
        placeholder = { Text("Buscar por código o nombre…") },
        leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = "Buscar") },
        trailingIcon = {
            if (busqueda.isNotEmpty()) {
                IconButton(onClick = { onBusqueda("") }) {
                    Icon(Icons.Outlined.Close, contentDescription = "Limpiar búsqueda")
                }
            }
        },
        singleLine = true,
        shape = MaterialTheme.shapes.extraLarge,
    )
}

// ── Articulo Card ─────────────────────────────────────────────────────────────

private val arsFormatter = NumberFormat.getNumberInstance(Locale("es", "AR")).apply {
    maximumFractionDigits = 0
}

private fun formatARS(precio: Double): String = "$ ${arsFormatter.format(precio)}"

// ── Total Registrado Header ───────────────────────────────────────────────────

@Composable
private fun TotalRegistradoHeader(
    total: Double,
    cantidad: Int,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = "TOTAL REGISTRADO",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = formatARS(total),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = MaterialTheme.shapes.small,
            ) {
                Text(
                    text = "$cantidad artículo${if (cantidad != 1) "s" else ""}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }
        }
    }
}

// ── Articulo Detalle Sheet ────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ArticuloDetalleSheet(
    articulo: Articulo,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val (stockTipo, stockTexto) = when {
        articulo.stock == 0 -> TipoEstado.ERROR to "Sin stock"
        articulo.stock <= 5 -> TipoEstado.WARNING to "${articulo.stock} uds"
        else -> TipoEstado.SUCCESS to "${articulo.stock} uds"
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = "IMPORTE",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Text(
                        text = formatARS(articulo.precio),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }

            Spacer(Modifier.size(16.dp))

            DetalleRow(label = "Artículo", valor = articulo.nombre, icon = Icons.Outlined.Inventory2)
            if (articulo.descripcion.isNotBlank()) {
                HorizontalDivider()
                DetalleRow(label = "Descripción", valor = articulo.descripcion, icon = Icons.Outlined.Inventory2)
            }
            HorizontalDivider()
            DetalleRow(label = "Código", valor = articulo.codigo, icon = Icons.Outlined.Inventory2)
            HorizontalDivider()
            DetalleRow(label = "Categoría", valor = articulo.categoria, icon = Icons.Outlined.Inventory2)
            HorizontalDivider()

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(
                    Icons.Outlined.Inventory2,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text("Stock", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(articulo.stock.toString(), style = MaterialTheme.typography.bodyMedium)
                }
                StatusChip(text = stockTexto, tipo = stockTipo)
            }
        }
    }
}

@Composable
private fun DetalleRow(
    label: String,
    valor: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(valor, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

// ── Articulo Card ─────────────────────────────────────────────────────────────

@Composable
private fun ArticuloCard(articulo: Articulo, onClick: () -> Unit = {}, modifier: Modifier = Modifier) {
    val (stockTipo, stockText) = when {
        articulo.stock == 0 -> TipoEstado.ERROR to "Sin stock"
        articulo.stock <= 5 -> TipoEstado.WARNING to "${articulo.stock} uds"
        else -> TipoEstado.SUCCESS to "${articulo.stock} uds"
    }

    ElevatedCard(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.Inventory2,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp),
            )
            Text(
                text = articulo.nombre,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 2,
            )
            Text(
                text = articulo.codigo,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = formatARS(articulo.precio),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            StatusChip(text = stockText, tipo = stockTipo)
        }
    }
}

// ── Agregar Artículo BottomSheet ──────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AgregarArticuloSheet(
    form: NuevoArticuloForm,
    onFormChange: (NuevoArticuloForm) -> Unit,
    onGuardar: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Nuevo artículo", style = MaterialTheme.typography.titleLarge)
            OutlinedTextField(
                value = form.nombre,
                onValueChange = { onFormChange(form.copy(nombre = it)) },
                label = { Text("Nombre *") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = form.codigo,
                onValueChange = { onFormChange(form.copy(codigo = it)) },
                label = { Text("Código *") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = form.precioStr,
                onValueChange = { onFormChange(form.copy(precioStr = it)) },
                label = { Text("Precio (ARS) *") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = form.categoria,
                onValueChange = { onFormChange(form.copy(categoria = it)) },
                label = { Text("Categoría *") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = form.stockStr,
                onValueChange = { onFormChange(form.copy(stockStr = it)) },
                label = { Text("Stock inicial *") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(4.dp))
            FilledPrimaryButton(
                text = "Guardar",
                onClick = onGuardar,
                enabled = form.esValido,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
