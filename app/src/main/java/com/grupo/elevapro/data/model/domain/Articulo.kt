package com.grupo.elevapro.data.model.domain

data class Articulo(
    val id: String,
    val codigo: String,
    val nombre: String,
    val descripcion: String = "",
    val precio: Double,
    val categoria: String,
    val stock: Int,
)
