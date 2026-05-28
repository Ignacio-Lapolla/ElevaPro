package com.grupo.elevapro.data.model.domain

data class Orden(
    val id: String,
    val numero: String,
    val clienteId: String,
    val clienteNombre: String,
    val fecha: String,
    val tipo: String,
    val plantillaId: String? = null,
    val observaciones: String = "",
    val firmada: Boolean = false,
    val firmaBase64: String? = null,
    val nombreFirmante: String? = null,
    val facturado: Boolean = false,
)
