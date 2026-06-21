package com.grupo.elevapro.data.remote

import com.grupo.elevapro.data.remote.dto.FacturaDto
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RemoteFacturacionDataSource @Inject constructor(
    private val api: ArcaApiService,
) {
    suspend fun getFacturas(): List<FacturaDto> = api.getFacturas()
    suspend fun emitirFactura(dto: FacturaDto): FacturaDto = api.emitirFactura(dto)
}
