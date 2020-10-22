package net.maxsmr.core_network.error.exception.http

import net.maxsmr.core_network.error.exception.http.BaseWrappedHttpException
import net.maxsmr.core_network.error.exception.http.HttpProtocolException

/**
 * Неизвестная ошибка, причина в [cause]
 */
class OtherHttpException(
        message: String,
        cause: HttpProtocolException
) : BaseWrappedHttpException(message, cause)