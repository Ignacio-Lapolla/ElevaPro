package com.grupo.elevapro.di

import android.content.Context
import androidx.room.Room
import com.grupo.elevapro.data.local.AppDatabase
import com.grupo.elevapro.data.local.dao.OrdenDao
import com.grupo.elevapro.data.local.dao.PermisoDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides @Singleton
    fun provideDatabase(@ApplicationContext ctx: Context): AppDatabase =
        Room.databaseBuilder(ctx, AppDatabase::class.java, "elevapro.db").build()

    @Provides
    fun provideOrdenDao(db: AppDatabase): OrdenDao = db.ordenDao()

    @Provides
    fun providePermisoDao(db: AppDatabase): PermisoDao = db.permisoDao()
}
