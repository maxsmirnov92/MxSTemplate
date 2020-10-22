package net.maxsmr.core_network.error.exception.inner

import net.maxsmr.commonutils.data.text.EMPTY_STRING
import net.maxsmr.core_network.model.request.api.IApiMapper

/**
 * [Throwable], возникающий в [BaseResponseHandler]
 * с целью проведения релогина
 */
class NeedReLoginException(
        val login: String,
        val password: String,
        val sourceMethod: String,
        requestInfo: IApiMapper.ApiValueInfo,
        message: String = EMPTY_STRING,
        cause: Throwable? = null
) : BaseApiException(requestInfo, message, cause)