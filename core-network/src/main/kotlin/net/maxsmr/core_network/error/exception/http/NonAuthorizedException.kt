package net.maxsmr.core_network.error.exception.http

import net.maxsmr.commonutils.data.text.EMPTY_STRING
import net.maxsmr.core_network.error.HttpErrorCode
import net.maxsmr.core_network.error.exception.NetworkException

/**
 * Ошибка при отсутствии авторизации:
 * на уровне http [HttpErrorCode.NOT_AUTHORIZED] - включает [HttpProtocolException]
 * или на уровне errorCode - включает [ServerInnerException]
 */
class NonAuthorizedException(
        message: String? = null,
        cause: Throwable
): NetworkException(message ?: EMPTY_STRING, cause)