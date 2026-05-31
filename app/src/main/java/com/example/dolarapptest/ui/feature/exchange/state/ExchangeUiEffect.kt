package com.example.dolarapptest.ui.feature.exchange.state

import androidx.annotation.StringRes

sealed interface ExchangeUiEffect {
    sealed class ShowToast : ExchangeUiEffect {
        data class Res(@StringRes val messageRes: Int) : ShowToast()
        data class Text(val message: String) : ShowToast()
    }
}
