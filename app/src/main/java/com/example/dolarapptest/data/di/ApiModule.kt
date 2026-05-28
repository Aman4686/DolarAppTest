package com.example.dolarapptest.data.di

import com.example.dolarapptest.data.typeadapter.BigDecimalTypeAdapter
import com.example.dolarapptest.data.api.TickersApi
import com.google.gson.GsonBuilder
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.math.BigDecimal
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class ApiModule {

    private val BASE_URL = "https://api.dolarapp.dev/"

    @Provides
    @Singleton
    fun provideDolarApi(): TickersApi {
        val gson = GsonBuilder()
            .registerTypeAdapter(BigDecimal::class.java, BigDecimalTypeAdapter())
            .create()

        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(TickersApi::class.java)
    }
}