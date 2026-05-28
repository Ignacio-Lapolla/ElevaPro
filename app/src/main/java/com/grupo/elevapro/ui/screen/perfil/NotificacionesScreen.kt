package com.grupo.elevapro.ui.screen.perfil

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DoneAll
import androidx.compose.material.icons.outlined.Error
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.grupo.elevapro.data.model.domain.GrupoNotificacion
import com.grupo.elevapro.data.model.domain.Notificacion
import com.grupo.elevapro.data.model.domain.TipoNotificacion
import com.grupo.elevapro.data.repository.FakeMockData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

// ── UiState & ViewModel ──────────────────────────────────────────────────────

data class NotificacionesUiState(
    val notis: List<Notificacion> = emptyList(),
) {
    val sinLeer: Int get() = notis.count { !it.leida }
}

@HiltViewModel
class NotificacionesViewModel @Inject constructor() : ViewModel() {

    private val _estado = MutableStateFlow(NotificacionesUiState(FakeMockData.notificaciones))
    val estado: StateFlow<NotificacionesUiState> = _estado.asStateFlow()

    fun marcarLeida(id: String) {
        _estado.update { s -> s.copy(notis = s.notis.map { if (it.id == id) it.copy(leida = true) else it }) }
    }

    fun marcarTodasLeidas() {
        _estado.update { s -> s.copy(notis = s.notis.map { it.copy(leida = true) }) }
    }

    fun eliminar(id: String) {
        _estado.update { s -> s.copy(notis = s.notis.filter { it.id != id }) }
    }

    fun limpiarTodas() {
        _estado.update { it.copy(notis = emptyList()) }
    }
}

// ── Screen ────────────────────────────────────────────────────────────────────

@Composable
fun NotificacionesScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: NotificacionesViewModel = hiltViewModel(),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    NotificacionesContent(
        estado = estado,
        onBack = onBack,
        onMarcarLeida = viewModel::marcarLeida,
        onMarcarTodasLeidas = viewModel::marcarTodasLeidas,
        onEliminar = viewModel::eliminar,
        onLimpiarTodas = viewModel::limpiarTodas,
        modifier = modifier,
    )
}

