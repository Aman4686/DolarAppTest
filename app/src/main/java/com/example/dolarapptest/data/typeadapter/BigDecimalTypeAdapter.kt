package com.example.dolarapptest.data.typeadapter

import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import java.math.BigDecimal

class BigDecimalTypeAdapter : TypeAdapter<BigDecimal>() {
    override fun write(out: JsonWriter, value: BigDecimal?) {
        out.value(value)
    }

    override fun read(reader: JsonReader): BigDecimal = BigDecimal(reader.nextString())
}