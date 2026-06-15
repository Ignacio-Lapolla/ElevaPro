package com.grupo.elevapro.ui.screen.admin

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.grupo.elevapro.data.model.domain.Rol
import com.grupo.elevapro.data.model.domain.Usuario
import com.grupo.elevapro.data.model.domain.permisosDefault
import com.grupo.elevapro.data.repository.PermisosRepository
import com.grupo.elevapro.data.repository.UsuariosRepository
import com.grupo.elevapro.ui.components.ElevaProTextField
import com.grupo.elevapro.ui.components.ElevaProTopAppBar
import com.grupo.elevapro.ui.components.FilledPrimaryButton
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

// ── UiState ──────────────────────────────────────────────────────────────────

sealed interface RolesPermisosUiState {
    data object Loading : RolesPermisosUiState
    data class Success(
        val usuarios: List<Usuario>,
    ) : RolesPermisosUiState {
        val totalAdmins: Int get() = usuarios.count { it.rol == Rol.ADMINISTRADOR }
        val totalEmpleados: Int get() = usuarios.count { it.rol == Rol.OPERATIVO }
    }
    data class Error(val mensaje: String) : RolesPermisosUiState
}

// ── ViewModel ────────────────────────────────────────────────────────────────

@HiltViewModel
class RolesPermisosViewModel @Inject constructor(
    private val usuariosRepository: UsuariosRepository,
    private val permisosRepository: PermisosRepository,
) : ViewModel() {

    val estado: StateFlow<RolesPermisosUiState> = usuariosRepository.observarUsuarios()
        .map { lista -> RolesPermisosUiState.Success(lista) as RolesPermisosUiState }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RolesPermisosUiState.Loading)

    fun crearRol(nombre: String, baseRol: Rol) {
        // Para H1: crea un usuario "plantilla" con el rol base.
        // En MVP2 esto se persistirá como rol personalizado en backend.
        viewModelScope.launch {
            val nuevoUsuario = Usuario(
                id = "rol_${System.currentTimeMillis()}",
                nombre = nombre,
                email = "${nombre.lowercase().replace(" ", ".")}@empresa.com",
                rol = baseRol,
                numeroEmpresa = "001",
                telefono = null,
                fotoUrl = null,
            )
            usuariosRepository.crear(nuevoUsuario)
            permisosRepository.actualizar(nuevoUsuario.id, baseRol.permisosDefault)
        }
    }
}

// ── Screen ───────────────────────────────────────────────────────────────────

@Composable
fun RolesPermisosScreen(
    onBack: () -> Unit,
    onModificarPermisos: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RolesPermisosViewModel = hiltViewModel(),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    RolesPermisosContent(
        estado = estado,
        onBack = onBack,
        onModificarPermisos = onModificarPermisos,
        onCrearRol = viewModel::crearRol,
        modifier = modifier,
    )
}

// ── Content ──────────────────────────────────────────────────────────────────

@Composable
private fun RolesPermisosContent(
    estado: RolesPermisosUiState,
    onBack: () -> Unit,
    onModificarPermisos: (String) -> Unit,
    onCrearRol: (String, Rol) -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHost = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var mostrarDialogNuevoRol by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            ElevaProTopAppBar(
                titulo = "Roles y Permisos",
                onBack = onBack,
                acciones = {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = CircleShape,
                        modifier = Modifier.padding(end = 8.dp),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Icon(
                                Icons.Outlined.Security,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(12.dp),
                            )
                            Text(
                                "Solo Admin",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                },
            )
        },
        bottomBar = {
            Column {
                HorizontalDivider()
                FilledPrimaryButton(
                    text = "Crear nuevo rol",
                    onClick = { mostrarDialogNuevoRol = true },
                    icon = Icons.Outlined.Add,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHost) },
        modifier = modifier,
    ) { padding ->
        when (val s = estado) {
            RolesPermisosUiState.Loading -> Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }

            is RolesPermisosUiState.Error -> Text(
                text = s.mensaje,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(padding).padding(16.dp),
            )

            is RolesPermisosUiState.Success -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp),
            ) {
                // ── Banner informativo ────────────────────────────────────
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.secondaryContainer)
                            .padding(12.dp),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Icon(
                            Icons.Outlined.Security,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(16.dp).padding(top = 2.dp),
                        )
                        Text(
                            "Gestioná los niveles de acceso de cada miembro de tu equipo. Los cambios aplican de forma inmediata.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                }

                // ── Resumen de roles ──────────────────────────────────────
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        ResumenRolChip(
                            count = s.totalAdmins,
                            label = "Administrador${if (s.totalAdmins != 1) "es" else ""}",
                            isAdmin = true,
                            modifier = Modifier.weight(1f),
                        )
                        ResumenRolChip(
                            count = s.totalEmpleados,
                            label = "Empleado${if (s.totalEmpleados != 1) "s" else ""}",
                            isAdmin = false,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                }

                // ── Header lista ──────────────────────────────────────────
                item {
                    Text(
                        text = "USUARIOS DEL DOMINIO · ${s.usuarios.size} TOTAL",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 10.dp),
                    )
                }

                // ── Usuarios ──────────────────────────────────────────────
                items(s.usuarios, key = { it.id }) { usuario ->
                    UsuarioRolCard(
                        usuario = usuario,
                        onModificarPermisos = { onModificarPermisos(usuario.id) },
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                }
            }
        }
    }

    if (mostrarDialogNuevoRol) {
        NuevoRolDialog(
            onDismiss = { mostrarDialogNuevoRol = false },
            onCrear = { nombre, rol ->
                onCrearRol(nombre, rol)
                mostrarDialogNuevoRol = false
                scope.launch { snackbarHost.showSnackbar("Rol \"$nombre\" creado") }
            },
        )
    }
}

