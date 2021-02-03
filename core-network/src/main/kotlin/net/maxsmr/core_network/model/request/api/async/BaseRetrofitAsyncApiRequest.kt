package net.maxsmr.core_network.model.request.api.async

import com.google.gson.JsonObject
import net.maxsmr.commonutils.gson.exclusion.FieldExclude
import net.maxsmr.commonutils.text.EMPTY_STRING
import org.json.JSONException
import org.json.JSONObject
import net.maxsmr.core_network.model.request.api.BaseRetrofitModelApiRequest
import net.maxsmr.core_network.model.request.log.BaseLogRequestData

abstract class BaseRetrofitAsyncApiRequest<LogRequestData : BaseLogRequestData.ISourceRequestData>(
        protected val requiredRequestBodyRepeatedly: Boolean
)
    : BaseRetrofitModelApiRequest<LogRequestData>(), IAsyncApiRequest, UniqueRequest {

    constructor() : this(false)

    /**
     * Время жизни кеша
     *
     * Одно и тоже содержимое кеша может быть валидным для разных клиентов,
     * в зависимости от допустимого времени жизни
     *
     * @see AsyncRequestManager
     *
     * @see RequestCacheManager
     */
    override val cacheLifeTime: Long get() = DEFAULT_CACHE_LIFE_TIME

    /**
     * максимальное количество попыток получить ответ от сервера
     */
    override val maxRequestCount: Int get() = DEFAULT_MAX_COUNTER

    override val uniqueRequestID: String get() =  requestId ?: EMPTY_STRING

    @FieldExclude
    override var updateInterval: Long = DEFAULT_INTERVAL

    @FieldExclude
    override var force = false

    @FieldExclude
    override var requestId: String? = null
        get() {
            var requestId = field
            if (requestId == null) {
                requestId = javaClass.simpleName + "@" + getBody().hashCode().toString()
                field = requestId
            }
            return requestId
        }

    @FieldExclude
    override var tag: String = EMPTY_STRING

    override val originalJSONBodyRequest: JSONObject? get() {
        if (!requiredRequestBodyRepeatedly) return JSONObject()
        val body: Any = getBody()
        return try {
            if (body is JSONObject || body is JsonObject || body is String) JSONObject(body.toString()) else JSONObject()
        } catch (e: JSONException) {
            JSONObject()
        }
    }
}