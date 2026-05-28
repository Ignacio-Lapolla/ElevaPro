package com.grupo.elevapro.di

import com.grupo.elevapro.data.repository.AuthRepository
import com.grupo.elevapro.data.repository.ClienteRepository
import com.grupo.elevapro.data.repository.FakeAuthRepository
import com.grupo.elevapro.data.repository.FakeClienteRepository
import com.grupo.elevapro.data.repository.FakeFacturacionRepository
import com.grupo.elevapro.data.repository.FakeOrdenesRepository
import com.grupo.elevapro.data.repository.FacturacionRepository
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
    abstract fun bindClienteRepository(impl: FakeClienteRepository): ClienteRepository

    @Binds
    @Singleton
    abstract fun bindFacturacionRepository(impl: FakeFacturacionRepository): FacturacionRepository
}
