package com.example.dolarapptest.ui.feature.exchange

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dolarapptest.domain.usecase.ConvertCurrencyUseCase
import com.example.dolarapptest.domain.usecase.GetCurrenciesUseCase
import com.example.dolarapptest.domain.usecase.GetTickersUseCase
import com.example.dolarapptest.ui.feature.exchange.state.ExchangeUiIntent
import com.example.dolarapptest.ui.feature.exchange.state.ExchangeUiState
import com.example.dolarapptest.ui.feature.exchange.state.ExchangeUiState.ExchangeInputFieldUiState
import com.example.dolarapptest.ui.feature.exchange.state.ExchangeUiState.UiState
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
    private val convertCurrencyUseCase: ConvertCurrencyUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(ExchangeUiState(state = UiState.Loading))
    val uiState: StateFlow<ExchangeUiState> = _uiState.asStateFlow()

    init {
        loadInitialState()
    }

    fun loadInitialState(){
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
            _uiState.value = ExchangeUiState(
                state = UiState.Success(
                    availableCurrencies = currenciesList.toImmutableList(),
                    topExchangeInputFieldUiState = ExchangeInputFieldUiState(
                        isSelectable = false,
                        currency = firstTicker.from.uppercase()
                    ),
                    bottomExchangeInputFieldUiState = ExchangeInputFieldUiState(
                        isSelectable = true,
                        currency = firstTicker.to.uppercase()
                    )
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
        updateSuccessUiState { it.copy(topExchangeInputFieldUiState = it.topExchangeInputFieldUiState.copy(amount = amount)) }
    }

    private fun onBottomAmountChanged(amount: String) {
        updateSuccessUiState { it.copy(bottomExchangeInputFieldUiState = it.bottomExchangeInputFieldUiState.copy(amount = amount)) }
    }

    private fun onSwap() {
        updateSuccessUiState {
            it.copy(
                topExchangeInputFieldUiState = it.bottomExchangeInputFieldUiState,
                bottomExchangeInputFieldUiState = it.topExchangeInputFieldUiState
            )
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
            if (state.topExchangeInputFieldUiState.isSelectable)
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

    private fun updateSuccessUiState(block: (UiState.Success) -> UiState.Success) {
        _uiState.update { uiState ->
            val success = uiState.state as? UiState.Success ?: return@update uiState
            uiState.copy(state = block(success))
        }
    }
}
