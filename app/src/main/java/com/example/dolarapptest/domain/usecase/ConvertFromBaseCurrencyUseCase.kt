package com.example.dolarapptest.domain.usecase

import com.example.dolarapptest.domain.model.Ticker
import java.math.BigDecimal
import java.math.RoundingMode
import javax.inject.Inject

class ConvertFromBaseCurrencyUseCase @Inject constructor() {

    operator fun invoke(amount: BigDecimal, ticker: Ticker): BigDecimal =
        amount.multiply(ticker.bid).setScale(8, RoundingMode.HALF_UP)
}
