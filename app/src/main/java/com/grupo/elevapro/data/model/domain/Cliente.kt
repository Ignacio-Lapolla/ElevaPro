package com.grupo.elevapro.data.model.domain

data class Cliente(
    val id: String,
    val nombre: String,
    val direccion: String,
    val telefono: String,
    val email: String,
    val cuit: String,
    val notas: String? = null,
    val supervisorId: String? = null,
)
