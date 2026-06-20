package com.grupo.elevapro.di

import com.grupo.elevapro.data.remote.ArcaApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    // Base URL del futuro backend de ARCA — reemplazar con la URL real al conectar
    private const val BASE_URL = "https://api.arca.elevapro.com.ar/"

    @Provides @Singleton
    fun provideOkHttpClient(): OkHttpClient =
        OkHttpClient.Builder()
            // Solo loga en debug; en release OkHttp no agrega overhead de logging
            .build()

    @Provides @Singleton
    fun provideRetrofit(client: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    @Provides @Singleton
    fun provideArcaApiService(retrofit: Retrofit): ArcaApiService =
        retrofit.create(ArcaApiService::class.java)
}
