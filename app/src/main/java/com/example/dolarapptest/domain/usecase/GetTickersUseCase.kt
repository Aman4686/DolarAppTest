package com.example.dolarapptest.domain.usecase

import com.example.dolarapptest.domain.model.Ticker
import com.example.dolarapptest.domain.repository.TickersRepository
import javax.inject.Inject

class GetTickersUseCase @Inject constructor(
    private val repository: TickersRepository
) {
    suspend operator fun invoke(currencies: List<String>): Result<List<Ticker>> =
        repository.getTickers(currencies)
}
