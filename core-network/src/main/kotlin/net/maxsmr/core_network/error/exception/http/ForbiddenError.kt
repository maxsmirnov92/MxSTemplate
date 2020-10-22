package net.maxsmr.core_network.error.exception.http

import net.maxsmr.core_network.error.HttpErrorCode

/**
 * Ошибка запрета доступа: [HttpErrorCode.FORBIDDEN]
 */
class ForbiddenError(
        message: String,
        source: HttpProtocolException
) : BaseWrappedHttpException(message, source)