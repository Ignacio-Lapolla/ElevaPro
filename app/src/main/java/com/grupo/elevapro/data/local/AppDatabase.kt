package com.grupo.elevapro.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.grupo.elevapro.data.local.dao.OrdenDao
import com.grupo.elevapro.data.local.dao.PermisoDao
import com.grupo.elevapro.data.local.entity.OrdenEntity
import com.grupo.elevapro.data.local.entity.PermisoEntity

@Database(
    entities = [OrdenEntity::class, PermisoEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun ordenDao(): OrdenDao
    abstract fun permisoDao(): PermisoDao
}
