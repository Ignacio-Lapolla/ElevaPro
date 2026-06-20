package com.grupo.elevapro.data.remote.dto

import com.grupo.elevapro.data.model.domain.EstadoFactura
import com.grupo.elevapro.data.model.domain.Factura

data class FacturaDto(
    val id: String,
    val numero: String,
    val tipo: String,
    val clienteId: String,
    val clienteNombre: String,
    val fecha: String,
    val monto: Double,
    val estado: String,
    val cae: String? = null,
    val vencimientoCae: String? = null,
    val ordenesIds: List<String> = emptyList(),
)

fun FacturaDto.toDomain() = Factura(
    id = id,
    numero = numero,
    tipo = tipo,
    clienteId = clienteId,
    clienteNombre = clienteNombre,
    fecha = fecha,
    monto = monto,
    estado = runCatching { EstadoFactura.valueOf(estado) }.getOrDefault(EstadoFactura.PENDIENTE),
    cae = cae,
    vencimientoCae = vencimientoCae,
    ordenesIds = ordenesIds,
)
