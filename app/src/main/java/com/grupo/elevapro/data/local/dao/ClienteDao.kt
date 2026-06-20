package com.grupo.elevapro.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.grupo.elevapro.data.local.entity.ClienteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ClienteDao {
    @Query("SELECT * FROM clientes ORDER BY nombre ASC")
    fun observarTodos(): Flow<List<ClienteEntity>>

    @Query("SELECT * FROM clientes WHERE id = :id")
    suspend fun obtenerPorId(id: String): ClienteEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertar(clientes: List<ClienteEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarUno(cliente: ClienteEntity)

    @Update
    suspend fun actualizar(cliente: ClienteEntity)

    @Query("SELECT COUNT(*) FROM clientes")
    suspend fun contar(): Int
}
