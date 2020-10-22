package net.maxsmr.core_network.error.exception.http

import net.maxsmr.core_network.error.exception.NetworkException

/**
 * Базовый класс ошибки;
 * содержит дополнительное описание к [HttpProtocolException]
 */
abstract class BaseWrappedHttpException
protected constructor(
        message: String,
        val httpCause: HttpProtocolException
) : NetworkException(message, httpCause) {

    override fun toString(): String {
        return "BaseWrappedHttpException(message='$message', httpCause=$httpCause)"
    }
}
