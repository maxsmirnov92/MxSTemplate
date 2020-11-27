package net.maxsmr.mxstemplate.api.handler

import android.content.Context
import net.maxsmr.core_network.error.exception.inner.BaseApiException
import net.maxsmr.core_common.arch.ErrorHandler
import net.maxsmr.core_network.error.handler.response.BaseResponseHandler
import net.maxsmr.core_network.model.request.api.IApiMapper
import net.maxsmr.core_network.model.response.ResponseObj

open class StandardResponseHandler(
        val context: Context,
        val errorHandler: ErrorHandler
) : BaseResponseHandler {

    override fun handleResponse(requestInfo: IApiMapper.ApiValueInfo, response: ResponseObj<*>): BaseApiException? {
       return null
    }
}