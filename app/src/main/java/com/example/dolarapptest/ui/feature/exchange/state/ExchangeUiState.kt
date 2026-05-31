package com.example.dolarapptest.ui.feature.exchange.state

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

data class ExchangeUiState(
    val state: UiState = UiState.Success(),
    val showOverlayLoader: Boolean = false
){
    sealed interface UiState {
        data object Loading : UiState
        data class Error(val message: String = "") : UiState
        data class Success(
            val firstExchangeInputFieldUiState: ExchangeInputFieldUiState = ExchangeInputFieldUiState(),
            val secondExchangeInputFieldUiState: ExchangeInputFieldUiState = ExchangeInputFieldUiState(),
            val baseCurrencyField: FieldPosition = FieldPosition.TOP,
            val activeInputField: FieldPosition = FieldPosition.TOP,
            val rateLabel: String = "",
            val isShowBottomSheet: Boolean = false,
            val availableCurrencies: ImmutableList<String> = persistentListOf(),
            val exchangeRate: String = "",
        ) : UiState {
            companion object {
                fun preview() = Success(
                    firstExchangeInputFieldUiState = ExchangeInputFieldUiState(currency = "USD", amount = "100"),
                    secondExchangeInputFieldUiState = ExchangeInputFieldUiState(currency = "EUR", amount = "92.50"),
                    baseCurrencyField = FieldPosition.TOP,
                    rateLabel = "1 USD = 0.925 EUR",
                    availableCurrencies = persistentListOf("USD", "EUR", "GBP", "JPY", "ARS")
                )
            }
        }
    }

    data class ExchangeInputFieldUiState(
        val currency: String = "",
        val amount: String = ""
    )
}

// TODO should add third state - NONE
enum class FieldPosition { TOP, BOTTOM }
