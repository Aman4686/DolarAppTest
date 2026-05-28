package com.example.dolarapptest.ui.feature.exchange

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dolarapptest.domain.model.Ticker
import com.example.dolarapptest.domain.usecase.ConvertCurrencyUseCase
import com.example.dolarapptest.domain.usecase.GetCurrenciesUseCase
import com.example.dolarapptest.domain.usecase.GetTickersUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.math.BigDecimal
import javax.inject.Inject

@HiltViewModel
class ExchangeViewModel @Inject constructor(
    private val getTickersUseCase: GetTickersUseCase,
    private val getCurrenciesUseCase: GetCurrenciesUseCase,
    private val convertCurrencyUseCase: ConvertCurrencyUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExchangeUiState(isLoading = true))
    val uiState: StateFlow<ExchangeUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun onIntent(intent: ExchangeIntent) {
        val current = _uiState.value
        if (current.isLoading || current.ticker == null) {
            if (intent is ExchangeIntent.RetryClicked) load()
            return
        }
        when (intent) {
            is ExchangeIntent.AmountChanged -> handleAmountChanged(current, intent.field, intent.amount)
            is ExchangeIntent.FieldFocused -> _uiState.value = current.copy(activeField = intent.field)
            is ExchangeIntent.SwapClicked -> handleSwap(current)
            is ExchangeIntent.BottomCurrencyTapped -> _uiState.value = current.copy(showBottomSheet = true)
            is ExchangeIntent.CurrencySelected -> handleCurrencySelected(current, intent.currency)
            is ExchangeIntent.BottomSheetDismissed -> _uiState.value = current.copy(showBottomSheet = false)
            is ExchangeIntent.RetryClicked -> load()
        }
    }

    private fun load() {
        viewModelScope.launch {
            _uiState.value = ExchangeUiState(isLoading = true)
            val currencies = getCurrenciesUseCase()
            val defaultCurrency = currencies.firstOrNull() ?: "MXN"
            getTickersUseCase(listOf(defaultCurrency))
                .onSuccess { tickers ->
                    val ticker = tickers.firstOrNull()
                    if (ticker == null) {
                        _uiState.value = ExchangeUiState(error = "No rate available")
                        return@onSuccess
                    }
                    _uiState.value = ExchangeUiState(
                        topCurrency = ticker.from,
                        bottomCurrency = ticker.to,
                        topAmount = "",
                        bottomAmount = "",
                        activeField = ActiveField.TOP,
                        ticker = ticker,
                        availableCurrencies = currencies,
                        showBottomSheet = false,
                        rateLabel = buildRateLabel(ticker)
                    )
                }
                .onFailure { _uiState.value = ExchangeUiState(error = it.message ?: "Failed to load rates") }
        }
    }

    private fun handleAmountChanged(state: ExchangeUiState, field: ActiveField, input: String) {
        val ticker = state.ticker ?: return
        val amount = input.toBigDecimalOrNull() ?: BigDecimal.ZERO
        val (topAmount, bottomAmount) = when (field) {
            ActiveField.TOP -> {
                val converted = if (amount > BigDecimal.ZERO)
                    convertCurrencyUseCase(amount, state.topCurrency, ticker).toPlainString()
                else ""
                input to converted
            }
            ActiveField.BOTTOM -> {
                val converted = if (amount > BigDecimal.ZERO)
                    convertCurrencyUseCase(amount, state.bottomCurrency, ticker).toPlainString()
                else ""
                converted to input
            }
        }
        _uiState.value = state.copy(topAmount = topAmount, bottomAmount = bottomAmount)
    }

    private fun handleSwap(state: ExchangeUiState) {
        _uiState.value = state.copy(
            topCurrency = state.bottomCurrency,
            bottomCurrency = state.topCurrency,
            topAmount = state.bottomAmount,
            bottomAmount = state.topAmount,
            activeField = when (state.activeField) {
                ActiveField.TOP -> ActiveField.BOTTOM
                ActiveField.BOTTOM -> ActiveField.TOP
            }
        )
    }

    private fun handleCurrencySelected(state: ExchangeUiState, currency: String) {
        viewModelScope.launch {
            getTickersUseCase(listOf(currency))
                .onSuccess { tickers ->
                    val ticker = tickers.firstOrNull() ?: return@onSuccess
                    _uiState.value = state.copy(
                        bottomCurrency = ticker.to,
                        topCurrency = ticker.from,
                        topAmount = "",
                        bottomAmount = "",
                        ticker = ticker,
                        showBottomSheet = false,
                        rateLabel = buildRateLabel(ticker)
                    )
                }
                .onFailure {
                    _uiState.value = state.copy(showBottomSheet = false)
                }
        }
    }

    private fun buildRateLabel(ticker: Ticker): String =
        "1 ${ticker.from} = ${ticker.ask.toPlainString()} ${ticker.to}"
}