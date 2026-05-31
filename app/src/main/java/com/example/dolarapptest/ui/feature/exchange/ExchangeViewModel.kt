package com.example.dolarapptest.ui.feature.exchange

import androidx.lifecycle.ViewModel
import com.example.dolarapptest.R
import androidx.lifecycle.viewModelScope
import com.example.dolarapptest.domain.model.Ticker
import com.example.dolarapptest.domain.usecase.ConvertFromBaseCurrencyUseCase
import com.example.dolarapptest.domain.usecase.ConvertToBaseCurrencyUseCase
import com.example.dolarapptest.domain.usecase.GetCurrenciesUseCase
import com.example.dolarapptest.domain.usecase.GetTickersUseCase
import com.example.dolarapptest.ui.feature.exchange.state.ExchangeUiEffect
import com.example.dolarapptest.ui.feature.exchange.state.ExchangeUiIntent
import com.example.dolarapptest.ui.feature.exchange.state.ExchangeUiState
import com.example.dolarapptest.ui.feature.exchange.state.ExchangeUiState.ExchangeInputFieldUiState
import com.example.dolarapptest.ui.feature.exchange.state.ExchangeUiState.UiState
import com.example.dolarapptest.domain.model.RateType
import com.example.dolarapptest.ui.feature.exchange.state.FieldPosition
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
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


    private val _uiState = MutableStateFlow(ExchangeUiState(
        showOverlayLoader = true,
        state = UiState.Success.empty()
    ))
    val uiState: StateFlow<ExchangeUiState> = _uiState.asStateFlow()

    private val _effect = Channel<ExchangeUiEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        showErrorToast(R.string.error_something_went_wrong)
    }

    private var currentTicker: Ticker? = null
    private var rateType: RateType = RateType.BID

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
                exchangeRate = getCurrentRate(RateType.BID)
            )

            _uiState.value = ExchangeUiState(state = initialSuccess)
        }
    }

    fun onIntent(intent: ExchangeUiIntent) {
        when (intent) {
            is ExchangeUiIntent.TopAmountChanged -> onAmountChanged(intent.amount, FieldPosition.TOP)
            is ExchangeUiIntent.BottomAmountChanged -> onAmountChanged(intent.amount, FieldPosition.BOTTOM)
            ExchangeUiIntent.Swap -> onSwap()
            ExchangeUiIntent.ShowBottomSheet -> onShowBottomSheet()
            ExchangeUiIntent.HideBottomSheet -> onHideBottomSheet()
            is ExchangeUiIntent.CurrencySelected -> onCurrencySelected(intent.currency)
        }
    }

    private fun onAmountChanged(amount: String, field: FieldPosition) {
        val clearedAmount = amount.replace(" ", "")
        if (exceedsMaxScale(clearedAmount)) return

        updateSuccessUiState { success ->
            if (clearedAmount.isEmpty()) {
                return@updateSuccessUiState success.copy(
                    firstExchangeInputFieldUiState = success.firstExchangeInputFieldUiState.copy(amount = ""),
                    secondExchangeInputFieldUiState = success.secondExchangeInputFieldUiState.copy(amount = ""),
                )
            }
            val isFromBase = success.baseCurrencyField == field
            val converted = convertAmount(clearedAmount, isFromBase) ?: return
            rateType = if (isFromBase) RateType.BID else RateType.ASK
            if (field == FieldPosition.TOP) {
                success.copy(
                    firstExchangeInputFieldUiState = success.firstExchangeInputFieldUiState.copy(amount = clearedAmount),
                    secondExchangeInputFieldUiState = success.secondExchangeInputFieldUiState.copy(amount = converted),
                )
            } else {
                success.copy(
                    firstExchangeInputFieldUiState = success.firstExchangeInputFieldUiState.copy(amount = converted),
                    secondExchangeInputFieldUiState = success.secondExchangeInputFieldUiState.copy(amount = clearedAmount),
                )
            }
        }
    }

    private fun onSwap() {
        updateSuccessUiState { success ->
            val newBaseCurrencyField = if (success.baseCurrencyField == FieldPosition.TOP) FieldPosition.BOTTOM else FieldPosition.TOP
            rateType = if (rateType == RateType.BID) RateType.ASK else RateType.BID
            val swapped = success.copy(
                firstExchangeInputFieldUiState = success.secondExchangeInputFieldUiState,
                secondExchangeInputFieldUiState = success.firstExchangeInputFieldUiState,
                baseCurrencyField = newBaseCurrencyField,
                exchangeRate = getCurrentRate(rateType),
            )
            recalculateAmounts(swapped, rateType)
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
            _uiState.update { it.copy(showOverlayLoader = true) }
            updateSuccessUiState { success ->
                if (success.baseCurrencyField == FieldPosition.BOTTOM)
                    success.copy(
                        firstExchangeInputFieldUiState = success.firstExchangeInputFieldUiState.copy(currency = currency),
                        isShowBottomSheet = false
                    )
                else
                    success.copy(
                        secondExchangeInputFieldUiState = success.secondExchangeInputFieldUiState.copy(currency = currency),
                        isShowBottomSheet = false
                    )
            }

            val ticker = getTickersUseCase(listOf(currency)).getOrNull()?.firstOrNull()

            if (ticker != null) {
                currentTicker = ticker
                updateSuccessUiState { success ->
                    recalculateAmounts(success, rateType).copy(exchangeRate = getCurrentRate(rateType))
                }
            } else {
                // handle error
            }

            _uiState.update { it.copy(showOverlayLoader = false) }
        }
    }

    private fun recalculateAmounts(state: UiState.Success, rateType: RateType): UiState.Success {
        return if (state.baseCurrencyField == FieldPosition.TOP) {
            val topAmount = convertAmount(
                state.firstExchangeInputFieldUiState.amount,
                isFromBase = true,
                rateType = rateType
            ) ?: state.secondExchangeInputFieldUiState.amount

            state.copy(
                secondExchangeInputFieldUiState = state.secondExchangeInputFieldUiState.copy(
                    amount = topAmount
                )
            )
        }else {
            val bottomAmount = convertAmount(
                state.secondExchangeInputFieldUiState.amount,
                isFromBase = true,
                rateType = rateType
            )
                ?: state.firstExchangeInputFieldUiState.amount

            state.copy(
                firstExchangeInputFieldUiState = state.firstExchangeInputFieldUiState.copy(
                    amount = bottomAmount
                )
            )
        }
    }

    private fun getCurrentRate(rateType: RateType): String {
        val ticker = currentTicker ?: return ""
        return if (rateType == RateType.ASK) ticker.ask.toPlainString() else ticker.bid.toPlainString()
    }

    private fun exceedsMaxScale(amount: String, maxScale: Int = 8): Boolean {
        val dotIndex = amount.indexOf('.')
        return dotIndex != -1 && amount.length - dotIndex - 1 > maxScale
    }

    private fun convertAmount(amount: String, isFromBase: Boolean, rateType: RateType = RateType.BID): String? {
        val ticker = currentTicker ?: return null
        val bigDecimal = amount.toBigDecimalOrNull() ?: return null
        return if (isFromBase)
            convertFromBaseCurrencyUseCase(bigDecimal, ticker, rateType).toPlainString()
        else
            convertToBaseCurrencyUseCase(bigDecimal, ticker).toPlainString()
    }

    private fun showErrorToast(@androidx.annotation.StringRes messageRes: Int) {
        _effect.trySend(ExchangeUiEffect.ShowToast(messageRes))
        _uiState.update {
            it.copy(showOverlayLoader = false)
        }
    }

    private inline fun updateSuccessUiState(block: (UiState.Success) -> UiState.Success) {
        _uiState.update { uiState ->
            val success = uiState.state as? UiState.Success ?: return@update uiState
            uiState.copy(state = block(success))
        }
    }
}
