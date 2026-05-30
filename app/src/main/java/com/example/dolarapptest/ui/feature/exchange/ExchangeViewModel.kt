package com.example.dolarapptest.ui.feature.exchange

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dolarapptest.domain.model.Ticker
import com.example.dolarapptest.domain.usecase.ConvertFromBaseCurrencyUseCase
import com.example.dolarapptest.domain.usecase.ConvertToBaseCurrencyUseCase
import com.example.dolarapptest.domain.usecase.GetCurrenciesUseCase
import com.example.dolarapptest.domain.usecase.GetTickersUseCase
import com.example.dolarapptest.ui.feature.exchange.state.ExchangeUiIntent
import com.example.dolarapptest.ui.feature.exchange.state.ExchangeUiState
import com.example.dolarapptest.ui.feature.exchange.state.ExchangeUiState.ExchangeInputFieldUiState
import com.example.dolarapptest.ui.feature.exchange.state.ExchangeUiState.UiState
import com.example.dolarapptest.ui.feature.exchange.state.FieldPosition
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ExchangeViewModel @Inject constructor(
    private val getTickersUseCase: GetTickersUseCase,
    private val getCurrenciesUseCase: GetCurrenciesUseCase,
    private val convertFromBaseCurrencyUseCase: ConvertFromBaseCurrencyUseCase,
    private val convertToBaseCurrencyUseCase: ConvertToBaseCurrencyUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ExchangeUiState(state = UiState.Loading))
    val uiState: StateFlow<ExchangeUiState> = _uiState.asStateFlow()

    private var currentTicker: Ticker? = null

    init {
        loadInitialState()
    }

    fun loadInitialState() {
        viewModelScope.launch {
            val currenciesList = getCurrenciesUseCase().getOrElse {
                _uiState.value = ExchangeUiState(state = UiState.Error())
                return@launch
            }
            val firstTicker = getTickersUseCase(currenciesList).getOrElse {
                _uiState.value = ExchangeUiState(state = UiState.Error())
                return@launch
            }.firstOrNull() ?: run {
                _uiState.value = ExchangeUiState(state = UiState.Error())
                return@launch
            }
            currentTicker = firstTicker
            val initialSuccess = UiState.Success(
                availableCurrencies = currenciesList.toImmutableList(),
                topExchangeInputFieldUiState = ExchangeInputFieldUiState(
                    currency = firstTicker.from.uppercase()
                ),
                bottomExchangeInputFieldUiState = ExchangeInputFieldUiState(
                    currency = firstTicker.to.uppercase()
                ),
                baseCurrencyField = FieldPosition.TOP,
                activeInputField = FieldPosition.TOP,
            )
            _uiState.value = ExchangeUiState(state = initialSuccess.copy(exchangeRate = getCurrentRate(initialSuccess)))
        }
    }

    fun onIntent(intent: ExchangeUiIntent) {
        when (intent) {
            is ExchangeUiIntent.TopAmountChanged -> onTopAmountChanged(intent.amount)
            is ExchangeUiIntent.BottomAmountChanged -> onBottomAmountChanged(intent.amount)
            ExchangeUiIntent.Swap -> onSwap()
            ExchangeUiIntent.ShowBottomSheet -> onShowBottomSheet()
            ExchangeUiIntent.HideBottomSheet -> onHideBottomSheet()
            is ExchangeUiIntent.CurrencySelected -> onCurrencySelected(intent.currency)
        }
    }

    private fun onTopAmountChanged(amount: String) {
        updateSuccessUiState { state ->
            val updated = state.copy(
                activeInputField = FieldPosition.TOP,
                topExchangeInputFieldUiState = state.topExchangeInputFieldUiState.copy(amount = amount),
            )
            updated.copy(
                bottomExchangeInputFieldUiState = state.bottomExchangeInputFieldUiState.copy(
                    amount = convertAmount(amount, updated)
                )
            )
        }
    }

    private fun onBottomAmountChanged(amount: String) {
        updateSuccessUiState { state ->
            val updated = state.copy(
                activeInputField = FieldPosition.BOTTOM,
                bottomExchangeInputFieldUiState = state.bottomExchangeInputFieldUiState.copy(amount = amount),
            )
            updated.copy(
                topExchangeInputFieldUiState = state.topExchangeInputFieldUiState.copy(
                    amount = convertAmount(amount, updated)
                )
            )
        }
    }

    private fun onSwap() {
        updateSuccessUiState { state ->
            val newBaseCurrencyField = if (state.baseCurrencyField == FieldPosition.TOP) FieldPosition.BOTTOM else FieldPosition.TOP
            val swapped = state.copy(
                topExchangeInputFieldUiState = state.bottomExchangeInputFieldUiState,
                bottomExchangeInputFieldUiState = state.topExchangeInputFieldUiState,
                baseCurrencyField = newBaseCurrencyField,
            )
            swapped.copy(exchangeRate = getCurrentRate(swapped))
        }
    }

    private fun onShowBottomSheet() {
        updateSuccessUiState { it.copy(isShowBottomSheet = true) }
    }

    private fun onHideBottomSheet() {
        updateSuccessUiState { it.copy(isShowBottomSheet = false) }
    }

    private fun onCurrencySelected(currency: String) {
        updateSuccessUiState { state ->
            if (state.baseCurrencyField == FieldPosition.BOTTOM)
                state.copy(
                    topExchangeInputFieldUiState = state.topExchangeInputFieldUiState.copy(currency = currency),
                    isShowBottomSheet = false
                )
            else
                state.copy(
                    bottomExchangeInputFieldUiState = state.bottomExchangeInputFieldUiState.copy(currency = currency),
                    isShowBottomSheet = false
                )
        }
    }

    private fun getCurrentRate(state: UiState.Success): String {
        val ticker = currentTicker ?: return ""
        return if (state.activeInputField == state.baseCurrencyField)
            ticker.bid.toPlainString()
        else
            ticker.ask.toPlainString()
    }

    private fun convertAmount(amount: String, state: UiState.Success): String {
        val ticker = currentTicker ?: return ""
        val bigDecimal = amount.toBigDecimalOrNull() ?: return ""
        return if (state.activeInputField == state.baseCurrencyField)
            convertFromBaseCurrencyUseCase(bigDecimal, ticker).toPlainString()
        else
            convertToBaseCurrencyUseCase(bigDecimal, ticker).toPlainString()
    }

    private fun updateSuccessUiState(block: (UiState.Success) -> UiState.Success) {
        _uiState.update { uiState ->
            val success = uiState.state as? UiState.Success ?: return@update uiState
            uiState.copy(state = block(success))
        }
    }
}
