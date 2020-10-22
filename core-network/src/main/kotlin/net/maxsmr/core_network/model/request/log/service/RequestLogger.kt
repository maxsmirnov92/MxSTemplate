package net.maxsmr.core_network.model.request.log.service

import net.maxsmr.core_network.model.request.MethodType
import org.json.JSONObject
import net.maxsmr.core_network.error.exception.http.HttpProtocolException
import net.maxsmr.core_network.model.request.api.BaseApiRequest
import net.maxsmr.core_network.model.request.log.BaseLogRequestData
import net.maxsmr.core_network.model.request.log.service.params.LogSendParams

/**
 * Класс занимается логированием сетевых запросов и ответов
 */
@Suppress("UNCHECKED_CAST")
class RequestLogger<P : LogSendParams,
        S : BaseLogRequestData.ISourceRequestData,
        L : BaseLogRequestData<S, *>>{

    lateinit var logCatLogger: BaseRequestLogcatLogger
    lateinit var remoteLogger: BaseRequestRemoteLogger<P, S, L>


    fun logRequest(
        apiRequest: BaseApiRequest<*, *>,
        methodType: MethodType,
        useLogCat: Boolean = true
    ) {
        if (useLogCat) {
            logCatLogger.logRequest(apiRequest, methodType)
        }
    }

    /**
     * Логирование успешного ответа
     */
    fun logResponse(request: BaseApiRequest<*, *>, response: JSONObject?, httpCode: Int, headers: Map<String, String>, useLogCat: Boolean = true) {
        if (useLogCat) {
            logCatLogger.logResponse(request, response)
        }
        remoteLogger.logResponse(request as BaseApiRequest<*, S>, response, httpCode, headers)
    }

    /**
     * Логирование ошибки от [com.android.volley.toolbox.Volley]
     *
     * @param error [VolleyError]
     */
    fun logError(request: BaseApiRequest<*, *>, error: HttpProtocolException?, useLogCat: Boolean = true) {
        if (useLogCat) {
            logCatLogger.logError(error)
        }
        remoteLogger.logError(request as BaseApiRequest<*, S>, error)
    }
}