package com.grupo.elevapro.data.remote

import com.grupo.elevapro.data.remote.dto.FacturaDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * Stub de la API de ARCA (AFIP) para facturación electrónica.
 * Actualmente no conectado a un backend real — los endpoints reflejan
 * el contrato esperado para la integración futura.
 */
interface ArcaApiService {
    @GET("facturas")
    suspend fun getFacturas(): List<FacturaDto>

    @GET("facturas/{id}")
    suspend fun getFactura(@Path("id") id: String): FacturaDto

    @POST("facturas")
    suspend fun emitirFactura(@Body factura: FacturaDto): FacturaDto
}
