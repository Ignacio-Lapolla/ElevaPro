package com.grupo.elevapro.data.model.domain

enum class TipoNotificacion {
    ORDEN_FIRMADA,
    ORDEN_PENDIENTE,
    FACTURA_APROBADA,
    FACTURA_RECHAZADA,
    CLIENTE_NUEVO,
    ALERTA,
    SUPERVISOR,
    SISTEMA,
}

enum class GrupoNotificacion(val label: String) {
    HOY("Hoy"),
    AYER("Ayer"),
    ESTA_SEMANA("Esta semana"),
}

data class Notificacion(
    val id: String,
    val tipo: TipoNotificacion,
    val titulo: String,
    val cuerpo: String,
    val hora: String,
    val leida: Boolean,
    val grupo: GrupoNotificacion,
)
