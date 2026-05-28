package com.example.dolarapptest.domain.usecase

import com.example.dolarapptest.domain.model.Ticker
import java.math.BigDecimal
import java.math.RoundingMode
import javax.inject.Inject


// Do not add Dispatcher.DEFAULT as its not a heavy logic
class ConvertCurrencyUseCase @Inject constructor() {

    operator fun invoke(amount: BigDecimal, fromCurrency: String, ticker: Ticker): BigDecimal =
        if (fromCurrency.equals(ticker.from, ignoreCase = true)) {
            (amount * ticker.ask).setScale(2, RoundingMode.HALF_UP)
        } else {
            (amount / ticker.bid).setScale(2, RoundingMode.HALF_UP)
        }
}
