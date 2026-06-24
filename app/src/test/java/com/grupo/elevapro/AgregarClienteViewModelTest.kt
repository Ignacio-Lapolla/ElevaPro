package com.grupo.elevapro

import androidx.lifecycle.SavedStateHandle
import com.grupo.elevapro.data.model.domain.Cliente
import com.grupo.elevapro.data.model.domain.Supervisor
import com.grupo.elevapro.data.repository.SupervisoresRepository
import com.grupo.elevapro.ui.screen.clientes.AgregarClienteUiState
import com.grupo.elevapro.ui.screen.clientes.AgregarClienteViewModel
import com.grupo.elevapro.ui.navigation.Screen
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
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
class AgregarClienteViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    private val clienteEjemplo = Cliente(
        id = "c1",
        nombre = "Empresa Test",
        direccion = "Av. Test 123",
        telefono = "1122334455",
        email = "test@test.com",
        cuit = "20123456789",
        notas = "nota",
        supervisorId = null,
    )

    @Before
    fun setUp() { Dispatchers.setMain(dispatcher) }

    @After
    fun tearDown() { Dispatchers.resetMain() }

    private val fakeSupervisoresRepo = object : SupervisoresRepository {
        override fun observarSupervisores(): Flow<List<Supervisor>> = flowOf(emptyList())
        override suspend fun obtenerPorId(id: String): Supervisor? = null
        override suspend fun crear(supervisor: Supervisor) {}
        override suspend fun actualizar(supervisor: Supervisor) {}
    }

    private fun buildVm(
        repo: FakeClienteRepository = FakeClienteRepository(),
        clienteId: String? = null,
    ): AgregarClienteViewModel {
        val handle = if (clienteId != null) {
            SavedStateHandle(mapOf(Screen.AgregarCliente.ARG_ID to clienteId))
        } else {
            SavedStateHandle()
        }
        return AgregarClienteViewModel(handle, repo, fakeSupervisoresRepo)
    }

    @Test
    fun `estado inicial es Formulario vacio`() = runTest {
        val vm = buildVm()
        val estado = vm.estado.first()
        assertTrue(estado is AgregarClienteUiState.Formulario)
        val form = (estado as AgregarClienteUiState.Formulario).form
        assertEquals("", form.nombre)
        assertFalse(form.guardando)
    }

    @Test
    fun `onNombre actualiza el form`() = runTest {
        val vm = buildVm()
        vm.onNombre("Nueva Empresa")
        val form = (vm.estado.first() as AgregarClienteUiState.Formulario).form
        assertEquals("Nueva Empresa", form.nombre)
    }

    @Test
    fun `cuit se recorta a 11 digitos`() = runTest {
        val vm = buildVm()
        vm.onCuit("123456789012345")
        val form = (vm.estado.first() as AgregarClienteUiState.Formulario).form
        assertEquals("12345678901", form.cuit)
        assertEquals(11, form.cuit.length)
    }

    @Test
    fun `cuit solo acepta digitos`() = runTest {
        val vm = buildVm()
        vm.onCuit("20-123456789")
        val form = (vm.estado.first() as AgregarClienteUiState.Formulario).form
        assertEquals("20123456789", form.cuit)
    }

    @Test
    fun `form sin nombre no es valido`() = runTest {
        val vm = buildVm()
        val form = (vm.estado.first() as AgregarClienteUiState.Formulario).form
        assertFalse(form.isFormValid)
    }

    @Test
    fun `guardar sin nombre no emite Guardado`() = runTest {
        val vm = buildVm()
        vm.guardar()
        dispatcher.scheduler.advanceUntilIdle()
        assertTrue(vm.estado.first() is AgregarClienteUiState.Formulario)
    }

    @Test
    fun `guardar con nombre valido emite Guardado`() = runTest {
        val repo = FakeClienteRepository()
        val vm = buildVm(repo)
        vm.onNombre("Cliente Nuevo")
        vm.guardar()
        dispatcher.scheduler.advanceUntilIdle()
        assertTrue(vm.estado.first() is AgregarClienteUiState.Guardado)
    }

    @Test
    fun `guardar crea el cliente en el repositorio`() = runTest {
        val repo = FakeClienteRepository()
        val vm = buildVm(repo)
        vm.onNombre("Cliente Nuevo")
        vm.guardar()
        dispatcher.scheduler.advanceUntilIdle()
        val clientes = repo.observarClientes().first()
        assertEquals(1, clientes.size)
        assertEquals("Cliente Nuevo", clientes[0].nombre)
    }

    @Test
    fun `en modo edicion carga los datos del cliente existente`() = runTest {
        val repo = FakeClienteRepository(listOf(clienteEjemplo))
        val vm = buildVm(repo, clienteId = "c1")
        dispatcher.scheduler.advanceUntilIdle()
        val form = (vm.estado.first() as AgregarClienteUiState.Formulario).form
        assertEquals("Empresa Test", form.nombre)
        assertEquals("20123456789", form.cuit)
    }

    @Test
    fun `en modo edicion guardar actualiza en lugar de crear`() = runTest {
        val repo = FakeClienteRepository(listOf(clienteEjemplo))
        val vm = buildVm(repo, clienteId = "c1")
        dispatcher.scheduler.advanceUntilIdle()
        vm.onNombre("Empresa Actualizada")
        vm.guardar()
        dispatcher.scheduler.advanceUntilIdle()
        val clientes = repo.observarClientes().first()
        assertEquals(1, clientes.size)
        assertEquals("Empresa Actualizada", clientes[0].nombre)
    }
}
