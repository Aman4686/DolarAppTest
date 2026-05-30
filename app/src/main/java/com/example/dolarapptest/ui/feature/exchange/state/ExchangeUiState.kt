package com.example.dolarapptest.ui.feature.exchange.state

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

data class ExchangeUiState(
    val state: ScreenState = ScreenState.Success(),
    val showOverlayLoader: Boolean = false
){
    sealed interface ScreenState {
        data object Loading : ScreenState
        data class Error(val message: String = "") : ScreenState
        data class Success(
            val topExchangeInputFieldUiState: ExchangeInputFieldUiState = ExchangeInputFieldUiState(),
            val bottomExchangeInputFieldUiState: ExchangeInputFieldUiState = ExchangeInputFieldUiState(),
            val rateLabel: String = "",
            val isShowBottomSheet: Boolean = false,
            val availableCurrencies: ImmutableList<String> = persistentListOf(),
        ) : ScreenState {
            companion object {
                fun preview() = Success(
                    topExchangeInputFieldUiState = ExchangeInputFieldUiState(currency = "USD", amount = "100"),
                    bottomExchangeInputFieldUiState = ExchangeInputFieldUiState(currency = "EUR", amount = "92.50"),
                    rateLabel = "1 USD = 0.925 EUR",
                    availableCurrencies = persistentListOf("USD", "EUR", "GBP", "JPY", "ARS")
                )
            }
        }
    }

    data class ExchangeInputFieldUiState(
        val isSelectable: Boolean = false,
        val currency: String = "",
        val amount: String = ""
    )
}
