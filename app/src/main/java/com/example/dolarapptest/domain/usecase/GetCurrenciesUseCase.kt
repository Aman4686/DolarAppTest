package com.example.dolarapptest.domain.usecase

import com.example.dolarapptest.domain.repository.TickersRepository
import javax.inject.Inject

class GetCurrenciesUseCase @Inject constructor(
    private val repository: TickersRepository
) {
    suspend operator fun invoke(): Result<List<String>> =
        repository.getAvailableCurrencies()
}
