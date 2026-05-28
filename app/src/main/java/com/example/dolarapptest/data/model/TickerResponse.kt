package com.example.dolarapptest.data.model

import com.google.gson.annotations.SerializedName
import java.math.BigDecimal

data class TickerResponse(
    @SerializedName("ask") val ask: BigDecimal,
    @SerializedName("bid") val bid: BigDecimal,
    @SerializedName("book") val book: String,
    @SerializedName("date") val date: String
) {
    // "usdc_mxn" → "MXN"
    val currencyCode: String
        get() = book.substringAfterLast("_").uppercase()
}