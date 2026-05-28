package com.grupo.elevapro

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.grupo.elevapro.data.model.domain.Usuario
import com.grupo.elevapro.data.repository.AuthRepository
import com.grupo.elevapro.ui.components.ElevaProBottomNav
import com.grupo.elevapro.ui.navigation.NavGraph
import com.grupo.elevapro.ui.navigation.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class AppViewModel @Inject constructor(
    authRepository: AuthRepository,
) : ViewModel() {
    val usuarioActual: StateFlow<Usuario?> = authRepository.usuarioActual
}

@Composable
fun ElevaProApp() {
    val navController = rememberNavController()
    val authVm: AppViewModel = hiltViewModel()
    val usuario by authVm.usuarioActual.collectAsStateWithLifecycle()
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route

    val rutasConBottomNav = setOf(
        Screen.Ordenes.route,
        Screen.Clientes.route,
        Screen.Articulos.route,
        Screen.Facturacion.route,
        Screen.Perfil.route,
    )

    Scaffold(
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
        )
    }
}
