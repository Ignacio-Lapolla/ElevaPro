package com.grupo.elevapro.di

import com.grupo.elevapro.data.repository.ArticulosRepository
import com.grupo.elevapro.data.repository.AuthRepository
import com.grupo.elevapro.data.repository.FakeArticulosRepository
import com.grupo.elevapro.data.repository.FakeAuthRepository
import com.grupo.elevapro.data.repository.FakeNotificacionesRepository
import com.grupo.elevapro.data.repository.FakeOrdenesRepository
import com.grupo.elevapro.data.repository.NotificacionesRepository
import com.grupo.elevapro.data.repository.OrdenesRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: FakeAuthRepository): AuthRepository

    @Binds
    @Singleton
    abstract fun bindOrdenesRepository(impl: FakeOrdenesRepository): OrdenesRepository

    @Binds
    @Singleton
    abstract fun bindArticulosRepository(impl: FakeArticulosRepository): ArticulosRepository

    @Binds
    @Singleton
    abstract fun bindNotificacionesRepository(impl: FakeNotificacionesRepository): NotificacionesRepository
}
