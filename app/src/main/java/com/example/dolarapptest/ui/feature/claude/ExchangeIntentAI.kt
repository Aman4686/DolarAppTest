package com.example.dolarapptest.ui.feature.claude

sealed class ExchangeIntentAI {
    data class AmountChanged(val field: ActiveField, val amount: String) : ExchangeIntentAI()
    data class FieldFocused(val field: ActiveField) : ExchangeIntentAI()
    data object SwapClicked : ExchangeIntentAI()
    data object BottomCurrencyTapped : ExchangeIntentAI()
    data class CurrencySelected(val currency: String) : ExchangeIntentAI()
    data object BottomSheetDismissed : ExchangeIntentAI()
    data object RetryClicked : ExchangeIntentAI()
}
