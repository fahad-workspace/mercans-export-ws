package com.mercans.export.constants

import com.fasterxml.jackson.annotation.JsonValue

@Suppress("unused")
enum class DataType(@JsonValue val typeName: String) {

    Text("Text"),
    Integer("Integer"),
    Decimal("Decimal"),
    Bool("Bool"),
    Date("Date")
}
