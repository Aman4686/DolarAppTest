package com.example.dolarapptest.ui.feature.exchange.state

sealed interface ExchangeUiAction {
    data object Swap : ExchangeUiAction
    data class TopAmountChanged(val amount: String) : ExchangeUiAction
    data class BottomAmountChanged(val amount: String) : ExchangeUiAction

    data object ShowBottomSheet : ExchangeUiAction
    data object HideBottomSheet : ExchangeUiAction
    data class CurrencySelected(val currency: String) : ExchangeUiAction
}