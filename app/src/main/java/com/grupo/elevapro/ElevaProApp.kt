package com.grupo.elevapro

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.grupo.elevapro.data.model.domain.Permiso
import com.grupo.elevapro.data.model.domain.Rol
import com.grupo.elevapro.data.model.domain.Usuario
import com.grupo.elevapro.data.repository.AuthRepository
import com.grupo.elevapro.data.repository.PermisosRepository
import com.grupo.elevapro.ui.components.ElevaProBottomNav
import com.grupo.elevapro.ui.components.ElevaProDrawerContent
import com.grupo.elevapro.ui.navigation.NavGraph
import com.grupo.elevapro.ui.navigation.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val permisosRepository: PermisosRepository,
) : ViewModel() {
    val usuarioActual: StateFlow<Usuario?> = authRepository.usuarioActual

    @OptIn(ExperimentalCoroutinesApi::class)
    val permisosActuales: StateFlow<Set<Permiso>> = authRepository.usuarioActual
        .flatMapLatest { u ->
            if (u != null) permisosRepository.observarPermisos(u.id) else flowOf(emptySet())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    fun logout() = authRepository.logout()
}

@Composable
fun ElevaProApp() {
    val navController = rememberNavController()
    val authVm: AppViewModel = hiltViewModel()
    val usuario by authVm.usuarioActual.collectAsStateWithLifecycle()
    val permisos by authVm.permisosActuales.collectAsStateWithLifecycle()
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route

    val rutasConBottomNav = when (usuario?.rol) {
        Rol.ADMINISTRADOR -> setOf(
            Screen.Ordenes.route,
            Screen.Clientes.route,
            Screen.Facturacion.route,
            Screen.Perfil.route,
        )
        Rol.OPERATIVO -> setOf(
            Screen.Ordenes.route,
            Screen.Clientes.route,
            Screen.Articulos.route,
            Screen.Perfil.route,
        )
        null -> emptySet()
    }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = currentRoute in rutasConBottomNav && usuario != null,
        drawerContent = {
            ModalDrawerSheet(modifier = Modifier.width(300.dp)) {
                ElevaProDrawerContent(
                    usuario = usuario,
                    permisos = permisos,
                    currentRoute = currentRoute,
                    navController = navController,
                    onLogout = {
                        authVm.logout()
                        scope.launch { drawerState.close() }
                    },
                    onClose = { scope.launch { drawerState.close() } },
                )
            }
        },
    ) {
        Scaffold(
            contentWindowInsets = WindowInsets(0),
            bottomBar = {
                val u = usuario
                if (u != null && currentRoute in rutasConBottomNav) {
                    ElevaProBottomNav(navController = navController, rol = u.rol)
                }
            },
        ) { padding ->
            NavGraph(
                navController = navController,
                startDestination = if (usuario == null) Screen.Onboarding.route else Screen.Ordenes.route,
                modifier = Modifier.padding(padding),
                onOpenDrawer = { scope.launch { drawerState.open() } },
            )
        }
    }
}
