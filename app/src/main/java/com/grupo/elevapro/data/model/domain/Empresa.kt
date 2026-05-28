package com.grupo.elevapro.data.model.domain

data class Empresa(
    val id: String,
    val razonSocial: String,
    val cuit: String,
    val direccion: String,
    val telefono: String,
    val email: String,
    val condicionIva: CondicionIva,
    val certificadoAfipNombre: String? = null,
)

enum class CondicionIva { RESPONSABLE_INSCRIPTO, MONOTRIBUTISTA, EXENTO }
