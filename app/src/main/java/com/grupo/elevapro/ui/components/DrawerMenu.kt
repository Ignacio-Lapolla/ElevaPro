package com.grupo.elevapro.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Article
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.automirrored.outlined.NoteAdd
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.Business
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.FileOpen
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.ManageAccounts
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Upload
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.grupo.elevapro.data.model.domain.Permiso
import com.grupo.elevapro.data.model.domain.Rol
import com.grupo.elevapro.data.model.domain.Usuario
import com.grupo.elevapro.ui.navigation.Screen

@Composable
fun ElevaProDrawerContent(
    usuario: Usuario?,
    permisos: Set<Permiso>,
    currentRoute: String?,
    navController: NavController,
    onLogout: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxHeight()) {
        DrawerHeader(usuario = usuario)

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        // Sección scrolleable — ocupa el espacio disponible y empuja el footer al fondo
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
        ) {
            DrawerSection(titulo = "PRINCIPAL") {
                if (Permiso.VER_ORDENES in permisos) {
                    DrawerItem(
                        icon = Icons.Outlined.Description,
                        label = "Órdenes de Trabajo",
                        isActive = currentRoute == Screen.Ordenes.route,
                        onClick = { navController.navigateSingleTop(Screen.Ordenes.route); onClose() },
                    )
                }
                if (Permiso.VER_CLIENTES in permisos) {
                    DrawerItem(
                        icon = Icons.Outlined.People,
                        label = "Clientes",
                        isActive = currentRoute == Screen.Clientes.route,
                        onClick = { navController.navigateSingleTop(Screen.Clientes.route); onClose() },
                    )
                }
                if (Permiso.VER_FACTURAS in permisos) {
                    DrawerItem(
                        icon = Icons.Outlined.Receipt,
                        label = "Ver Facturas",
                        isActive = currentRoute == Screen.Facturacion.route,
                        onClick = { navController.navigateSingleTop(Screen.Facturacion.route); onClose() },
                    )
                }
                DrawerItem(
                    icon = Icons.AutoMirrored.Outlined.Article,
                    label = "Plantillas de Trabajo",
                    isActive = currentRoute == Screen.Plantillas.route,
                    onClick = { navController.navigateSingleTop(Screen.Plantillas.route); onClose() },
                )
            }

            if (usuario?.rol == Rol.ADMINISTRADOR) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                DrawerSection(titulo = "ADMINISTRACIÓN") {
                    DrawerItem(
                        icon = Icons.AutoMirrored.Outlined.NoteAdd,
                        label = "Generar Factura",
                        isActive = currentRoute == Screen.GenerarFactura.route,
                        onClick = { navController.navigateSingleTop(Screen.GenerarFactura.route); onClose() },
                    )
                    DrawerItem(
                        icon = Icons.Outlined.Group,
                        label = "Usuarios",
                        isActive = currentRoute == Screen.Usuarios.route,
                        onClick = { navController.navigateSingleTop(Screen.Usuarios.route); onClose() },
                    )
                    DrawerItem(
                        icon = Icons.Outlined.Security,
                        label = "Roles y Permisos",
                        isActive = currentRoute == Screen.RolesPermisos.route,
                        onClick = { navController.navigateSingleTop(Screen.RolesPermisos.route); onClose() },
                    )
                    DrawerItem(
                        icon = Icons.Outlined.ManageAccounts,
                        label = "Supervisores",
                        isActive = currentRoute == Screen.Supervisores.route,
                        onClick = { navController.navigateSingleTop(Screen.Supervisores.route); onClose() },
                    )
                    DrawerItem(
                        icon = Icons.Outlined.Business,
                        label = "Datos de Empresa",
                        isActive = currentRoute == Screen.DatosEmpresa.route,
                        onClick = { navController.navigateSingleTop(Screen.DatosEmpresa.route); onClose() },
                    )
                    DrawerItem(
                        icon = Icons.Outlined.FileOpen,
                        label = "Artículos",
                        isActive = currentRoute == Screen.Articulos.route,
                        onClick = { navController.navigateSingleTop(Screen.Articulos.route); onClose() },
                    )
                }
            }
        }

        // Footer siempre visible al fondo del drawer
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        DrawerFooterItem(
            icon = Icons.Outlined.Upload,
            label = "Sin órdenes pendientes",
            onClick = {},
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        DrawerFooterItem(
            icon = Icons.AutoMirrored.Outlined.Logout,
            label = "Cerrar sesión",
            onClick = onLogout,
            contentColor = MaterialTheme.colorScheme.error,
        )
    }
}

@Composable
private fun DrawerHeader(usuario: Usuario?) {
    val isAdmin = usuario?.rol == Rol.ADMINISTRADOR
    val containerColor = if (isAdmin)
        MaterialTheme.colorScheme.secondaryContainer
    else
        MaterialTheme.colorScheme.primaryContainer
    val onContainerColor = if (isAdmin)
        MaterialTheme.colorScheme.onSecondaryContainer
    else
        MaterialTheme.colorScheme.onPrimaryContainer

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(containerColor)
            .padding(horizontal = 16.dp, vertical = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (usuario != null) {
            val iniciales = usuario.nombre
                .split(" ")
                .take(2)
                .mapNotNull { it.firstOrNull()?.uppercaseChar() }
                .joinToString("")

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(if (isAdmin) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary),
            ) {
                Text(
                    text = iniciales,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isAdmin) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.Bold,
                )
            }

            Column {
                Text(
                    text = usuario.nombre,
                    style = MaterialTheme.typography.titleSmall,
                    color = onContainerColor,
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(99.dp))
                        .background(if (isAdmin) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary)
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                ) {
                    Icon(
                        imageVector = if (isAdmin) Icons.Outlined.AdminPanelSettings else Icons.Outlined.AccountCircle,
                        contentDescription = null,
                        tint = if (isAdmin) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(12.dp),
                    )
                    Text(
                        text = if (isAdmin) "Administrador" else "Operativo",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isAdmin) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onPrimary,
                    )
                }
            }
        } else {
            Icon(
                imageVector = Icons.Outlined.Business,
                contentDescription = "ElevaPro",
                tint = onContainerColor,
                modifier = Modifier.size(48.dp),
            )
            Text(
                text = "ElevaPro",
                style = MaterialTheme.typography.titleMedium,
                color = onContainerColor,
            )
        }
    }
}

@Composable
private fun DrawerSection(
    titulo: String,
    content: @Composable () -> Unit,
) {
    Text(
        text = titulo,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 4.dp),
    )
    content()
}

@Composable
private fun DrawerItem(
    icon: ImageVector,
    label: String,
    isActive: Boolean,
    onClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp)
            .clip(RoundedCornerShape(99.dp))
            .background(
                if (isActive) MaterialTheme.colorScheme.secondaryContainer
                else MaterialTheme.colorScheme.surface,
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(22.dp),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
            color = if (isActive) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        if (isActive) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

@Composable
private fun DrawerFooterItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    contentColor: androidx.compose.ui.graphics.Color,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = contentColor,
            modifier = Modifier.size(20.dp),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = contentColor,
        )
    }
}

private fun NavController.navigateSingleTop(route: String) {
    navigate(route) { launchSingleTop = true }
}
