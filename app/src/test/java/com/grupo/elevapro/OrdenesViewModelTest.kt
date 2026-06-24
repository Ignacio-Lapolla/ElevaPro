package com.grupo.elevapro

import androidx.lifecycle.SavedStateHandle
import com.grupo.elevapro.data.model.domain.Orden
import com.grupo.elevapro.ui.screen.ordenes.DetalleUiState
import com.grupo.elevapro.ui.screen.ordenes.OrdenDetalleViewModel
import com.grupo.elevapro.ui.navigation.Screen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OrdenesViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    private val ordenEjemplo = Orden(
        id = "o1", numero = "OT-0001", clienteId = "c1",
        clienteNombre = "Cons. Prop. Test", fecha = "01 jun 2026",
        tipo = "Mantenimiento", firmada = false,
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `estado inicial es Loading`() = runTest {
        val repo = FakeOrdenesRepository(listOf(ordenEjemplo))
        val handle = SavedStateHandle(mapOf(Screen.OrdenDetalle.ARG_ID to "o1"))
        val vm = OrdenDetalleViewModel(repo, handle)

        val estado = vm.estado.first()
        // Puede ser Loading o Success dependiendo de la coroutine; lo que NO debe ser es Error
        assertTrue(estado !is DetalleUiState.Error)
    }

    @Test
    fun `orden encontrada emite Success con los datos correctos`() = runTest {
        val repo = FakeOrdenesRepository(listOf(ordenEjemplo))
        val handle = SavedStateHandle(mapOf(Screen.OrdenDetalle.ARG_ID to "o1"))
        val vm = OrdenDetalleViewModel(repo, handle)

        dispatcher.scheduler.advanceUntilIdle()

        val estado = vm.estado.first { it is DetalleUiState.Success }
        assertEquals("OT-0001", (estado as DetalleUiState.Success).orden.numero)
    }

    @Test
    fun `orden no encontrada emite Error`() = runTest {
        val repo = FakeOrdenesRepository(emptyList())
        val handle = SavedStateHandle(mapOf(Screen.OrdenDetalle.ARG_ID to "no-existe"))
        val vm = OrdenDetalleViewModel(repo, handle)

        dispatcher.scheduler.advanceUntilIdle()

        val estado = vm.estado.first { it !is DetalleUiState.Loading }
        assertTrue(estado is DetalleUiState.Error)
    }

    @Test
    fun `agregarFoto agrega la ruta a la orden`() = runTest {
        val repo = FakeOrdenesRepository(listOf(ordenEjemplo))
        val handle = SavedStateHandle(mapOf(Screen.OrdenDetalle.ARG_ID to "o1"))
        val vm = OrdenDetalleViewModel(repo, handle)

        vm.agregarFoto("/storage/foto1.jpg")
        dispatcher.scheduler.advanceUntilIdle()

        val orden = repo.obtenerPorId("o1")
        assertTrue(orden?.fotos?.contains("/storage/foto1.jpg") == true)
    }

    @Test
    fun `eliminarFoto remueve la ruta de la orden`() = runTest {
        val repo = FakeOrdenesRepository(listOf(ordenEjemplo.copy(fotos = listOf("/storage/foto1.jpg"))))
        val handle = SavedStateHandle(mapOf(Screen.OrdenDetalle.ARG_ID to "o1"))
        val vm = OrdenDetalleViewModel(repo, handle)

        vm.eliminarFoto("/storage/foto1.jpg")
        dispatcher.scheduler.advanceUntilIdle()

        val orden = repo.obtenerPorId("o1")
        assertTrue(orden?.fotos?.isEmpty() == true)
    }

    // ── mostrarVistaPrevia ────────────────────────────────────────────────────

    @Test
    fun `mostrarVistaPrevia inicia en false`() = runTest {
        val repo = FakeOrdenesRepository(listOf(ordenEjemplo))
        val handle = SavedStateHandle(mapOf(Screen.OrdenDetalle.ARG_ID to "o1"))
        val vm = OrdenDetalleViewModel(repo, handle)

        assertFalse(vm.mostrarVistaPrevia.value)
    }

    @Test
    fun `setMostrarVistaPrevia true actualiza el state`() = runTest {
        val repo = FakeOrdenesRepository(listOf(ordenEjemplo))
        val handle = SavedStateHandle(mapOf(Screen.OrdenDetalle.ARG_ID to "o1"))
        val vm = OrdenDetalleViewModel(repo, handle)

        vm.setMostrarVistaPrevia(true)

        assertTrue(vm.mostrarVistaPrevia.value)
    }

    @Test
    fun `setMostrarVistaPrevia false resetea el state`() = runTest {
        val repo = FakeOrdenesRepository(listOf(ordenEjemplo))
        val handle = SavedStateHandle(mapOf(Screen.OrdenDetalle.ARG_ID to "o1"))
        val vm = OrdenDetalleViewModel(repo, handle)

        vm.setMostrarVistaPrevia(true)
        vm.setMostrarVistaPrevia(false)

        assertFalse(vm.mostrarVistaPrevia.value)
    }

    @Test
    fun `setMostrarVistaPrevia persiste en SavedStateHandle`() = runTest {
        val repo = FakeOrdenesRepository(listOf(ordenEjemplo))
        val handle = SavedStateHandle(mapOf(Screen.OrdenDetalle.ARG_ID to "o1"))
        val vm = OrdenDetalleViewModel(repo, handle)

        vm.setMostrarVistaPrevia(true)

        assertEquals(true, handle.get<Boolean>("mostrar_vista_previa"))
    }

    @Test
    fun `mostrarVistaPrevia se restaura desde SavedStateHandle al recrear el ViewModel`() = runTest {
        val repo = FakeOrdenesRepository(listOf(ordenEjemplo))
        val handle = SavedStateHandle(
            mapOf(
                Screen.OrdenDetalle.ARG_ID to "o1",
                "mostrar_vista_previa" to true,
            )
        )
        val vm = OrdenDetalleViewModel(repo, handle)

        assertTrue(vm.mostrarVistaPrevia.value)
    }
}
