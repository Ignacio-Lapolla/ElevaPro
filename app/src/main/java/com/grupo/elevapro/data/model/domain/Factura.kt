package com.grupo.elevapro.data.model.domain

data class Factura(
    val id: String,
    val numero: String,
    val tipo: String, // "A", "B", "C"
    val clienteId: String,
    val clienteNombre: String,
    val fecha: String,
    val monto: Double,
    val estado: EstadoFactura,
    val cae: String? = null,
    val vencimientoCae: String? = null,
    val ordenesIds: List<String> = emptyList(),
)

enum class EstadoFactura { PENDIENTE, APROBADA, RECHAZADA }
