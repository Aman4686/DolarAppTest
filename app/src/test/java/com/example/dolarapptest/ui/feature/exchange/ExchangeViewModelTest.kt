package com.example.dolarapptest.ui.feature.exchange

import com.example.dolarapptest.R
import com.example.dolarapptest.domain.model.Ticker
import com.example.dolarapptest.domain.usecase.ConvertFromBaseCurrencyUseCase
import com.example.dolarapptest.domain.usecase.ConvertToBaseCurrencyUseCase
import com.example.dolarapptest.domain.usecase.GetCurrenciesUseCase
import com.example.dolarapptest.domain.usecase.GetTickersUseCase
import com.example.dolarapptest.ui.feature.exchange.state.ExchangeUiEffect
import com.example.dolarapptest.ui.feature.exchange.state.ExchangeUiIntent
import com.example.dolarapptest.ui.feature.exchange.state.ExchangeUiState.UiState
import com.example.dolarapptest.ui.feature.exchange.state.FieldPosition
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal
import java.time.ZonedDateTime

@OptIn(ExperimentalCoroutinesApi::class)
class ExchangeViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val getTickersUseCase = mockk<GetTickersUseCase>()
    private val getCurrenciesUseCase = mockk<GetCurrenciesUseCase>()
    private val convertFromBase = ConvertFromBaseCurrencyUseCase()
    private val convertToBase = ConvertToBaseCurrencyUseCase()

    private lateinit var viewModel: ExchangeViewModel

    private val currencies = listOf("MXN", "ARS", "BRL", "COP")
    private val ticker = Ticker(
        from = "USDC",
        to = "MXN",
        ask = BigDecimal("18.50"),
        bid = BigDecimal("18.40"),
        date = ZonedDateTime.now()
    )

    private val arsTickerResponse = Ticker(
        from = "USDC",
        to = "ARS",
        ask = BigDecimal("900.00"),
        bid = BigDecimal("890.00"),
        date = ZonedDateTime.now()
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        coEvery { getCurrenciesUseCase() } returns Result.success(currencies)
        coEvery { getTickersUseCase(any()) } returns Result.success(listOf(ticker))
        viewModel = ExchangeViewModel(getTickersUseCase, getCurrenciesUseCase, convertFromBase, convertToBase)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // loadInitialState

    @Test
    fun `loadInitialState emits Success state with currencies and ticker data`() {
        val state = viewModel.uiState.value.state
        assertTrue(state is UiState.Success)
        state as UiState.Success

        assertEquals(currencies, state.availableCurrencies)
        assertEquals("USDC", state.firstExchangeInputFieldUiState.currency)
        assertEquals("MXN", state.secondExchangeInputFieldUiState.currency)
        assertEquals("1 ${ticker.from.uppercase()} = ${ticker.bid.toPlainString()} ${ticker.to.uppercase()}", state.exchangeRate)
        assertEquals(FieldPosition.TOP, state.baseCurrencyField)
    }

    @Test
    fun `loadInitialState hides overlay loader after load`() {
        assertFalse(viewModel.uiState.value.showOverlayLoader)
    }

    @Test
    fun `loadInitialState emits ShowToast effect when currencies fetch fails`() = runTest {
        coEvery { getCurrenciesUseCase() } returns Result.failure(RuntimeException("error"))
        val errorViewModel = ExchangeViewModel(getTickersUseCase, getCurrenciesUseCase, convertFromBase, convertToBase)

        val effect = errorViewModel.effect.first()

        assertEquals(ExchangeUiEffect.ShowToast.Res(R.string.error_something_went_wrong), effect)
    }

    @Test
    fun `loadInitialState emits ShowToast effect when tickers fetch fails`() = runTest {
        coEvery { getTickersUseCase(any()) } returns Result.failure(RuntimeException("error"))
        val errorViewModel = ExchangeViewModel(getTickersUseCase, getCurrenciesUseCase, convertFromBase, convertToBase)

        val effect = errorViewModel.effect.first()

        assertEquals(ExchangeUiEffect.ShowToast.Res(R.string.error_something_went_wrong), effect)
    }

    // TopAmountChanged intent

    @Test
    fun `TopAmountChanged converts amount using bid rate and updates bottom field`() {
        viewModel.onIntent(ExchangeUiIntent.TopAmountChanged("100"))

        val state = viewModel.uiState.value.state as UiState.Success
        assertEquals("100", state.firstExchangeInputFieldUiState.amount)
        assertEquals("1,840.00", state.secondExchangeInputFieldUiState.amount)
    }

    @Test
    fun `TopAmountChanged clears both fields when amount is empty`() {
        viewModel.onIntent(ExchangeUiIntent.TopAmountChanged("50"))
        viewModel.onIntent(ExchangeUiIntent.TopAmountChanged(""))

        val state = viewModel.uiState.value.state as UiState.Success
        assertEquals("", state.firstExchangeInputFieldUiState.amount)
        assertEquals("", state.secondExchangeInputFieldUiState.amount)
    }

    @Test
    fun `TopAmountChanged strips spaces from amount`() {
        viewModel.onIntent(ExchangeUiIntent.TopAmountChanged("1 0 0"))

        val state = viewModel.uiState.value.state as UiState.Success
        assertEquals("100", state.firstExchangeInputFieldUiState.amount)
    }

    @Test
    fun `TopAmountChanged ignores input exceeding 8 decimal places`() {
        viewModel.onIntent(ExchangeUiIntent.TopAmountChanged("1.000000000"))

        val state = viewModel.uiState.value.state as UiState.Success
        assertEquals("", state.firstExchangeInputFieldUiState.amount)
    }

    // BottomAmountChanged intent

    @Test
    fun `BottomAmountChanged formats typed amount with commas and converts to top field with no decimals`() {
        viewModel.onIntent(ExchangeUiIntent.BottomAmountChanged("1850"))

        val state = viewModel.uiState.value.state as UiState.Success
        // typed BOTTOM field gets comma-formatted
        assertEquals("1,850", state.secondExchangeInputFieldUiState.amount)
        // TOP (USDC) result uses scale=0: 1850 / bid(18.40) = 100.54… → 101
        assertEquals("101", state.firstExchangeInputFieldUiState.amount)
    }

    @Test
    fun `BottomAmountChanged clears both fields when amount is empty`() {
        viewModel.onIntent(ExchangeUiIntent.BottomAmountChanged(""))

        val state = viewModel.uiState.value.state as UiState.Success
        assertEquals("", state.firstExchangeInputFieldUiState.amount)
        assertEquals("", state.secondExchangeInputFieldUiState.amount)
    }

    // Swap intent

    @Test
    fun `Swap swaps field positions`() {
        viewModel.onIntent(ExchangeUiIntent.TopAmountChanged("100"))
        val stateBefore = viewModel.uiState.value.state as UiState.Success

        viewModel.onIntent(ExchangeUiIntent.Swap)

        val stateAfter = viewModel.uiState.value.state as UiState.Success
        assertEquals(stateBefore.secondExchangeInputFieldUiState.currency, stateAfter.firstExchangeInputFieldUiState.currency)
        assertEquals(stateBefore.firstExchangeInputFieldUiState.currency, stateAfter.secondExchangeInputFieldUiState.currency)
    }

    @Test
    fun `Swap toggles base currency field from TOP to BOTTOM`() {
        viewModel.onIntent(ExchangeUiIntent.Swap)

        val state = viewModel.uiState.value.state as UiState.Success
        assertEquals(FieldPosition.BOTTOM, state.baseCurrencyField)
    }

    @Test
    fun `Swap toggles base currency field back to TOP on second swap`() {
        viewModel.onIntent(ExchangeUiIntent.Swap)
        viewModel.onIntent(ExchangeUiIntent.Swap)

        val state = viewModel.uiState.value.state as UiState.Success
        assertEquals(FieldPosition.TOP, state.baseCurrencyField)
    }

    @Test
    fun `Swap exchange rate switches from bid to ask`() {
        viewModel.onIntent(ExchangeUiIntent.Swap)

        val state = viewModel.uiState.value.state as UiState.Success
        assertEquals("1 ${ticker.from.uppercase()} = ${ticker.ask.toPlainString()} ${ticker.to.uppercase()}", state.exchangeRate)
    }

    @Test
    fun `Swap preserves base amount after swap`() {
        viewModel.onIntent(ExchangeUiIntent.TopAmountChanged("100"))
        viewModel.onIntent(ExchangeUiIntent.Swap)

        val state = viewModel.uiState.value.state as UiState.Success
        assertEquals("100", state.secondExchangeInputFieldUiState.amount)
    }

    // ShowBottomSheet / HideBottomSheet intents

    @Test
    fun `ShowBottomSheet sets isShowBottomSheet to true`() {
        viewModel.onIntent(ExchangeUiIntent.ShowBottomSheet)

        val state = viewModel.uiState.value.state as UiState.Success
        assertTrue(state.isShowBottomSheet)
    }

    @Test
    fun `HideBottomSheet sets isShowBottomSheet to false`() {
        viewModel.onIntent(ExchangeUiIntent.ShowBottomSheet)
        viewModel.onIntent(ExchangeUiIntent.HideBottomSheet)

        val state = viewModel.uiState.value.state as UiState.Success
        assertFalse(state.isShowBottomSheet)
    }

    // CurrencySelected intent

    @Test
    fun `CurrencySelected updates bottom field currency`() {
        coEvery { getTickersUseCase(listOf("ARS")) } returns Result.success(listOf(arsTickerResponse))
        viewModel.onIntent(ExchangeUiIntent.CurrencySelected("ARS"))

        val state = viewModel.uiState.value.state as UiState.Success
        assertEquals("ARS", state.secondExchangeInputFieldUiState.currency)
    }

    @Test
    fun `CurrencySelected fetches new ticker for selected currency`() {
        coEvery { getTickersUseCase(listOf("ARS")) } returns Result.success(listOf(arsTickerResponse))
        viewModel.onIntent(ExchangeUiIntent.CurrencySelected("ARS"))

        coVerify { getTickersUseCase(listOf("ARS")) }
    }

    @Test
    fun `CurrencySelected updates exchange rate for new currency`() {
        coEvery { getTickersUseCase(listOf("ARS")) } returns Result.success(listOf(arsTickerResponse))
        viewModel.onIntent(ExchangeUiIntent.CurrencySelected("ARS"))

        val state = viewModel.uiState.value.state as UiState.Success
        assertEquals("1 ${arsTickerResponse.from.uppercase()} = ${arsTickerResponse.bid.toPlainString()} ${arsTickerResponse.to.uppercase()}", state.exchangeRate)
    }

    @Test
    fun `CurrencySelected hides overlay loader after currency change`() {
        coEvery { getTickersUseCase(listOf("ARS")) } returns Result.success(listOf(arsTickerResponse))
        viewModel.onIntent(ExchangeUiIntent.CurrencySelected("ARS"))

        assertFalse(viewModel.uiState.value.showOverlayLoader)
    }

    @Test
    fun `CurrencySelected closes bottom sheet after currency selection`() {
        coEvery { getTickersUseCase(listOf("ARS")) } returns Result.success(listOf(arsTickerResponse))
        viewModel.onIntent(ExchangeUiIntent.ShowBottomSheet)
        viewModel.onIntent(ExchangeUiIntent.CurrencySelected("ARS"))

        val state = viewModel.uiState.value.state as UiState.Success
        assertFalse(state.isShowBottomSheet)
    }
}