// ── ResumenRolChip ────────────────────────────────────────────────────────────

@Composable
private fun ResumenRolChip(
    count: Int,
    label: String,
    isAdmin: Boolean,
    modifier: Modifier = Modifier,
) {
    val bg = if (isAdmin) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.primaryContainer
    val fg = if (isAdmin) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
    val iconColor = if (isAdmin) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(fg),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                if (isAdmin) Icons.Outlined.Security else Icons.Outlined.People,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.surface,
                modifier = Modifier.size(16.dp),
            )
        }
        Column {
            Text(
                text = "$count",
                style = MaterialTheme.typography.titleSmall,
                color = iconColor,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = iconColor,
            )
        }
    }
}

// ── UsuarioRolCard ────────────────────────────────────────────────────────────

@Composable
private fun UsuarioRolCard(
    usuario: Usuario,
    onModificarPermisos: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isAdmin = usuario.rol == Rol.ADMINISTRADOR
    val chipBg = if (isAdmin) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.primaryContainer
    val chipFg = if (isAdmin) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
    val avatarBg = if (isAdmin) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.primaryContainer
    val avatarFg = if (isAdmin) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary

    ElevatedCard(modifier = modifier.fillMaxWidth()) {
        // Info del usuario
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(avatarBg),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = usuario.nombre.first().uppercaseChar().toString(),
                    style = MaterialTheme.typography.titleMedium,
                    color = avatarFg,
                    fontWeight = FontWeight.Bold,
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = usuario.nombre,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = usuario.email,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            // Chip de rol
            Surface(
                color = chipBg,
                shape = CircleShape,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(
                        if (isAdmin) Icons.Outlined.Security else Icons.Outlined.People,
                        contentDescription = null,
                        tint = chipFg,
                        modifier = Modifier.size(11.dp),
                    )
                    Text(
                        text = if (isAdmin) "Administrador" else "Empleado",
                        style = MaterialTheme.typography.labelSmall,
                        color = chipFg,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }

        // Botón Modificar permisos
        HorizontalDivider()
        TextButton(
            onClick = onModificarPermisos,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.textButtonColors(
                contentColor = MaterialTheme.colorScheme.primary,
            ),
        ) {
            Icon(Icons.Outlined.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(6.dp))
            Text("Modificar permisos", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.weight(1f))
            Icon(Icons.Outlined.ChevronRight, contentDescription = null, modifier = Modifier.size(14.dp))
        }
    }
}

// ── NuevoRolDialog ────────────────────────────────────────────────────────────

@Composable
private fun NuevoRolDialog(
    onDismiss: () -> Unit,
    onCrear: (String, Rol) -> Unit,
    modifier: Modifier = Modifier,
) {
    var nombre by remember { mutableStateOf("") }
    var baseRol by remember { mutableStateOf(Rol.OPERATIVO) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = modifier,
        ) {
            Column {
                // Header
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(20.dp),
                ) {
                    Text(
                        "Crear nuevo rol",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Text(
                        "Configurá los permisos base del nuevo rol",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }

                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    // Nombre del rol
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "NOMBRE DEL ROL",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        ElevaProTextField(
                            value = nombre,
                            onValueChange = { nombre = it },
                            label = "Ej: Supervisor de campo",
                            leadingIcon = Icons.Outlined.Security,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }

                    // Base rol
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "PERMISOS BASE (PERSONALIZABLE LUEGO)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            RolBaseCard(
                                label = "Administrador",
                                descripcion = "Acceso total al sistema",
                                isAdmin = true,
                                selected = baseRol == Rol.ADMINISTRADOR,
                                onClick = { baseRol = Rol.ADMINISTRADOR },
                                modifier = Modifier.weight(1f),
                            )
                            RolBaseCard(
                                label = "Empleado",
                                descripcion = "Acceso operativo estándar",
                                isAdmin = false,
                                selected = baseRol == Rol.OPERATIVO,
                                onClick = { baseRol = Rol.OPERATIVO },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }

                // Footer
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(bottom = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(24.dp),
                    ) {
                        Text("Cancelar", style = MaterialTheme.typography.labelLarge)
                    }
                    Button(
                        onClick = { onCrear(nombre.trim(), baseRol) },
                        enabled = nombre.isNotBlank(),
                        modifier = Modifier.weight(2f).height(48.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                        ),
                    ) {
                        Text("Crear rol", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }
}

@Composable
private fun RolBaseCard(
    label: String,
    descripcion: String,
    isAdmin: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val borderColor = if (selected) {
        if (isAdmin) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
    } else MaterialTheme.colorScheme.outlineVariant
    val bg = if (selected) {
        if (isAdmin) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.primaryContainer
    } else MaterialTheme.colorScheme.surface
    val iconBg = if (isAdmin) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
    val labelColor = if (selected) {
        if (isAdmin) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
    } else MaterialTheme.colorScheme.onSurface

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = bg,
        modifier = modifier,
        border = androidx.compose.foundation.BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = borderColor,
        ),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(iconBg),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    if (isAdmin) Icons.Outlined.Security else Icons.Outlined.People,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.size(18.dp),
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                color = labelColor,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = descripcion,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
    }
}
