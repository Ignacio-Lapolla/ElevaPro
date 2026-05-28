package com.grupo.elevapro.data.model.domain

data class Plantilla(
    val id: String,
    val nombre: String,
    val descripcion: String,
    val categoria: String,
    val tareas: List<String>,
    val tiempoEstimadoMin: Int,
)
