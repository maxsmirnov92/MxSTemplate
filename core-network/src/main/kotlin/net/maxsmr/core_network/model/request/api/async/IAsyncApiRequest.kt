package net.maxsmr.core_network.model.request.api.async

import com.google.gson.GsonBuilder
import org.json.JSONObject

const val DEFAULT_INTERVAL = 1000L
const val DEFAULT_CACHE_LIFE_TIME = 10 * 60 * 1000L
/**
 * при изменении параметра следует учитывать таймаут между запросами {@link DEFAULT_INTERVAL}
 * <p>
 * общее время выполнения запросов = {@link AsyncApiRequest#DEFAULT_INTERVAL} * DEFAULT_MAX_COUNTER
 *
 * @see SimpleRequestInfo#counter
 */
const val DEFAULT_MAX_COUNTER = 60

interface IAsyncApiRequest {
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
    val cacheLifeTime: Long

    /**
     * максимальное количество попыток получить ответ от сервера
     */
    val maxRequestCount: Int

    var tag: String

    var updateInterval: Long

    var force: Boolean

    /**
     * @return уникальный идентификатор запроса на основе его параметров
     * по умолчанию используется тело запроса, но не его заголовки
     */
    var requestId: String?

    fun getVersion(): String

    fun getMethod(): String

    fun isRequireDisplayErrorMessage(): Boolean

    fun useOnlyRussianLocale(): Boolean

    fun getGsonBuilderBody(): GsonBuilder

    /**
     * JSON, содержащий тело исходного запроса (если необходимо) или пустой JSON
     * @return представление тела запроса в виде JSONObject
     */
    val originalJSONBodyRequest: JSONObject?
}