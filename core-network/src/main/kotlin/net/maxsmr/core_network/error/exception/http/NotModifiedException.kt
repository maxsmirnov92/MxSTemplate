package net.maxsmr.core_network.error.exception.http

import net.maxsmr.core_network.error.exception.http.BaseWrappedHttpException
import net.maxsmr.core_network.error.exception.http.HttpProtocolException
import net.maxsmr.core_network.error.HttpErrorCode

/**
 * Ошибка сервера с кодом [HttpErrorCode.NOT_MODIFIED]
 */
class NotModifiedException(
        message: String,
        source: HttpProtocolException
) : BaseWrappedHttpException(message, source)