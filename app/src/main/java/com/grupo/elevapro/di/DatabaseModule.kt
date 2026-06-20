package com.grupo.elevapro.di

import android.content.Context
import androidx.room.Room
import com.grupo.elevapro.data.local.AppDatabase
import com.grupo.elevapro.data.local.dao.ClienteDao
import com.grupo.elevapro.data.local.dao.OrdenDao
import com.grupo.elevapro.data.local.dao.PermisoDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides @Singleton
    fun provideDatabase(@ApplicationContext ctx: Context): AppDatabase =
        Room.databaseBuilder(ctx, AppDatabase::class.java, "elevapro.db")
            // Dev/TP only — en producción reemplazar con Migration explícita para no perder datos
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()

    @Provides @Singleton @ApplicationScope
    fun provideApplicationScope(): CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Provides
    fun provideOrdenDao(db: AppDatabase): OrdenDao = db.ordenDao()

    @Provides
    fun providePermisoDao(db: AppDatabase): PermisoDao = db.permisoDao()

    @Provides
    fun provideClienteDao(db: AppDatabase): ClienteDao = db.clienteDao()
}
