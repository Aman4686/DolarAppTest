package com.example.dolarapptest.ui.feature.exchange

import android.util.Log
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
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private val TAG: String = "ExchangeViewModel"

@HiltViewModel
class ExchangeViewModel @Inject constructor(
    private val getTickersUseCase: GetTickersUseCase,
    private val getCurrenciesUseCase: GetCurrenciesUseCase,
    private val convertFromBaseCurrencyUseCase: ConvertFromBaseCurrencyUseCase,
    private val convertToBaseCurrencyUseCase: ConvertToBaseCurrencyUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ExchangeUiState(state = UiState.Loading))
    val uiState: StateFlow<ExchangeUiState> = _uiState.asStateFlow()

    private val exceptionHandler = CoroutineExceptionHandler { _, _ ->
        _uiState.value = ExchangeUiState(state = UiState.Error())
    }

    private var currentTicker: Ticker? = null

    init {
        loadInitialState()
    }

    fun loadInitialState() {
        viewModelScope.launch(exceptionHandler) {
            val currenciesList = getCurrenciesUseCase().getOrThrow()
            val firstTicker = getTickersUseCase(currenciesList).getOrThrow().first()

            currentTicker = firstTicker
            val initialSuccess = UiState.Success(
                availableCurrencies = currenciesList.toImmutableList(),
                firstExchangeInputFieldUiState = ExchangeInputFieldUiState(
                    currency = firstTicker.from.uppercase()
                ),
                secondExchangeInputFieldUiState = ExchangeInputFieldUiState(
                    currency = firstTicker.to.uppercase()
                ),
                baseCurrencyField = FieldPosition.TOP,
                activeInputField = FieldPosition.TOP,
            )
            _uiState.value = ExchangeUiState(
                state = initialSuccess.copy(
                    exchangeRate = getCurrentRate(initialSuccess)
                )
            )
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
        val sanitized = amount.replace(" ", "")
        if (exceedsMaxScale(sanitized)) return
        updateSuccessUiState { state ->
            val converted =
                convertAmount(sanitized, isFromBase = state.baseCurrencyField == FieldPosition.TOP)
                    ?: return@updateSuccessUiState state.copy(activeInputField = FieldPosition.TOP)
            state.copy(
                activeInputField = FieldPosition.TOP,
                firstExchangeInputFieldUiState = state.firstExchangeInputFieldUiState.copy(amount = sanitized),
                secondExchangeInputFieldUiState = state.secondExchangeInputFieldUiState.copy(amount = converted),
            )
        }
    }

    private fun onBottomAmountChanged(amount: String) {
        val sanitized = amount.replace(" ", "")
        if (exceedsMaxScale(sanitized)) return
        updateSuccessUiState { state ->
            val converted = convertAmount(
                sanitized,
                isFromBase = state.baseCurrencyField == FieldPosition.BOTTOM
            ) ?: return@updateSuccessUiState state.copy(activeInputField = FieldPosition.BOTTOM)

            state.copy(
                activeInputField = FieldPosition.BOTTOM,
                secondExchangeInputFieldUiState = state.secondExchangeInputFieldUiState.copy(amount = sanitized),
                firstExchangeInputFieldUiState = state.firstExchangeInputFieldUiState.copy(amount = converted),
            )
        }
    }

    private fun onSwap() {
        updateSuccessUiState { state ->
            val newBaseCurrencyField =
                if (state.baseCurrencyField == FieldPosition.TOP) FieldPosition.BOTTOM else FieldPosition.TOP
            val swapped = state.copy(
                firstExchangeInputFieldUiState = state.secondExchangeInputFieldUiState,
                secondExchangeInputFieldUiState = state.firstExchangeInputFieldUiState,
                baseCurrencyField = newBaseCurrencyField,
            )
            recalculateAmounts(  swapped.copy(exchangeRate = getCurrentRate(swapped)))
        }
    }

    private fun onShowBottomSheet() {
        updateSuccessUiState { it.copy(isShowBottomSheet = true) }
    }

    private fun onHideBottomSheet() {
        updateSuccessUiState { it.copy(isShowBottomSheet = false) }
    }

    private fun onCurrencySelected(currency: String) {
        viewModelScope.launch(exceptionHandler) {
            _uiState.update {
                it.copy(
                    showOverlayLoader = true,
                    state = updateCurrencyInState(it.state, currency)
                )
            }

            val ticker = getTickersUseCase(listOf(currency)).getOrNull()?.firstOrNull()

            if (ticker != null) {
                currentTicker = ticker
                updateSuccessUiState { state ->
                    recalculateAmounts(state).copy(exchangeRate = getCurrentRate(state))
                }
            } else {
                // handle error
            }

            _uiState.update { it.copy(showOverlayLoader = false) }
        }
    }

    private fun updateCurrencyInState(state: UiState, currency: String): UiState {
        val success = state as? UiState.Success ?: return state
        return if (success.baseCurrencyField == FieldPosition.BOTTOM)
            success.copy(
                firstExchangeInputFieldUiState = success.firstExchangeInputFieldUiState.copy(
                    currency = currency
                ),
                isShowBottomSheet = false
            )
        else
            success.copy(
                secondExchangeInputFieldUiState = success.secondExchangeInputFieldUiState.copy(
                    currency = currency
                ),
                isShowBottomSheet = false
            )
    }

    private fun recalculateAmounts(state: UiState.Success): UiState.Success {
        Log.d(TAG, "recalculateAmounts: ${state.baseCurrencyField}")
        return if (state.baseCurrencyField == FieldPosition.TOP)
            state.copy(
                secondExchangeInputFieldUiState = state.secondExchangeInputFieldUiState.copy(
                    amount = convertAmount(
                        state.firstExchangeInputFieldUiState.amount,
                        isFromBase = true
                    ) ?: state.secondExchangeInputFieldUiState.amount
                )
            )
        else
            state.copy(
                firstExchangeInputFieldUiState = state.firstExchangeInputFieldUiState.copy(
                    amount = convertAmount(
                        state.secondExchangeInputFieldUiState.amount,
                        isFromBase = true
                    ) ?: state.firstExchangeInputFieldUiState.amount
                )
            )
    }

    private fun getCurrentRate(state: UiState.Success): String {
        val ticker = currentTicker ?: return ""
        return if (state.activeInputField == state.baseCurrencyField)
            ticker.bid.toPlainString()
        else
            ticker.ask.toPlainString()
    }

    private fun exceedsMaxScale(amount: String, maxScale: Int = 8): Boolean {
        val dotIndex = amount.indexOf('.')
        return dotIndex != -1 && amount.length - dotIndex - 1 > maxScale
    }

    private fun convertAmount(amount: String, isFromBase: Boolean): String? {
        Log.d(TAG, "convertAmount: ${amount} isFromBase ${isFromBase}")
        val ticker = currentTicker ?: return null
        val bigDecimal = amount.toBigDecimalOrNull() ?: return null
        return if (isFromBase)
            convertFromBaseCurrencyUseCase(bigDecimal, ticker).toPlainString()
        else
            convertToBaseCurrencyUseCase(bigDecimal, ticker).toPlainString()
    }

    private inline fun updateSuccessUiState(block: (UiState.Success) -> UiState.Success) {
        _uiState.update { uiState ->
            val success = uiState.state as? UiState.Success ?: return@update uiState
            uiState.copy(state = block(success))
        }
    }
}
