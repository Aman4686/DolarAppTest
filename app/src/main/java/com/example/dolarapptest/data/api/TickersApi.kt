package com.example.dolarapptest.data.api

import com.example.dolarapptest.data.model.TickerResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface TickersApi {

    @GET("v1/tickers")
    suspend fun getTickers(
        @Query("currencies") currencies: String
    ): List<TickerResponse>

    @GET("v1/tickers-currencies")
    suspend fun getAvailableCurrencies(): List<String>
}