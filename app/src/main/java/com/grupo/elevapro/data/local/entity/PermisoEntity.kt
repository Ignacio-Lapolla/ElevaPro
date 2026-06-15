package com.grupo.elevapro.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "permisos")
data class PermisoEntity(
    @PrimaryKey val id: String,       // "usuarioId:PERMISO_NAME"
    val usuarioId: String,
    val permiso: String,
)
