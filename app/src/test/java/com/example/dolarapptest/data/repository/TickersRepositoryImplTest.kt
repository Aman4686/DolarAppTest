package com.example.dolarapptest.data.repository

import com.example.dolarapptest.data.api.TickersApi
import com.example.dolarapptest.data.model.TickerResponse
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

@OptIn(ExperimentalCoroutinesApi::class)
class TickersRepositoryImplTest {

    private val api = mockk<TickersApi>()
    private val dispatcher = UnconfinedTestDispatcher()
    private lateinit var repository: TickersRepositoryImpl

    private val tickerResponse = TickerResponse(
        ask = BigDecimal("18.50"),
        bid = BigDecimal("18.40"),
        book = "usdc_mxn",
        date = "2024-01-15T10:30:00"
    )

    @Before
    fun setUp() {
        repository = TickersRepositoryImpl(api, dispatcher)
    }

    @Test
    fun `getTickers returns mapped domain tickers on API success`() = runTest {
        coEvery { api.getTickers("MXN") } returns listOf(tickerResponse)

        val result = repository.getTickers(listOf("MXN"))

        assertTrue(result.isSuccess)
        val tickers = result.getOrThrow()
        assertEquals(1, tickers.size)
        assertEquals("USDC", tickers[0].from)
        assertEquals("MXN", tickers[0].to)
        assertEquals(BigDecimal("18.50"), tickers[0].ask)
        assertEquals(BigDecimal("18.40"), tickers[0].bid)
    }

    @Test
    fun `getTickers joins multiple currencies with comma`() = runTest {
        coEvery { api.getTickers("MXN,ARS") } returns listOf(tickerResponse)

        repository.getTickers(listOf("MXN", "ARS"))

        coVerify { api.getTickers("MXN,ARS") }
    }

    @Test
    fun `getTickers returns failure when API throws`() = runTest {
        coEvery { api.getTickers(any()) } throws RuntimeException("Network error")

        val result = repository.getTickers(listOf("MXN"))

        assertTrue(result.isFailure)
        assertEquals("Network error", result.exceptionOrNull()?.message)
    }

    @Test
    fun `getAvailableCurrencies returns currencies from API on success`() = runTest {
        coEvery { api.getAvailableCurrencies() } returns listOf("MXN", "ARS", "BRL", "COP")

        val result = repository.getAvailableCurrencies()

        assertTrue(result.isSuccess)
        assertEquals(listOf("MXN", "ARS", "BRL", "COP"), result.getOrThrow())
    }

    @Test
    fun `getAvailableCurrencies falls back to hardcoded list when API throws`() = runTest {
        coEvery { api.getAvailableCurrencies() } throws RuntimeException("404 Not Found")

        val result = repository.getAvailableCurrencies()

        assertTrue(result.isSuccess)
        assertEquals(listOf("MXN", "ARS", "BRL", "COP"), result.getOrThrow())
    }

    @Test
    fun `getAvailableCurrencies returns success even when API endpoint does not exist`() = runTest {
        coEvery { api.getAvailableCurrencies() } throws Exception("Endpoint not found")

        val result = repository.getAvailableCurrencies()

        assertTrue(result.isSuccess)
    }
}