package com.example.dolarapptest.ui.feature.exchange.state

sealed interface ExchangeUiIntent {
    data object Swap : ExchangeUiIntent
    data class TopAmountChanged(val amount: String) : ExchangeUiIntent
    data class BottomAmountChanged(val amount: String) : ExchangeUiIntent

    data object ShowBottomSheet : ExchangeUiIntent
    data object HideBottomSheet : ExchangeUiIntent
    data class CurrencySelected(val currency: String) : ExchangeUiIntent
}