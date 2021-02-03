package net.maxsmr.core_network.error.exception.inner

import net.maxsmr.commonutils.text.EMPTY_STRING
import net.maxsmr.core_network.error.exception.NetworkException
import net.maxsmr.core_network.model.request.api.IApiMapper

/**
 * [NetworkException], содержащий информацию об исходном запросе
 */
open class BaseApiException(
        val requestInfo: IApiMapper.ApiValueInfo?,
        message: String = EMPTY_STRING,
        cause: Throwable? = null
): NetworkException(message, cause)