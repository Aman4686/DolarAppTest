package com.example.dolarapptest.domain.usecase

import com.example.dolarapptest.domain.AMOUNT_SCALE
import com.example.dolarapptest.domain.model.RateType
import com.example.dolarapptest.domain.model.Ticker
import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.ZonedDateTime

class ConvertToBaseCurrencyUseCaseTest {

    private val useCase = ConvertToBaseCurrencyUseCase()

    private val ticker = Ticker(
        from = "USDC",
        to = "MXN",
        ask = BigDecimal("18.50"),
        bid = BigDecimal("18.40"),
        date = ZonedDateTime.now()
    )

    @Test
    fun `default rate type uses ASK`() {
        val result = useCase(BigDecimal("185.00"), ticker)
        assertEquals(BigDecimal("185.00").divide(ticker.ask, AMOUNT_SCALE, RoundingMode.HALF_UP), result)
    }

    @Test
    fun `explicit ASK divides by ask rate`() {
        val result = useCase(BigDecimal("185.00"), ticker, RateType.ASK)
        assertEquals(BigDecimal("185.00").divide(ticker.ask, AMOUNT_SCALE, RoundingMode.HALF_UP), result)
    }

    @Test
    fun `BID divides by bid rate`() {
        val result = useCase(BigDecimal("184.00"), ticker, RateType.BID)
        assertEquals(BigDecimal("184.00").divide(ticker.bid, AMOUNT_SCALE, RoundingMode.HALF_UP), result)
    }

    @Test
    fun `custom scale overrides AMOUNT_SCALE`() {
        val result = useCase(BigDecimal("185.00"), ticker, RateType.ASK, scale = 0)
        assertEquals(BigDecimal("185.00").divide(ticker.ask, 0, RoundingMode.HALF_UP), result)
    }

    @Test
    fun `ASK and BID produce different results`() {
        val amount = BigDecimal("100.00")
        val askResult = useCase(amount, ticker, RateType.ASK)
        val bidResult = useCase(amount, ticker, RateType.BID)
        assert(askResult != bidResult)
    }
}
