package net.maxsmr.core_network.error.handler.error.http

import android.net.Uri
import android.util.Log
import net.maxsmr.commonutils.data.text.EMPTY_STRING
import net.maxsmr.core_network.error.exception.http.HttpProtocolException
import net.maxsmr.core_network.model.request.api.IApiMapper
import net.maxsmr.core_network.model.request.log.service.RequestLoggerHolder

private const val LOG_TAG = "LogHttpErrorHandler"

/**
 * Пример реализации [BaseHttpErrorHandler]
 * с выводом обёрнутого сообщения в лог
 */
class LogHttpErrorHandler(
        private val apiMap: Map<IApiMapper.ApiKeyInfo, IApiMapper.ApiValueInfo>
) : BaseHttpErrorHandler {

    override fun handle(e: HttpProtocolException) {
//        Log.e(LOG_TAG, "Network error occurred: $e", e)
        logHttpProtocolException(e)
    }

    private fun logHttpProtocolException(e: HttpProtocolException) {
        val path = Uri.parse(e.url).path ?: EMPTY_STRING
        val baseApiRequestPair = IApiMapper.findApiRequestByOriginalRequestBody(apiMap, e.requestBodyString, path)
        val baseApiRequest = baseApiRequestPair.second?.request
        if (baseApiRequest == null) {
            Log.w(LOG_TAG, "No request found in mapper for path $path and hash ${baseApiRequestPair.first}!")
        } else {
            RequestLoggerHolder.requestLogger.logError(baseApiRequest, e, true)
        }
    }
}