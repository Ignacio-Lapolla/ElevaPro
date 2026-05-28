package com.grupo.elevapro.data.model.domain

data class Usuario(
    val id: String,
    val nombre: String,
    val email: String,
    val rol: Rol,
    val numeroEmpresa: String,
    val telefono: String?,
    val fotoUrl: String?,
)

enum class Rol { OPERATIVO, ADMINISTRADOR }
