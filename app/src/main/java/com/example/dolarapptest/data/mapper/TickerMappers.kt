package com.example.dolarapptest.data.mapper

import com.example.dolarapptest.data.model.TickerResponse
import com.example.dolarapptest.domain.model.Ticker
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

fun TickerResponse.toDomain(): Ticker {
    val (from, to) = book.split("_").map { it.uppercase() }
    return Ticker(
        from = from,
        to = to,
        ask = ask,
        bid = bid,
        date = LocalDateTime.parse(date, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            .atZone(ZoneOffset.UTC)
    )
}