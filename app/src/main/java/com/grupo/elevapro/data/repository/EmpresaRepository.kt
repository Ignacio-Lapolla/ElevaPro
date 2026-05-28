package com.grupo.elevapro.data.repository

import com.grupo.elevapro.data.model.domain.Empresa
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

interface EmpresaRepository {
    fun observar(): Flow<Empresa>
    suspend fun actualizar(empresa: Empresa)
}

@Singleton
class FakeEmpresaRepository @Inject constructor() : EmpresaRepository {

    private val _empresa = MutableStateFlow(FakeMockData.empresa)

    override fun observar(): Flow<Empresa> = _empresa.asStateFlow()

    override suspend fun actualizar(empresa: Empresa) {
        _empresa.value = empresa
    }
}
