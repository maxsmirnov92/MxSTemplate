package net.maxsmr.core_network.model.request

enum class MethodType(val value: Int) {
    GET(0),
    POST(1),
    PUT (2),
    DELETE (3),
    HEAD(4),
    OPTIONS(5),
    TRACE(6),
    PATCH(7),
    UNKNOWN(-1)
}