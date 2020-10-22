package net.maxsmr.core_network.error.exception.inner

import net.maxsmr.commonutils.data.text.EMPTY_STRING
import net.maxsmr.core_network.model.request.api.IApiMapper
import net.maxsmr.core_network.model.response.ResponseObj

/**
 * Бросается при возникновении ненулевого errorCode в [ResponseObj]
 */
class ServerInnerException(
        val errorCode: Int,
        requestInfo: IApiMapper.ApiValueInfo?,
        message: String = EMPTY_STRING,
        cause: Throwable? = null
) : BaseApiException(requestInfo, message, cause) {

    constructor(source: ServerInnerException, cause: Throwable? = null) : this(source.errorCode, source.requestInfo, source.message
            ?: EMPTY_STRING, cause)
}