package com.example.dolarapptest.domain.repository

import com.example.dolarapptest.domain.model.Ticker

interface TickersRepository {
    suspend fun getTickers(currencies: List<String>): Result<List<Ticker>>
    suspend fun getAvailableCurrencies(): List<String>
}
