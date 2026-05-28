package com.grupo.elevapro.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.grupo.elevapro.ui.components.PlaceholderScreen
import com.grupo.elevapro.ui.screen.admin.DatosEmpresaScreen
import com.grupo.elevapro.ui.screen.admin.PlantillasScreen
import com.grupo.elevapro.ui.screen.admin.RolesPermisosScreen
import com.grupo.elevapro.ui.screen.admin.SupervisoresScreen
import com.grupo.elevapro.ui.screen.admin.UsuarioPermisosScreen
import com.grupo.elevapro.ui.screen.admin.UsuariosScreen
import com.grupo.elevapro.ui.screen.auth.LoginScreen
import com.grupo.elevapro.ui.screen.auth.OnboardingScreen
import com.grupo.elevapro.ui.screen.ordenes.OrdenesScreen
import com.grupo.elevapro.ui.screen.perfil.PerfilScreen

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
        composable(Screen.Clientes.route) { PlaceholderScreen(titulo = "Clientes") }
        composable(
            route = Screen.ClienteDetalle.route,
            arguments = listOf(navArgument(Screen.ClienteDetalle.ARG_ID) { type = NavType.StringType }),
        ) {
            PlaceholderScreen(titulo = "Detalle Cliente", onBack = { navController.popBackStack() })
        }
        composable(Screen.AgregarCliente.route) {
            PlaceholderScreen(titulo = "Nuevo Cliente", onBack = { navController.popBackStack() })
        }

        // Facturación
        composable(Screen.Facturacion.route) { PlaceholderScreen(titulo = "Facturación") }
        composable(
            route = Screen.FacturaDetalle.route,
            arguments = listOf(navArgument(Screen.FacturaDetalle.ARG_ID) { type = NavType.StringType }),
        ) {
            PlaceholderScreen(titulo = "Detalle Factura", onBack = { navController.popBackStack() })
        }
        composable(Screen.GenerarFactura.route) {
            PlaceholderScreen(titulo = "Generar Factura", onBack = { navController.popBackStack() })
        }
        composable(Screen.NuevaFactura.route) {
            PlaceholderScreen(titulo = "Nueva Factura", onBack = { navController.popBackStack() })
        }

        // Artículos
        composable(Screen.Articulos.route) { PlaceholderScreen(titulo = "Artículos") }

        // Admin
        composable(Screen.Usuarios.route) {
            UsuariosScreen(
                onEditarPermisos = { id -> navController.navigate(Screen.UsuarioPermisos.build(id)) },
                onBack = { navController.popBackStack() },
            )
        }
        composable(
            route = Screen.UsuarioPermisos.route,
            arguments = listOf(navArgument(Screen.UsuarioPermisos.ARG_ID) { type = NavType.StringType }),
        ) {
            UsuarioPermisosScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.RolesPermisos.route) {
            RolesPermisosScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.Supervisores.route) {
            SupervisoresScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.Plantillas.route) {
            PlantillasScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.DatosEmpresa.route) {
            DatosEmpresaScreen(onBack = { navController.popBackStack() })
        }

        // Perfil
        composable(Screen.Perfil.route) {
            PerfilScreen(
                onSupervisores  = { navController.navigate(Screen.Supervisores.route) },
                onPlantillas    = { navController.navigate(Screen.Plantillas.route) },
                onDatosEmpresa  = { navController.navigate(Screen.DatosEmpresa.route) },
                onUsuarios      = { navController.navigate(Screen.Usuarios.route) },
                onRolesPermisos = { navController.navigate(Screen.RolesPermisos.route) },
                onLogout        = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
            )
        }
        composable(Screen.Notificaciones.route) { PlaceholderScreen(titulo = "Notificaciones", onBack = { navController.popBackStack() }) }
        composable(Screen.Configuracion.route) { PlaceholderScreen(titulo = "Configuración", onBack = { navController.popBackStack() }) }
        composable(Screen.AyudaSoporte.route) { PlaceholderScreen(titulo = "Ayuda y soporte", onBack = { navController.popBackStack() }) }
        composable(Screen.Opciones.route) { PlaceholderScreen(titulo = "Opciones", onBack = { navController.popBackStack() }) }
    }
}
