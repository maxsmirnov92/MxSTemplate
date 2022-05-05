package net.maxsmr.core_network.error.exception.http

import net.maxsmr.core_network.error.exception.http.BaseWrappedHttpException
import net.maxsmr.core_network.error.exception.http.HttpProtocolException

/**
 * Внутренняя ошибка сервера: [HttpErrorCode.INTERNAL_SERVER_ERROR]
 */
class InternalServerError(
    message: String,
    source: HttpProtocolException
) : BaseWrappedHttpException(message, source)