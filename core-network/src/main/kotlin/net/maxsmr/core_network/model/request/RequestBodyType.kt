package net.maxsmr.core_network.model.request

enum class RequestBodyType {
    STRING,
    JSON,
    /**
     * Content-Type: multipart/form-data
     */
    MULTIPART
}