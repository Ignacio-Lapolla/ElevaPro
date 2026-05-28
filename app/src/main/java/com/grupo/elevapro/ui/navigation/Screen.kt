package com.grupo.elevapro.ui.navigation

sealed class Screen(val route: String) {
    data object Login : Screen("login")
    data object Onboarding : Screen("onboarding")

    data object Ordenes : Screen("ordenes")
    data object OrdenDetalle : Screen("orden/{id}") {
        const val ARG_ID = "id"
        fun build(id: String) = "orden/$id"
    }
    data object NuevaOrden : Screen("orden/nueva")
    data object Firma : Screen("firma/{ordenId}") {
        const val ARG_ID = "ordenId"
        fun build(ordenId: String) = "firma/$ordenId"
    }

    data object Clientes : Screen("clientes")
    data object ClienteDetalle : Screen("cliente/{id}") {
        const val ARG_ID = "id"
        fun build(id: String) = "cliente/$id"
    }
    data object AgregarCliente : Screen("cliente/nuevo?clienteId={clienteId}") {
        const val ARG_ID = "clienteId"
        fun build(id: String) = "cliente/nuevo?clienteId=$id"
        val route_base = "cliente/nuevo"
    }

    data object Facturacion : Screen("facturacion")
    data object FacturaDetalle : Screen("factura/{id}") {
        const val ARG_ID = "id"
        fun build(id: String) = "factura/$id"
    }
    data object GenerarFactura : Screen("factura/generar")
    data object NuevaFactura : Screen("factura/nueva")

    data object Articulos : Screen("articulos")

    data object Usuarios : Screen("usuarios")
    data object UsuarioPermisos : Screen("usuario/{id}/permisos") {
        const val ARG_ID = "id"
        fun build(id: String) = "usuario/$id/permisos"
    }
    data object RolesPermisos : Screen("roles")
    data object Supervisores : Screen("supervisores")
    data object Plantillas : Screen("plantillas")
    data object DatosEmpresa : Screen("empresa")

    data object Perfil : Screen("perfil")
    data object Notificaciones : Screen("notificaciones")
    data object Configuracion : Screen("configuracion")
    data object AyudaSoporte : Screen("ayuda")
    data object Opciones : Screen("opciones")
}
