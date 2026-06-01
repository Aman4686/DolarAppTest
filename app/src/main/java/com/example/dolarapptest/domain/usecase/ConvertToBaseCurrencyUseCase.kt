package com.example.dolarapptest.domain.usecase

import com.example.dolarapptest.domain.AMOUNT_SCALE
import com.example.dolarapptest.domain.model.RateType
import com.example.dolarapptest.domain.model.Ticker
import java.math.BigDecimal
import java.math.RoundingMode
import javax.inject.Inject

class ConvertToBaseCurrencyUseCase @Inject constructor() {

    operator fun invoke(amount: BigDecimal, ticker: Ticker, rateType: RateType = RateType.ASK): BigDecimal =
        amount.divide(if (rateType == RateType.BID) ticker.bid else ticker.ask, AMOUNT_SCALE, RoundingMode.HALF_UP)
}
