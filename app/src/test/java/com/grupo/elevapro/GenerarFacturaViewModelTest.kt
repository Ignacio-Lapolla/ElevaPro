package com.grupo.elevapro

import com.grupo.elevapro.data.model.domain.Cliente
import com.grupo.elevapro.ui.screen.facturacion.GenerarFacturaUiState
import com.grupo.elevapro.ui.screen.facturacion.GenerarFacturaViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
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
class GenerarFacturaViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    private val clienteId = "1"
    private val clienteNombre = "Cons. Prop. Lafinur 3060"

    @Before
    fun setUp() { Dispatchers.setMain(dispatcher) }

    @After
    fun tearDown() { Dispatchers.resetMain() }

    private fun buildVm(
        clienteRepo: FakeClienteRepository = FakeClienteRepository(listOf(
            Cliente(clienteId, clienteNombre, "Lafinur 3060", "+54 11 4801-1234", "admin@lafinur3060.com.ar", "30-65432189-1")
        )),
        ordenesRepo: FakeOrdenesRepository = FakeOrdenesRepository(),
        facturacionRepo: FakeFacturacionRepository = FakeFacturacionRepository(),
    ) = GenerarFacturaViewModel(clienteRepo, ordenesRepo, facturacionRepo)

    private suspend fun GenerarFacturaViewModel.llenarFormValido() {
        onCliente(clienteId, clienteNombre)
        dispatcher.scheduler.advanceUntilIdle() // onCliente es async → flush para que el CUIT se cargue
        onTipo("B")
        onFechaDesde("01/06/2026")
        onFechaHasta("30/06/2026")
        onTipoPago("Transferencia")
        onDescripcion("Servicio de mantenimiento")
        onMontoNeto("1000")
    }

    @Test
    fun `estado inicial es Loading luego Formulario`() = runTest {
        val vm = buildVm()
        dispatcher.scheduler.advanceUntilIdle()
        assertTrue(vm.estado.first() is GenerarFacturaUiState.Formulario)
    }

    @Test
    fun `Formulario expone los clientes del repositorio`() = runTest {
        val vm = buildVm()
        dispatcher.scheduler.advanceUntilIdle()
        val estado = vm.estado.first() as GenerarFacturaUiState.Formulario
        assertEquals(1, estado.clientes.size)
        assertEquals(clienteNombre, estado.clientes[0].nombre)
    }

    @Test
    fun `form sin datos no es valido`() = runTest {
        val vm = buildVm()
        dispatcher.scheduler.advanceUntilIdle()
        val estado = vm.estado.first() as GenerarFacturaUiState.Formulario
        assertFalse(estado.form.isFormValid)
    }

    @Test
    fun `generar sin form valido no navega atras`() = runTest {
        val vm = buildVm()
        dispatcher.scheduler.advanceUntilIdle()
        vm.generar()
        dispatcher.scheduler.advanceUntilIdle()
        assertTrue(vm.estado.first() is GenerarFacturaUiState.Formulario)
    }

    @Test
    fun `generar con form completo navega atras`() = runTest {
        val vm = buildVm()
        dispatcher.scheduler.advanceUntilIdle()
        vm.llenarFormValido()
        dispatcher.scheduler.advanceUntilIdle()
        var navigated = false
        backgroundScope.launch { vm.navegarAtras.first(); navigated = true }
        vm.generar()
        dispatcher.scheduler.advanceUntilIdle()
        assertTrue(navigated)
    }

    @Test
    fun `generar agrega la factura al repositorio`() = runTest {
        val facturacionRepo = FakeFacturacionRepository()
        val vm = buildVm(facturacionRepo = facturacionRepo)
        dispatcher.scheduler.advanceUntilIdle()
        vm.llenarFormValido()
        dispatcher.scheduler.advanceUntilIdle()
        vm.generar()
        dispatcher.scheduler.advanceUntilIdle()
        val facturas = facturacionRepo.observarFacturas().first()
        assertEquals(1, facturas.size)
        assertEquals("B", facturas[0].tipo)
    }

    @Test
    fun `onMontoNeto solo acepta digitos coma y punto`() = runTest {
        val vm = buildVm()
        dispatcher.scheduler.advanceUntilIdle()
        vm.onMontoNeto("1.500,75abc$%")
        dispatcher.scheduler.advanceUntilIdle()
        val form = (vm.estado.first() as GenerarFacturaUiState.Formulario).form
        assertEquals("1.500,75", form.montoNetoStr)
    }

    @Test
    fun `calculo IVA correcto para tipo A`() = runTest {
        val vm = buildVm()
        dispatcher.scheduler.advanceUntilIdle()
        vm.onTipo("A")
        vm.onMontoNeto("1000")
        dispatcher.scheduler.advanceUntilIdle()
        val form = (vm.estado.first() as GenerarFacturaUiState.Formulario).form
        assertEquals(210.0, form.montoIVA, 0.01)
        assertEquals(1210.0, form.montoTotal, 0.01)
    }

    @Test
    fun `calculo IVA es cero para tipo C`() = runTest {
        val vm = buildVm()
        dispatcher.scheduler.advanceUntilIdle()
        vm.onTipo("C")
        vm.onMontoNeto("1000")
        dispatcher.scheduler.advanceUntilIdle()
        val form = (vm.estado.first() as GenerarFacturaUiState.Formulario).form
        assertEquals(0.0, form.montoIVA, 0.01)
        assertEquals(1000.0, form.montoTotal, 0.01)
    }

    @Test
    fun `factura generada usa monto total con IVA para tipo B`() = runTest {
        val facturacionRepo = FakeFacturacionRepository()
        val vm = buildVm(facturacionRepo = facturacionRepo)
        dispatcher.scheduler.advanceUntilIdle()
        vm.llenarFormValido() // tipo B, monto 1000
        dispatcher.scheduler.advanceUntilIdle()
        vm.generar()
        dispatcher.scheduler.advanceUntilIdle()
        val factura = facturacionRepo.observarFacturas().first()[0]
        // tipo B tiene IVA 21% → monto total = 1210
        assertEquals(1210.0, factura.monto, 0.01)
    }
}
