package com.example.dolarapptest.domain.usecase

import com.example.dolarapptest.domain.AMOUNT_SCALE
import com.example.dolarapptest.domain.model.RateType
import com.example.dolarapptest.domain.model.Ticker
import java.math.BigDecimal
import java.math.RoundingMode
import javax.inject.Inject

class ConvertFromBaseCurrencyUseCase @Inject constructor() {

    operator fun invoke(
        amount: BigDecimal,
        ticker: Ticker,
        rateType: RateType = RateType.BID,
        scale: Int = AMOUNT_SCALE,
    ): BigDecimal {
        val tickerRate = if (rateType == RateType.ASK) ticker.ask else ticker.bid
        return amount.multiply(tickerRate)
            .setScale(scale, RoundingMode.HALF_UP)
    }
}
