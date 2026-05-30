package com.example.dolarapptest.ui.feature.claude

import com.example.dolarapptest.domain.model.Ticker

enum class ActiveField { TOP, BOTTOM }

data class ExchangeUiStateAI(
    val isLoading: Boolean = false,
    val error: String? = null,
    val topCurrency: String = "",
    val bottomCurrency: String = "",
    val topAmount: String = "",
    val bottomAmount: String = "",
    val activeField: ActiveField = ActiveField.TOP,
    val ticker: Ticker? = null,
    val availableCurrencies: List<String> = emptyList(),
    val showBottomSheet: Boolean = false,
    val rateLabel: String = ""
)