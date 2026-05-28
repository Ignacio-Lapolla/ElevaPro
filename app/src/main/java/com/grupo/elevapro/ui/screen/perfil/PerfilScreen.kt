package com.grupo.elevapro.ui.screen.perfil

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ExitToApp
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.Assignment
import androidx.compose.material.icons.outlined.Business
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Help
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.SupervisorAccount
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.grupo.elevapro.data.model.domain.Rol
import com.grupo.elevapro.data.model.domain.Usuario
import com.grupo.elevapro.data.repository.AuthRepository
import com.grupo.elevapro.ui.components.ElevaProTopAppBar
import com.grupo.elevapro.ui.navigation.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

// ── UiState & ViewModel ──────────────────────────────────────────────────────

sealed interface PerfilUiState {
    data object Loading : PerfilUiState
    data class Success(val usuario: Usuario, val isAdmin: Boolean) : PerfilUiState
    data class Error(val mensaje: String) : PerfilUiState
}

@HiltViewModel
class PerfilViewModel @Inject constructor(
    private val authRepo: AuthRepository,
) : ViewModel() {

    val estado: StateFlow<PerfilUiState> = authRepo.usuarioActual
        .map { u ->
            if (u != null) PerfilUiState.Success(u, u.rol == Rol.ADMINISTRADOR)
            else PerfilUiState.Error("No se encontró la sesión")
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PerfilUiState.Loading)

    fun logout() = authRepo.logout()
}

// ── Screen ────────────────────────────────────────────────────────────────────

@Composable
fun PerfilScreen(
    onNavTo: (String) -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PerfilViewModel = hiltViewModel(),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    when (val s = estado) {
        is PerfilUiState.Loading -> Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) { CircularProgressIndicator() }
        is PerfilUiState.Error -> Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) { Text(s.mensaje, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.error) }
        is PerfilUiState.Success -> PerfilContent(
            estado = s,
            onNavTo = onNavTo,
            onLogout = { viewModel.logout(); onLogout() },
            modifier = modifier,
        )
    }
}

// ── Content ───────────────────────────────────────────────────────────────────

private data class MenuEntry(
    val icon: ImageVector,
    val label: String,
    val subtitle: String,
    val route: String,
)

@Composable
private fun PerfilContent(
    estado: PerfilUiState.Success,
    onNavTo: (String) -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val u = estado.usuario
    val isAdmin = estado.isAdmin

    val accentColor = if (isAdmin) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
    val containerColor = if (isAdmin) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.primaryContainer
    val onContainerColor = if (isAdmin) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onPrimaryContainer

    val adminEntries = listOf(
        MenuEntry(Icons.Outlined.Business,         "Datos de empresa",  "Información corporativa",  Screen.DatosEmpresa.route),
        MenuEntry(Icons.Outlined.Group,            "Usuarios",          "Gestión de accesos",        Screen.Usuarios.route),
        MenuEntry(Icons.Outlined.Security,         "Roles y permisos",  "Control de accesos",        Screen.RolesPermisos.route),
        MenuEntry(Icons.Outlined.SupervisorAccount,"Supervisores",      "Equipo técnico",            Screen.Supervisores.route),
        MenuEntry(Icons.Outlined.Assignment,       "Plantillas",        "Plantillas de servicio",    Screen.Plantillas.route),
    )
    val prefEntries = listOf(
        MenuEntry(Icons.Outlined.Notifications, "Notificaciones",  "Alertas y novedades",      Screen.Notificaciones.route),
        MenuEntry(Icons.Outlined.Settings,      "Configuración",   "Preferencias de la app",   Screen.Configuracion.route),
        MenuEntry(Icons.Outlined.Help,          "Ayuda y soporte", "Centro de ayuda",          Screen.AyudaSoporte.route),
    )

    Scaffold(
        topBar = { ElevaProTopAppBar("Perfil") },
        modifier = modifier,
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // ── Hero ──
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.extraLarge,
                    colors = CardDefaults.cardColors(containerColor = containerColor),
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            AvatarInicial(
                                name = u.nombre,
                                sizeDp = 64,
                                color = accentColor,
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = u.nombre,
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = onContainerColor,
                                )
                                Surface(
                                    color = accentColor,
                                    shape = RoundedCornerShape(50),
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Shield,
                                            contentDescription = "Rol",
                                            modifier = Modifier.size(12.dp),
                                            tint = Color.White,
                                        )
                                        Text(
                                            text = if (isAdmin) "Administrador" else "Operativo",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White,
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        InfoRow(Icons.Outlined.Email, "Email", u.email, accentColor, onContainerColor)
                        if (u.telefono != null) {
                            Spacer(Modifier.height(4.dp))
                            InfoRow(Icons.Outlined.Phone, "Teléfono", u.telefono, accentColor, onContainerColor)
                        }
                        Spacer(Modifier.height(4.dp))
                        InfoRow(Icons.Outlined.Business, "Empresa", "Empresa #${u.numeroEmpresa}", accentColor, onContainerColor)
                    }
                }
            }

            // ── Sección admin (solo ADMINISTRADOR) ──
            if (isAdmin) {
                item { SectionLabel("Administración") }
                item { MenuCard(entries = adminEntries, onNavTo = onNavTo) }
            }

            // ── Sección preferencias ──
            item { SectionLabel("Preferencias") }
            item { MenuCard(entries = prefEntries, onNavTo = onNavTo) }

            // ── Botón cerrar sesión ──
            item {
                Button(
                    onClick = onLogout,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ExitToApp,
                        contentDescription = "Cerrar sesión",
                        modifier = Modifier.padding(end = 8.dp),
                    )
                    Text("Cerrar sesión", style = MaterialTheme.typography.labelLarge)
                }
            }

            // ── Footer ──
            item {
                Text(
                    text = "ElevaPro · v0.1.0-demo",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.outline,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                )
            }
        }
    }
}

// ── Componentes internos ──────────────────────────────────────────────────────

@Composable
private fun AvatarInicial(name: String, sizeDp: Int, color: Color) {
    val initials = name.split(" ")
        .mapNotNull { it.firstOrNull()?.uppercaseChar() }
        .take(2)
        .joinToString("")
    Box(
        modifier = Modifier
            .size(sizeDp.dp)
            .background(color, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = initials,
            style = MaterialTheme.typography.headlineSmall,
            color = Color.White,
        )
    }
}

@Composable
private fun InfoRow(icon: ImageVector, descripcion: String, valor: String, iconColor: Color, textColor: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(icon, contentDescription = descripcion, modifier = Modifier.size(14.dp), tint = iconColor)
        Text(valor, style = MaterialTheme.typography.bodyMedium, color = textColor)
    }
}

@Composable
private fun SectionLabel(label: String) {
    Text(
        text = label.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 4.dp),
    )
}

@Composable
private fun MenuCard(entries: List<MenuEntry>, onNavTo: (String) -> Unit) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        entries.forEachIndexed { index, entry ->
            ListItem(
                headlineContent = { Text(entry.label, style = MaterialTheme.typography.titleSmall) },
                supportingContent = { Text(entry.subtitle, style = MaterialTheme.typography.bodyMedium) },
                leadingContent = {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(entry.icon, contentDescription = entry.label, tint = MaterialTheme.colorScheme.primary)
                    }
                },
                trailingContent = {
                    Icon(
                        imageVector = Icons.Filled.ChevronRight,
                        contentDescription = "Navegar a ${entry.label}",
                        tint = MaterialTheme.colorScheme.outline,
                    )
                },
                modifier = Modifier.clickable { onNavTo(entry.route) },
            )
            if (index < entries.size - 1) {
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            }
        }
    }
}
