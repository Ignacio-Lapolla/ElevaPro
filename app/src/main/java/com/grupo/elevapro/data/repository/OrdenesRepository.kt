package com.grupo.elevapro.data.repository

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.grupo.elevapro.data.local.datasource.OrdenLocalDataSource
import com.grupo.elevapro.data.local.entity.OrdenEntity
import com.grupo.elevapro.data.model.domain.Orden
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

interface OrdenesRepository {
    fun observarOrdenes(): Flow<List<Orden>>
    suspend fun obtenerPorId(id: String): Orden?
    suspend fun crear(orden: Orden)
    suspend fun firmar(id: String, firmaBase64: String, nombreFirmante: String)
    suspend fun agregarFoto(id: String, rutaFoto: String)
    suspend fun eliminarFoto(id: String, rutaFoto: String)
}

@Singleton
class RoomOrdenesRepository @Inject constructor(
    private val local: OrdenLocalDataSource,
) : OrdenesRepository {

    private val gson = Gson()
    private val listType = object : TypeToken<List<String>>() {}.type
    private val seedMutex = Mutex()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        scope.launch {
            seedMutex.withLock {
                if (local.contar() == 0) {
                    local.insertar(FakeMockData.ordenes.map { it.toEntity() })
                }
            }
        }
    }

    override fun observarOrdenes(): Flow<List<Orden>> =
        local.observarTodas().map { entities -> entities.map { it.toDomain() } }

    override suspend fun obtenerPorId(id: String): Orden? =
        local.obtenerPorId(id)?.toDomain()

    override suspend fun crear(orden: Orden) {
        local.insertarUna(orden.toEntity())
    }

    override suspend fun firmar(id: String, firmaBase64: String, nombreFirmante: String) {
        val entity = local.obtenerPorId(id) ?: return
        local.actualizar(entity.copy(firmada = true, firmaBase64 = firmaBase64, nombreFirmante = nombreFirmante))
    }

    override suspend fun agregarFoto(id: String, rutaFoto: String) {
        val entity = local.obtenerPorId(id) ?: return
        val fotos = parseFotos(entity.fotosJson).toMutableList()
        if (rutaFoto !in fotos) fotos.add(rutaFoto)
        local.actualizar(entity.copy(fotosJson = gson.toJson(fotos)))
    }

    override suspend fun eliminarFoto(id: String, rutaFoto: String) {
        val entity = local.obtenerPorId(id) ?: return
        val fotos = parseFotos(entity.fotosJson).filter { it != rutaFoto }
        local.actualizar(entity.copy(fotosJson = gson.toJson(fotos)))
    }

    private fun parseFotos(json: String): List<String> =
        runCatching { gson.fromJson<List<String>>(json, listType) }.getOrDefault(emptyList())

    private fun Orden.toEntity() = OrdenEntity(
        id = id, numero = numero, clienteId = clienteId, clienteNombre = clienteNombre,
        fecha = fecha, tipo = tipo, plantillaId = plantillaId, observaciones = observaciones,
        firmada = firmada, firmaBase64 = firmaBase64, nombreFirmante = nombreFirmante,
        facturado = facturado, fotosJson = "[]",
    )

    private fun OrdenEntity.toDomain() = Orden(
        id = id, numero = numero, clienteId = clienteId, clienteNombre = clienteNombre,
        fecha = fecha, tipo = tipo, plantillaId = plantillaId, observaciones = observaciones,
        firmada = firmada, firmaBase64 = firmaBase64, nombreFirmante = nombreFirmante,
        facturado = facturado, fotos = parseFotos(fotosJson),
    )
}
