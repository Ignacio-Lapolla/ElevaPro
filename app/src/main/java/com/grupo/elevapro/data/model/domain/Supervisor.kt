package com.grupo.elevapro.data.model.domain

data class Supervisor(
    val id: String,
    val nombre: String,
    val telefono: String,
    val email: String,
    val especialidad: Especialidad,
    val activo: Boolean = true,
)

enum class Especialidad { PREVENTIVO, EMERGENCIA, INSTALACIONES, MODERNIZACION }
