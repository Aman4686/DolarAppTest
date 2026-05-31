package com.example.dolarapptest.ui.feature.exchange.state

import androidx.annotation.StringRes

sealed interface ExchangeUiEffect {
    data class ShowToast(@StringRes val messageRes: Int) : ExchangeUiEffect
}
