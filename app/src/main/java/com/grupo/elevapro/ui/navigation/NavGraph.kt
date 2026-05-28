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
import com.grupo.elevapro.ui.screen.articulos.ArticulosScreen
import com.grupo.elevapro.ui.screen.auth.LoginScreen
import com.grupo.elevapro.ui.screen.auth.OnboardingScreen
import com.grupo.elevapro.ui.screen.clientes.AgregarClienteScreen
import com.grupo.elevapro.ui.screen.clientes.ClientesScreen
import com.grupo.elevapro.ui.screen.facturacion.FacturaDetalleScreen
import com.grupo.elevapro.ui.screen.facturacion.FacturacionScreen
import com.grupo.elevapro.ui.screen.facturacion.GenerarFacturaScreen
import com.grupo.elevapro.ui.screen.facturacion.NuevaFacturaScreen
import com.grupo.elevapro.ui.screen.ordenes.NuevaOrdenScreen
import com.grupo.elevapro.ui.screen.ordenes.OrdenDetalleScreen
import com.grupo.elevapro.ui.screen.ordenes.OrdenesScreen
import com.grupo.elevapro.ui.screen.perfil.AyudaSoporteScreen
import com.grupo.elevapro.ui.screen.perfil.ConfiguracionScreen
import com.grupo.elevapro.ui.screen.perfil.NotificacionesScreen
import com.grupo.elevapro.ui.screen.perfil.OpcionesScreen
import com.grupo.elevapro.ui.screen.perfil.PerfilScreen

@Composable
fun NavGraph(
    navController: NavHostController,
    startDestination: String,
    modifier: Modifier = Modifier,
    onOpenDrawer: () -> Unit = {},
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
                onOpenDrawer = onOpenDrawer,
            )
        }
        composable(
            route = Screen.OrdenDetalle.route,
            arguments = listOf(navArgument(Screen.OrdenDetalle.ARG_ID) { type = NavType.StringType }),
        ) {
            OrdenDetalleScreen(
                onBack   = { navController.popBackStack() },
                onFirmar = { id -> navController.navigate(Screen.Firma.build(id)) },
            )
        }
        composable(Screen.NuevaOrden.route) {
            NuevaOrdenScreen(onBack = { navController.popBackStack() })
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
                onClienteClick = { id -> navController.navigate(Screen.AgregarCliente.build(id)) },
                onAgregarCliente = { navController.navigate(Screen.AgregarCliente.route_base) },
                onOpenDrawer = onOpenDrawer,
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
                onOpenDrawer = onOpenDrawer,
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
        composable(Screen.Articulos.route) { ArticulosScreen() }

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
                onSupervisores   = { navController.navigate(Screen.Supervisores.route) },
                onPlantillas     = { navController.navigate(Screen.Plantillas.route) },
                onDatosEmpresa   = { navController.navigate(Screen.DatosEmpresa.route) },
                onUsuarios       = { navController.navigate(Screen.Usuarios.route) },
                onRolesPermisos  = { navController.navigate(Screen.RolesPermisos.route) },
                onNotificaciones = { navController.navigate(Screen.Notificaciones.route) },
                onConfiguracion  = { navController.navigate(Screen.Configuracion.route) },
                onAyudaSoporte   = { navController.navigate(Screen.AyudaSoporte.route) },
                onOpciones       = { navController.navigate(Screen.Opciones.route) },
                onLogout         = {
                    navController.navigate(Screen.Onboarding.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
            )
        }
        composable(Screen.Notificaciones.route) {
            NotificacionesScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.Configuracion.route) {
            ConfiguracionScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.AyudaSoporte.route) {
            AyudaSoporteScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.Opciones.route) {
            OpcionesScreen(
                onNavTo = { navController.navigate(it) },
                onBack = { navController.popBackStack() },
            )
        }
    }
}
