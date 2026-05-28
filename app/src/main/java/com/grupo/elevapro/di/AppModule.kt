package com.grupo.elevapro.di

import com.grupo.elevapro.data.repository.AuthRepository
import com.grupo.elevapro.data.repository.EmpresaRepository
import com.grupo.elevapro.data.repository.FakeAuthRepository
import com.grupo.elevapro.data.repository.FakeEmpresaRepository
import com.grupo.elevapro.data.repository.FakeOrdenesRepository
import com.grupo.elevapro.data.repository.FakePermisosRepository
import com.grupo.elevapro.data.repository.FakePlantillasRepository
import com.grupo.elevapro.data.repository.FakeSupervisoresRepository
import com.grupo.elevapro.data.repository.FakeUsuariosRepository
import com.grupo.elevapro.data.repository.OrdenesRepository
import com.grupo.elevapro.data.repository.PermisosRepository
import com.grupo.elevapro.data.repository.PlantillasRepository
import com.grupo.elevapro.data.repository.SupervisoresRepository
import com.grupo.elevapro.data.repository.UsuariosRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {

    @Binds @Singleton
    abstract fun bindAuthRepository(impl: FakeAuthRepository): AuthRepository

    @Binds @Singleton
    abstract fun bindOrdenesRepository(impl: FakeOrdenesRepository): OrdenesRepository

    @Binds @Singleton
    abstract fun bindUsuariosRepository(impl: FakeUsuariosRepository): UsuariosRepository

    @Binds @Singleton
    abstract fun bindSupervisoresRepository(impl: FakeSupervisoresRepository): SupervisoresRepository

    @Binds @Singleton
    abstract fun bindPlantillasRepository(impl: FakePlantillasRepository): PlantillasRepository

    @Binds @Singleton
    abstract fun bindEmpresaRepository(impl: FakeEmpresaRepository): EmpresaRepository

    @Binds @Singleton
    abstract fun bindPermisosRepository(impl: FakePermisosRepository): PermisosRepository
}
