package com.grupo.elevapro.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.grupo.elevapro.data.local.entity.OrdenEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface OrdenDao {
    @Query("SELECT * FROM ordenes ORDER BY fecha DESC")
    fun observarTodas(): Flow<List<OrdenEntity>>

    @Query("SELECT * FROM ordenes WHERE id = :id")
    suspend fun obtenerPorId(id: String): OrdenEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertar(ordenes: List<OrdenEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarUna(orden: OrdenEntity)

    @Update
    suspend fun actualizar(orden: OrdenEntity)

    @Query("SELECT COUNT(*) FROM ordenes")
    suspend fun contar(): Int
}
