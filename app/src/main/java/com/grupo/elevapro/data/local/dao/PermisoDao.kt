package com.grupo.elevapro.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.grupo.elevapro.data.local.entity.PermisoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PermisoDao {
    @Query("SELECT * FROM permisos WHERE usuarioId = :usuarioId")
    fun observar(usuarioId: String): Flow<List<PermisoEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertar(permisos: List<PermisoEntity>)

    @Query("DELETE FROM permisos WHERE usuarioId = :usuarioId")
    suspend fun eliminarDeUsuario(usuarioId: String)

    @Query("SELECT COUNT(*) > 0 FROM permisos WHERE usuarioId = :usuarioId")
    suspend fun existeAlguno(usuarioId: String): Boolean
}
