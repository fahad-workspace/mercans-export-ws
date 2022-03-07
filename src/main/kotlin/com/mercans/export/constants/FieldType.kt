package com.mercans.export.constants

import com.fasterxml.jackson.annotation.JsonValue

@Suppress("unused")
enum class FieldType(@JsonValue val typeName: String) {

    Regular("Regular"),
    ActionCode("ActionCode"),
    EmployeeCode("EmployeeCode")
}
