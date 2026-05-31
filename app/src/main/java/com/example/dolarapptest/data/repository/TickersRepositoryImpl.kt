package com.example.dolarapptest.data.repository

import com.example.dolarapptest.data.api.TickersApi
import com.example.dolarapptest.data.mapper.toDomain
import com.example.dolarapptest.data.model.Currency
import com.example.dolarapptest.domain.di.IoDispatcher
import com.example.dolarapptest.domain.model.Ticker
import com.example.dolarapptest.domain.repository.TickersRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TickersRepositoryImpl @Inject constructor(
    private val api: TickersApi,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : TickersRepository {

    override suspend fun getTickers(currencies: List<String>): Result<List<Ticker>> = withContext(ioDispatcher) {
            runCatching {
                api.getTickers(currencies.joinToString(",")).map { it.toDomain() }
            }
        }

    override suspend fun getAvailableCurrencies(): Result<List<String>> = withContext(ioDispatcher) {
        runCatching {
            api.getAvailableCurrencies()
        }.recoverCatching {
            Currency.entries.map { it.name }
        }
    }
}