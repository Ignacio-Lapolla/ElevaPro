package com.grupo.elevapro.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.AttachMoney
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Inventory
import androidx.compose.material.icons.outlined.People
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.grupo.elevapro.data.model.domain.Rol
import com.grupo.elevapro.ui.navigation.Screen

private data class ItemNav(val route: String, val label: String, val icon: ImageVector)

@Composable
fun ElevaProBottomNav(
    navController: NavHostController,
    rol: Rol,
    modifier: Modifier = Modifier,
) {
    val items: List<ItemNav> = when (rol) {
        Rol.ADMINISTRADOR -> listOf(
            ItemNav(Screen.Ordenes.route, "Órdenes", Icons.Outlined.Description),
            ItemNav(Screen.Clientes.route, "Clientes", Icons.Outlined.People),
            ItemNav(Screen.Facturacion.route, "Facturas", Icons.Outlined.AttachMoney),
            ItemNav(Screen.Perfil.route, "Perfil", Icons.Outlined.AccountCircle),
        )
        Rol.OPERATIVO -> listOf(
            ItemNav(Screen.Ordenes.route, "Órdenes", Icons.Outlined.Description),
            ItemNav(Screen.Clientes.route, "Clientes", Icons.Outlined.People),
            ItemNav(Screen.Articulos.route, "Artículos", Icons.Outlined.Inventory),
            ItemNav(Screen.Perfil.route, "Perfil", Icons.Outlined.AccountCircle),
        )
    }
    val currentEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentEntry?.destination?.route

    Column(modifier = modifier) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        NavigationBar(
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = NavigationBarDefaults.Elevation,
            windowInsets = WindowInsets(0),
        ) {
            items.forEach { item ->
                val selected = currentRoute == item.route
                NavigationBarItem(
                    selected = selected,
                    onClick = {
                        if (!selected) {
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    },
                    icon = { Icon(item.icon, contentDescription = item.label) },
                    label = { Text(item.label, style = MaterialTheme.typography.labelMedium) },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                )
            }
        }
        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsBottomHeight(WindowInsets.navigationBars)
                .background(MaterialTheme.colorScheme.surface),
        )
    }
}
