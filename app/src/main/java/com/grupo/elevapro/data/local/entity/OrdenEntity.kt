package com.grupo.elevapro.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "ordenes",
    foreignKeys = [
        ForeignKey(
            entity = ClienteEntity::class,
            parentColumns = ["id"],
            childColumns = ["clienteId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index("clienteId")],
)
data class OrdenEntity(
    @PrimaryKey val id: String,
    val numero: String,
    val clienteId: String,
    val clienteNombre: String,
    val fecha: String,
    val tipo: String,
    val plantillaId: String?,
    val observaciones: String,
    val firmada: Boolean,
    val firmaBase64: String?,
    val nombreFirmante: String?,
    val facturado: Boolean,
    val fotosJson: String = "[]",     // JSON array de rutas de archivo
)
