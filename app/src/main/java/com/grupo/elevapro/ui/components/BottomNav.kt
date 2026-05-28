package com.grupo.elevapro.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Inventory
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
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
            ItemNav(Screen.Facturacion.route, "Facturas", Icons.Outlined.ReceiptLong),
            ItemNav(Screen.Perfil.route, "Perfil", Icons.Outlined.AccountCircle),
        )
        Rol.OPERATIVO -> listOf(
            ItemNav(Screen.Ordenes.route, "Órdenes", Icons.Outlined.Description),
            ItemNav(Screen.Clientes.route, "Clientes", Icons.Outlined.People),
            ItemNav(Screen.Articulos.route, "Artículos", Icons.Outlined.Inventory),
            ItemNav(Screen.Perfil.route, "Perfil", Icons.Outlined.AccountCircle),
        )
    }
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
    NavigationBar(modifier = modifier) {
        items.forEach { item ->
            NavigationBarItem(
                selected = currentRoute == item.route,
                onClick = {
                    if (currentRoute != item.route) {
                        navController.navigate(item.route) {
                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                icon = { Icon(item.icon, contentDescription = item.label) },
                label = { Text(item.label) },
            )
        }
    }
}
