package com.grupo.elevapro.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.grupo.elevapro.ui.components.PlaceholderScreen
import com.grupo.elevapro.ui.screen.auth.LoginScreen
import com.grupo.elevapro.ui.screen.auth.OnboardingScreen
import com.grupo.elevapro.ui.screen.clientes.AgregarClienteScreen
import com.grupo.elevapro.ui.screen.facturacion.FacturaDetalleScreen
import com.grupo.elevapro.ui.screen.facturacion.FacturacionScreen
import com.grupo.elevapro.ui.screen.facturacion.GenerarFacturaScreen
import com.grupo.elevapro.ui.screen.facturacion.NuevaFacturaScreen
import com.grupo.elevapro.ui.screen.clientes.ClienteDetalleScreen
import com.grupo.elevapro.ui.screen.clientes.ClientesScreen
import com.grupo.elevapro.ui.screen.ordenes.OrdenesScreen

@Composable
fun NavGraph(
    navController: NavHostController,
    startDestination: String,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
    ) {
        // Auth
        composable(Screen.Onboarding.route) {
            val irALogin: () -> Unit = {
                navController.navigate(Screen.Login.route) {
                    popUpTo(Screen.Onboarding.route) { inclusive = true }
                }
            }
            OnboardingScreen(
                onFin = irALogin,
                onIniciarSesion = irALogin,
            )
        }
        composable(Screen.Login.route) {
            LoginScreen(onLoginOk = {
                navController.navigate(Screen.Ordenes.route) {
                    popUpTo(Screen.Login.route) { inclusive = true }
                }
            })
        }

        // Órdenes (funcional)
        composable(Screen.Ordenes.route) {
            OrdenesScreen(
                onOrdenClick = { id -> navController.navigate(Screen.OrdenDetalle.build(id)) },
                onNuevaOrden = { navController.navigate(Screen.NuevaOrden.route) },
            )
        }
        composable(
            route = Screen.OrdenDetalle.route,
            arguments = listOf(navArgument(Screen.OrdenDetalle.ARG_ID) { type = NavType.StringType }),
        ) {
            PlaceholderScreen(titulo = "Detalle Orden", onBack = { navController.popBackStack() })
        }
        composable(Screen.NuevaOrden.route) {
            PlaceholderScreen(titulo = "Nueva Orden", onBack = { navController.popBackStack() })
        }
        composable(
            route = Screen.Firma.route,
            arguments = listOf(navArgument(Screen.Firma.ARG_ID) { type = NavType.StringType }),
        ) {
            PlaceholderScreen(titulo = "Firma de conformidad", onBack = { navController.popBackStack() })
        }

        // Clientes
        composable(Screen.Clientes.route) {
            ClientesScreen(
                onClienteClick = { id -> navController.navigate(Screen.ClienteDetalle.build(id)) },
                onAgregarCliente = { navController.navigate(Screen.AgregarCliente.route_base) },
            )
        }
        composable(
            route = Screen.ClienteDetalle.route,
            arguments = listOf(navArgument(Screen.ClienteDetalle.ARG_ID) { type = NavType.StringType }),
        ) {
            ClienteDetalleScreen(
                onBack = { navController.popBackStack() },
                onEditar = { id -> navController.navigate(Screen.AgregarCliente.build(id)) },
                onOrdenClick = { id -> navController.navigate(Screen.OrdenDetalle.build(id)) },
            )
        }
        composable(
            route = Screen.AgregarCliente.route,
            arguments = listOf(
                navArgument(Screen.AgregarCliente.ARG_ID) {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
            ),
        ) {
            AgregarClienteScreen(onBack = { navController.popBackStack() })
        }

        // Facturación
        composable(Screen.Facturacion.route) {
            FacturacionScreen(
                onFacturaClick = { id -> navController.navigate(Screen.FacturaDetalle.build(id)) },
                onGenerarFactura = { navController.navigate(Screen.GenerarFactura.route) },
            )
        }
        composable(
            route = Screen.FacturaDetalle.route,
            arguments = listOf(navArgument(Screen.FacturaDetalle.ARG_ID) { type = NavType.StringType }),
        ) {
            FacturaDetalleScreen(
                onBack = { navController.popBackStack() },
                onOrdenClick = { id -> navController.navigate(Screen.OrdenDetalle.build(id)) },
            )
        }
        composable(Screen.GenerarFactura.route) {
            GenerarFacturaScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.NuevaFactura.route) {
            NuevaFacturaScreen(onBack = { navController.popBackStack() })
        }

        // Artículos
        composable(Screen.Articulos.route) { PlaceholderScreen(titulo = "Artículos") }

        // Admin
        composable(Screen.Usuarios.route) { PlaceholderScreen(titulo = "Usuarios", onBack = { navController.popBackStack() }) }
        composable(
            route = Screen.UsuarioPermisos.route,
            arguments = listOf(navArgument(Screen.UsuarioPermisos.ARG_ID) { type = NavType.StringType }),
        ) {
            PlaceholderScreen(titulo = "Permisos de usuario", onBack = { navController.popBackStack() })
        }
        composable(Screen.RolesPermisos.route) { PlaceholderScreen(titulo = "Roles y permisos", onBack = { navController.popBackStack() }) }
        composable(Screen.Supervisores.route) { PlaceholderScreen(titulo = "Supervisores", onBack = { navController.popBackStack() }) }
        composable(Screen.Plantillas.route) { PlaceholderScreen(titulo = "Plantillas", onBack = { navController.popBackStack() }) }
        composable(Screen.DatosEmpresa.route) { PlaceholderScreen(titulo = "Datos de empresa", onBack = { navController.popBackStack() }) }

        // Perfil
        composable(Screen.Perfil.route) { PlaceholderScreen(titulo = "Perfil") }
        composable(Screen.Notificaciones.route) { PlaceholderScreen(titulo = "Notificaciones", onBack = { navController.popBackStack() }) }
        composable(Screen.Configuracion.route) { PlaceholderScreen(titulo = "Configuración", onBack = { navController.popBackStack() }) }
        composable(Screen.AyudaSoporte.route) { PlaceholderScreen(titulo = "Ayuda y soporte", onBack = { navController.popBackStack() }) }
        composable(Screen.Opciones.route) { PlaceholderScreen(titulo = "Opciones", onBack = { navController.popBackStack() }) }
    }
}
