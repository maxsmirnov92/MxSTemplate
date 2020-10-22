package net.maxsmr.core_network.error.exception.http

import net.maxsmr.core_network.error.exception.http.BaseWrappedHttpException
import net.maxsmr.core_network.error.exception.http.HttpProtocolException
import net.maxsmr.core_network.error.HttpErrorCode

/**
 * Ошибка возникающая при отсутствии запрашиваемых данных: [HttpErrorCode.NOT_FOUND]
 */
class NotFoundError(
        message: String,
        source: HttpProtocolException
) : BaseWrappedHttpException(message, source)