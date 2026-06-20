package com.grupo.elevapro.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.grupo.elevapro.data.model.domain.Cliente

@Entity(tableName = "clientes")
data class ClienteEntity(
    @PrimaryKey val id: String,
    val nombre: String,
    val direccion: String,
    val telefono: String,
    val email: String,
    val cuit: String,
    val supervisorId: String?,
    val notas: String?,
)

fun ClienteEntity.toDomain() = Cliente(
    id = id,
    nombre = nombre,
    direccion = direccion,
    telefono = telefono,
    email = email,
    cuit = cuit,
    supervisorId = supervisorId,
    notas = notas,
)

fun Cliente.toEntity() = ClienteEntity(
    id = id,
    nombre = nombre,
    direccion = direccion,
    telefono = telefono,
    email = email,
    cuit = cuit,
    supervisorId = supervisorId,
    notas = notas,
)
