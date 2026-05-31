package com.example.dolarapptest.ui.feature.exchange.state

sealed interface ExchangeUiEffect {
    data class ShowToast(val message: String) : ExchangeUiEffect
}
