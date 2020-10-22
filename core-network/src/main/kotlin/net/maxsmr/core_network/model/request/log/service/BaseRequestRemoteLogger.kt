package net.maxsmr.core_network.model.request.log.service

import org.json.JSONObject
import net.maxsmr.core_network.error.exception.http.HttpProtocolException
import net.maxsmr.core_network.model.request.api.BaseApiRequest
import net.maxsmr.core_network.model.request.log.BaseLogRequestData
import net.maxsmr.core_network.model.request.log.service.params.LogSendParams
import java.util.concurrent.TimeUnit

val DEFAULT_SCHEDULE_TIME = TimeUnit.MINUTES.toMillis(1)

/**
 * Базовый логгер для создания [BaseLogRequestData] и её отправки
 */
abstract class BaseRequestRemoteLogger<P : LogSendParams,
        S : BaseLogRequestData.ISourceRequestData,
        L : BaseLogRequestData<S, *>> {

    /**
     * Из внешнего класса оповещение о том, что отправка была начата
     */
    abstract fun notifySendStarted()

    /**
     * Из внешнего класса оповещение о том, что фоновое состояние изменилось
     */
    abstract fun notifyAppStateChanged(state: AppState)

    abstract fun startSend(params: P)

    abstract fun restartSend(params: P)

    abstract fun stopSend()

    abstract fun scheduleSend(delay: Long = DEFAULT_SCHEDULE_TIME, params: P)

    abstract fun rescheduleSend(delay: Long = DEFAULT_SCHEDULE_TIME, params: P)

    abstract fun cancelScheduleSend()

    fun notifyAppForeground() {
        notifyAppStateChanged(AppState.FOREGROUND)
    }

    /**
     * Логирование успешного ответа с указанием исходного [request]
     */
    fun logResponse(request: BaseApiRequest<*, S>, response: JSONObject?, httpCode: Int, headers: Map<String, String>) {
        val result: JSONObject? = response?.optJSONObject("result")
        //метод вызывается и на промежуточный результат асинхронных запросов, игнорируем
        if (result?.has("rid") == true) return
        //метод вызывается и на промежуточный результат асинхронных запросов, игнорируем
        toLogRequestDataFromResponse(request, headers, httpCode, response)?.let { logRequestData ->
            var shouldSend = true
            result.let {
                if (handleResult(logRequestData, result)) {
                    shouldSend = false
                }
            }
            if (shouldSend) {
                sendLogs(logRequestData)
            }
        }
    }

    /**
     * Логирование успешного ответа с указанием готовой [BaseLogRequestData]
     */
    fun logResponse(requestData: S?, response: Any?, httpCode: Int, headers: Map<String, String>) {
        toLogRequestDataFromResponse(requestData, headers, httpCode, response)?.let {
            sendLogs(it)
        }
    }

    /**
     * Логирование ошибки от [com.android.volley.toolbox.Volley]
     *
     * @param error [VolleyError]
     */
    fun logError(request: BaseApiRequest<*, S>, error: HttpProtocolException?) {
        toLogRequestDataFromError(request, error)?.let {
            sendLogs(it)
        }
    }

    protected abstract fun handleResult(logRequestData: L, result: JSONObject?): Boolean

    /**
     * Отправка готового объекта с логами
     */
    protected abstract fun sendLogs(requestData: L)

    protected abstract fun toLogRequestDataFromError(request: BaseApiRequest<*, S>, error: HttpProtocolException?): L?

    protected abstract fun toLogRequestDataFromResponse(
            source: S?,
            headers: Map<String, String>,
            httpCode: Int,
            response: Any?
    ): L?

    protected fun toLogRequestDataFromResponse(
        request: BaseApiRequest<*, S>,
        headers: Map<String, String>,
        httpCode: Int,
        response: Any?
    ): L? = toLogRequestDataFromResponse(request.getLoggedData(), headers, httpCode, response)

    enum class AppState {

        FOREGROUND, BACKGROUND, NOT_RUNNING
    }
}