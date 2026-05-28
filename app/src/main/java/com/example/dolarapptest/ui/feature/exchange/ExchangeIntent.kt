package com.example.dolarapptest.ui.feature.exchange

sealed class ExchangeIntent {
    data class AmountChanged(val field: ActiveField, val amount: String) : ExchangeIntent()
    data class FieldFocused(val field: ActiveField) : ExchangeIntent()
    data object SwapClicked : ExchangeIntent()
    data object BottomCurrencyTapped : ExchangeIntent()
    data class CurrencySelected(val currency: String) : ExchangeIntent()
    data object BottomSheetDismissed : ExchangeIntent()
    data object RetryClicked : ExchangeIntent()
}
