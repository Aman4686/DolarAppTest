package com.example.dolarapptest.domain.model

import java.math.BigDecimal
import java.time.ZonedDateTime

data class Ticker(
    val from: String,
    val to: String,
    val ask: BigDecimal,
    val bid: BigDecimal,
    val date: ZonedDateTime
)
