package com.mercans.export.models

data class Request(
    var errors: Set<String>? = setOf(),
    var fname: String = "",
    var payload: List<Payload> = listOf(),
    var uuid: String = ""
) {

    data class Payload(
        var action: String = "",
        var data: Map<String, Any> = mapOf(),
        var employeeCode: String = "",
        var payComponents: List<Map<String, Any>>? = listOf()
    )
}
