package com.example.dolarapptest.ui.feature.exchange

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dolarapptest.domain.usecase.ConvertCurrencyUseCase
import com.example.dolarapptest.domain.usecase.GetCurrenciesUseCase
import com.example.dolarapptest.domain.usecase.GetTickersUseCase
import com.example.dolarapptest.ui.feature.exchange.state.ExchangeUiAction
import com.example.dolarapptest.ui.feature.exchange.state.ExchangeUiState
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
    private val _uiState = MutableStateFlow(ExchangeUiState(isLoading = true))
    val uiState: StateFlow<ExchangeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val currenciesList = getCurrenciesUseCase().getOrNull() ?: return@launch
            val firstTicker = getTickersUseCase(currenciesList).getOrNull()?.firstOrNull() ?: return@launch
            _uiState.update {
                it.copy(
                    isLoading = false,
                    availableCurrencies = currenciesList.toImmutableList(),
                    topExchangeInputFieldUiState = it.topExchangeInputFieldUiState.copy(
                        isSelectable = false,
                        currency = firstTicker.from.uppercase()
                    ),
                    bottomExchangeInputFieldUiState = it.bottomExchangeInputFieldUiState.copy(
                        isSelectable = true,
                        currency = firstTicker.to.uppercase()
                    )
                )
            }
        }
    }


    fun onAction(action: ExchangeUiAction) {
        when (action) {
            is ExchangeUiAction.TopAmountChanged -> onTopAmountChanged(action.amount)
            is ExchangeUiAction.BottomAmountChanged -> onBottomAmountChanged(action.amount)
            ExchangeUiAction.Swap -> onSwap()
            ExchangeUiAction.ShowBottomSheet -> onShowBottomSheet()
            ExchangeUiAction.HideBottomSheet -> onHideBottomSheet()
            is ExchangeUiAction.CurrencySelected -> onCurrencySelected(action.currency)
        }
    }

    private fun onTopAmountChanged(amount: String) {
        _uiState.update {
            it.copy(topExchangeInputFieldUiState = it.topExchangeInputFieldUiState.copy(amount = amount))
        }
    }

    private fun onBottomAmountChanged(amount: String) {
        _uiState.update {
            it.copy(bottomExchangeInputFieldUiState = it.bottomExchangeInputFieldUiState.copy(amount = amount))
        }
    }

    private fun onSwap() {
        _uiState.update {
            it.copy(
                topExchangeInputFieldUiState = it.bottomExchangeInputFieldUiState,
                bottomExchangeInputFieldUiState = it.topExchangeInputFieldUiState
            )
        }
    }

    private fun onShowBottomSheet() {
        _uiState.update { it.copy(isShowBottomSheet = true) }
    }

    private fun onHideBottomSheet() {
        _uiState.update { it.copy(isShowBottomSheet = false) }
    }

    private fun onCurrencySelected(currency: String) {
        _uiState.update {
            if (it.topExchangeInputFieldUiState.isSelectable)
                it.copy(
                    topExchangeInputFieldUiState = it.topExchangeInputFieldUiState.copy(currency = currency),
                    isShowBottomSheet = false
                )
            else
                it.copy(
                    bottomExchangeInputFieldUiState = it.bottomExchangeInputFieldUiState.copy(currency = currency),
                    isShowBottomSheet = false
                )
        }
    }


}