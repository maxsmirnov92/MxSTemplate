package net.maxsmr.core_network.error.exception.inner

import net.maxsmr.commonutils.data.text.EMPTY_STRING
import net.maxsmr.core_network.model.request.api.IApiMapper

/**
 * Исключение, возникающее при пустых данных
 */
class EmptyResponseException(
        requestInfo: IApiMapper.ApiValueInfo?,
        message: String = EMPTY_STRING,
        cause: Throwable? = null
) : BaseApiException(requestInfo, message, cause)