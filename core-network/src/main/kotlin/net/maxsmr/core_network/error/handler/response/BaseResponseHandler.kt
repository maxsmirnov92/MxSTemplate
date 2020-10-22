package net.maxsmr.core_network.error.handler.response

import net.maxsmr.core_network.error.exception.inner.BaseApiException
import net.maxsmr.core_network.model.request.api.IApiMapper
import net.maxsmr.core_network.model.response.ResponseObj

/**
 * Интерфейс обработчика [ResponseObj] в маппере
 */
interface BaseResponseHandler {

    fun handleResponse(requestInfo: IApiMapper.ApiValueInfo, response: ResponseObj<*>): BaseApiException?
}