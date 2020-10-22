package net.maxsmr.core_network.error.exception.inner.rid

import net.maxsmr.commonutils.data.text.EMPTY_STRING
import net.maxsmr.core_network.model.request.api.IApiMapper
import net.maxsmr.core_network.model.request.api.async.BaseRetrofitAsyncApiRequest
import net.maxsmr.core_network.error.exception.inner.BaseApiException
import net.maxsmr.core_network.error.handler.response.BaseResponseHandler

/**
 * Бросается из [BaseResponseHandler], если в ответе есть "rid"
 * @param asyncApiRequest должен совпадать с тем, что в sourceRequestInfo
 */
class RidException(
        var rid: String,
        val asyncApiRequest: BaseRetrofitAsyncApiRequest<*>,
        sourceRequestInfo: IApiMapper.ApiValueInfo
) : BaseApiException(sourceRequestInfo, EMPTY_STRING, null)