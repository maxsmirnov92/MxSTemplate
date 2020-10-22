package net.maxsmr.core_network.model.request.api.async

interface UniqueRequest {
    /**
     * @return уникальная строка на основе параметров запроса
     */
    val uniqueRequestID: String

    var tag: String
}