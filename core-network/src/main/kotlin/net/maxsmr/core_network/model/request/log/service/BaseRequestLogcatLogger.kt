package net.maxsmr.core_network.model.request.log.service

import net.maxsmr.core_network.BuildConfig
import net.maxsmr.core_network.error.exception.http.HttpProtocolException
import net.maxsmr.core_network.model.request.MethodType
import org.json.JSONObject

import net.maxsmr.core_network.model.request.api.BaseApiRequest

/**
 * Базовый класс для логирования request/response в LogCat
 */
abstract class BaseRequestLogcatLogger {

    protected open val logEnabled: Boolean
        get() = BuildConfig.DEBUG

    abstract fun logRequest(apiRequest: BaseApiRequest<*, *>, methodType: MethodType)

    /**
     * Логирование успешного ответа
     *
     * @param response дополненный ответ, включает также информацию об ошибки
     */
    abstract fun logResponse(request: BaseApiRequest<*, *>, response: JSONObject?)

    /**
     * Логирование ошибки от [com.android.volley.toolbox.Volley]
     *
     * @param error [VolleyError]
     */
    abstract fun logError(error: HttpProtocolException?)
}