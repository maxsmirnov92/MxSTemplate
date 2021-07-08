package net.maxsmr.core_network.interceptor

import android.text.TextUtils
import android.util.Log
import net.maxsmr.commonutils.logger.BaseLogger
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder
import net.maxsmr.core_network.model.request.api.IApiMapper
import net.maxsmr.core_network.model.request.api.IApiMapper.Companion.findApiRequestByOriginalRequest
import net.maxsmr.core_network.model.request.log.service.RequestLoggerHolder
import net.maxsmr.core_network.utils.headersToMap
import net.maxsmr.core_network.utils.responseBodyToString
import okhttp3.Interceptor
import okhttp3.Response
import okhttp3.ResponseBody
import org.json.JSONObject
import java.io.IOException

private val logger = BaseLoggerHolder.instance.getLogger<BaseLogger>("RemoteLoggingInterceptor")

class RemoteLoggingInterceptor(
    apiMap: Map<IApiMapper.ApiKeyInfo, IApiMapper.ApiValueInfo>,
    proceedOriginalRequest: Boolean
) : BaseApiInterceptor(apiMap, proceedOriginalRequest) {

    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        var response = proceed(chain, request)
        val path = TextUtils.join("/", request.url.pathSegments)
        val apiRequestPair = findApiRequestByOriginalRequest(apiMap, request, path)
        val apiRequest = apiRequestPair.second?.request
        if (apiRequest == null) {
            logger.w("No request found in mapper for path $path and hash ${apiRequestPair.first}!")
        } else {
            if (response.isSuccessful) {
                val responseBody = response.body
                if (responseBody != null) {
                    val contentType = responseBody.contentType()
                    val responseBodyString = responseBodyToString(response)
                    val responseJsonBody = if (responseBodyString.isNotEmpty()) JSONObject(responseBodyString) else null
                    RequestLoggerHolder.requestLogger.logResponse(apiRequest, responseJsonBody, response.code, headersToMap(response.headers), false)
                    // новый response, поскольку у оригинального тело было закрыто
                    response = response.newBuilder().body(ResponseBody.create(contentType, responseBodyString)).build()
                }
            }
        }
        return response
    }
}