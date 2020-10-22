package net.maxsmr.core_network.error.exception.inner.rid

import net.maxsmr.commonutils.data.text.EMPTY_STRING
import net.maxsmr.core_network.model.request.api.IApiMapper
import net.maxsmr.core_network.error.exception.inner.BaseApiException

/**
 * Ошибка, возникающая при конверсии результата [GetResultRequest]
 * @param resultData из ответа
 * @param cause исходная ошибка из [BaseResponseHandler], если есть
 */
class RidGetResultException(
        val resultData: String,
        sourceRequestInfo: IApiMapper.ApiValueInfo,
        cause: Throwable? = null
) : BaseApiException(sourceRequestInfo, EMPTY_STRING, cause)