// ── Content ───────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NotificacionesContent(
    estado: NotificacionesUiState,
    onBack: () -> Unit,
    onMarcarLeida: (String) -> Unit,
    onMarcarTodasLeidas: () -> Unit,
    onEliminar: (String) -> Unit,
    onLimpiarTodas: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text("Notificaciones", style = MaterialTheme.typography.titleLarge)
                        if (estado.sinLeer > 0) {
                            Badge(containerColor = MaterialTheme.colorScheme.primary) {
                                Text(
                                    "${estado.sinLeer} nueva${if (estado.sinLeer != 1) "s" else ""}",
                                    style = MaterialTheme.typography.labelSmall,
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    if (estado.sinLeer > 0) {
                        IconButton(onClick = onMarcarTodasLeidas) {
                            Icon(Icons.Outlined.DoneAll, contentDescription = "Marcar todas como leídas")
                        }
                    }
                    if (estado.notis.isNotEmpty()) {
                        IconButton(onClick = onLimpiarTodas) {
                            Icon(Icons.Outlined.Delete, contentDescription = "Limpiar todas las notificaciones")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
        modifier = modifier,
    ) { padding ->
        if (estado.notis.isEmpty()) {
            EstadoVacio(modifier = Modifier.padding(padding))
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                GrupoNotificacion.entries.forEach { grupo ->
                    val items = estado.notis.filter { it.grupo == grupo }
                    if (items.isNotEmpty()) {
                        item(key = grupo.name) {
                            Text(
                                text = grupo.label.uppercase(),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
                            )
                        }
                        items(items, key = { it.id }) { noti ->
                            NotiItem(
                                noti = noti,
                                onLeer = { onMarcarLeida(noti.id) },
                                onEliminar = { onEliminar(noti.id) },
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── Notificacion item ─────────────────────────────────────────────────────────

@Composable
private fun NotiItem(
    noti: Notificacion,
    onLeer: () -> Unit,
    onEliminar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val (iconoVector, containerBg, iconTint) = iconConfigParaTipo(noti.tipo)

    ListItem(
        headlineContent = {
            Text(
                text = noti.titulo,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = if (!noti.leida) FontWeight.Bold else FontWeight.Normal,
            )
        },
        supportingContent = {
            Column {
                Text(noti.cuerpo, style = MaterialTheme.typography.bodyMedium)
                Text(
                    text = noti.hora,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        },
        leadingContent = {
            BadgedBox(badge = {
                if (!noti.leida) {
                    Badge(containerColor = MaterialTheme.colorScheme.primary) {
                        Text("Nueva", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(containerBg, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = iconoVector,
                        contentDescription = noti.tipo.etiqueta(),
                        tint = iconTint,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        },
        trailingContent = {
            IconButton(onClick = onEliminar) {
                Icon(Icons.Outlined.Delete, contentDescription = "Eliminar notificación", tint = MaterialTheme.colorScheme.outline)
            }
        },
        colors = if (!noti.leida) {
            ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f))
        } else {
            ListItemDefaults.colors()
        },
        modifier = modifier.clickable { onLeer() },
    )
}

// ── Estado vacío ──────────────────────────────────────────────────────────────

@Composable
private fun EstadoVacio(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, androidx.compose.foundation.shape.CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.NotificationsNone,
                contentDescription = "Sin notificaciones",
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.outline,
            )
        }
        Text(
            text = "Sin notificaciones",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 16.dp),
        )
        Text(
            text = "Cuando haya actividad, las novedades aparecerán aquí.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.padding(top = 8.dp, start = 32.dp, end = 32.dp),
            textAlign = TextAlign.Center,
        )
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

private data class IconConfig(val icon: ImageVector, val bg: Color, val tint: Color)

@Composable
private fun iconConfigParaTipo(tipo: TipoNotificacion): IconConfig {
    val successBg  = MaterialTheme.colorScheme.tertiaryContainer
    val successTint = MaterialTheme.colorScheme.tertiary
    val errorBg    = MaterialTheme.colorScheme.errorContainer
    val errorTint  = MaterialTheme.colorScheme.error
    val primaryBg  = MaterialTheme.colorScheme.primaryContainer
    val primaryTint = MaterialTheme.colorScheme.primary
    val secondaryBg  = MaterialTheme.colorScheme.secondaryContainer
    val secondaryTint = MaterialTheme.colorScheme.secondary

    return when (tipo) {
        TipoNotificacion.ORDEN_FIRMADA     -> IconConfig(Icons.Outlined.CheckCircle, successBg,   successTint)
        TipoNotificacion.ORDEN_PENDIENTE   -> IconConfig(Icons.Outlined.Schedule,    secondaryBg, secondaryTint)
        TipoNotificacion.FACTURA_APROBADA  -> IconConfig(Icons.Outlined.CheckCircle, successBg,   successTint)
        TipoNotificacion.FACTURA_RECHAZADA -> IconConfig(Icons.Outlined.Error,       errorBg,     errorTint)
        TipoNotificacion.CLIENTE_NUEVO     -> IconConfig(Icons.Outlined.People,      primaryBg,   primaryTint)
        TipoNotificacion.ALERTA            -> IconConfig(Icons.Outlined.Warning,     errorBg,     errorTint)
        TipoNotificacion.SUPERVISOR        -> IconConfig(Icons.Outlined.Person,      secondaryBg, secondaryTint)
        TipoNotificacion.SISTEMA           -> IconConfig(Icons.Outlined.Info,        primaryBg,   primaryTint)
    }
}

private fun TipoNotificacion.etiqueta(): String = when (this) {
    TipoNotificacion.ORDEN_FIRMADA     -> "Orden firmada"
    TipoNotificacion.ORDEN_PENDIENTE   -> "Orden pendiente"
    TipoNotificacion.FACTURA_APROBADA  -> "Factura aprobada"
    TipoNotificacion.FACTURA_RECHAZADA -> "Factura rechazada"
    TipoNotificacion.CLIENTE_NUEVO     -> "Nuevo cliente"
    TipoNotificacion.ALERTA            -> "Alerta"
    TipoNotificacion.SUPERVISOR        -> "Supervisor"
    TipoNotificacion.SISTEMA           -> "Sistema"
}
