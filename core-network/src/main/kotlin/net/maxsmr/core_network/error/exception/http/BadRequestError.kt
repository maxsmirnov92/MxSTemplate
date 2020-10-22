package net.maxsmr.core_network.error.exception.http

import net.maxsmr.core_network.error.HttpErrorCode

/**
 * Ошибка при [HttpErrorCode.BAD_REQUEST]
 */
class BadRequestError(
        message: String,
        source: HttpProtocolException
): BaseWrappedHttpException(message, source)