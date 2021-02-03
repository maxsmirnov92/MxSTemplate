package net.maxsmr.core_network.model.request.api

import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import net.maxsmr.commonutils.text.EMPTY_STRING
import net.maxsmr.core_network.di.networkComponent
import net.maxsmr.core_network.model.request.RequestBodyType
import net.maxsmr.core_network.model.request.log.BaseLogRequestData
import org.json.JSONObject
import java.io.Serializable
import java.util.*

private const val MAP_REQUEST_HASH_FORMAT = "%s_%s"

/**
 * Базовый ApiRequest: может быть использован в связке с Volley или отдельно от него
 */
abstract class BaseApiRequest<RequestBody, LogRequestData: BaseLogRequestData.ISourceRequestData>: Serializable {

    /**
     * Метод запроса, рекомендуется использовать [getMethod]
     *
     * @return контроллер сервиса с наименование метода
     */
    abstract fun getMethod(): String

    /**
     * @return тело запроса [JSONObject] или [JsonObject] или [BaseModel] или[FileRequestBody]
     * Если добавлять в запрос тело не нужно, то можно использовать заглушку [ApiRequest.EMPTY_BODY]
     */
    abstract fun getBody(): RequestBody?

    /**
     * @return версия API
     */
    open fun getVersion(): String {
        return ApiVersion.V1.versionName;
    }

    /**
     * Метод для получения билдера Gson-на для тела запроса
     *
     * @return билдер для тела запроса
     */
    open fun getGsonBuilderBody(): GsonBuilder {
        return networkComponent.gsonBuilder()
    }

    /**
     * Метод для получения билдера Gson-на для ответа
     *
     * @return билдер для ответа
     */
    open fun getGsonBuilderResponse(): GsonBuilder {
        return GsonBuilder()
    }

    open fun getRequestBodyType(): RequestBodyType {
        return RequestBodyType.JSON
    }

    /**
     * @return true - значит, сессия [Session.get] будет добавлена в тело запроса
     */
    open fun isRequireSession(): Boolean {
        return false
    }

    /**
     * @return true - значит, DeviceGuid [Session.get] будет добавлена в тело запроса
     */
    open fun isRequireDeviceGuid(): Boolean {
        return true
    }

    /**
     * @return true - значит, ProtocolVersion [Session.get] будет добавлена в тело запроса
     */
    open fun isRequireProtocolVersion(): Boolean {
        return true
    }

    /**
     * @return true - значит, PlatformAndVersionName [Session.get] будет добавлена в тело запроса
     */
    open fun isRequirePlatformAndVersionName(): Boolean {
        return true
    }

    /**
     * Показывать ли пользователю сообщение об ошибке сервисов
     *
     * @return true - отображать пользвоателю диалог с ошибкой
     */
    open fun isRequireDisplayErrorMessage(): Boolean {
        return true
    }

    /**
     * Нужно ли добавлять в тело запроса информацию о текущем языке устройства
     *
     * @return true - добавлять, по умолчанию false
     */
    open fun isRequireLanguage(): Boolean {
        return false
    }

    /**
     * Нужно ли устанавливать всегда русскую локаль при isRequireLanguage = true
     *
     * @return true - устанавливать всегда русскую локаль, по умолчанию false
     */
    open fun useOnlyRussianLocale(): Boolean {
        return false
    }

    open fun needProcessUnauthorizedError(): Boolean {
        return true
    }

    /**
     * Нужно ли добавлять в тело запроса хеш-код
     *
     * @return true - добавлять
     */
    open fun isRequireHashCode(): Boolean {
        return true
    }

    /**
     * Вычисляет специфичную для запроса часть строки на основе нескольких его параметров.
     * Затем эта строка используется для вычисления хеш-кода запроса.
     *
     * @return конкатенацию определённых параметров запроса, по умолчанию ""
     */
    open fun getHashString(): String {
        return EMPTY_STRING
    }

    /**
     * Установка времени выполнения запроса, по умолчанию [.DEFAULT_REQUEST_TIMEOUT_IN_MILLS]
     *
     * @return количество миллисекунд на запрос [JsonObjectRequest]
     */
    open fun getTimeoutRequest(): Int {
        return DEFAULT_REQUEST_TIMEOUT_IN_MILLS
    }

    /**
     * @return мапа с заголовками запроса
     */
    open fun getHeaders(): Map<String, String> {
        return emptyMap()
    }


    open fun isNotLogged(): Boolean {
        return false
    }

    /**
     * @return данные запроса, подлежащие логированию. Null, если запрос не логируется.
     * Если часть подлежащих логированию данных находится в ответе на запрос, необходимо установить
     * [BaseLogRequestData.ISourceRequestData.updateFromResponse]
     */
    open fun getLoggedData(): LogRequestData? {
        return null
    }

    /**
     * @param host   базованя часть url [String]
     * @param port   порт [String]
     * @param method наименование метода [String]
     * @return абсолютный сформированный урл [String]
     */
    open fun url(host: String, port: String, version: String?, method: String, useHttps: Boolean): String {
        val protocol = if (useHttps) "https" else "http"
        return if (version != null) {
            String.format(Locale.getDefault(), URL_MASK_VERSION, protocol, host, port, version, method)
        } else {
            String.format(Locale.getDefault(), URL_MASK_WITHOUT_VERSION, protocol, host, port, method)
        }
    }

    /**
     * @return дополнительный hashcode, дописываемый на каждый ретрофитовский вызов
     * в исходный JsonObject; включает время, т.к. обычный hash + path может совпадать с тем, что уже добавлен в мап
     */
    @JvmOverloads
    fun getMapRequestHash(time: Long = System.currentTimeMillis()): String =
            with(getHashString()) {
                String.format(MAP_REQUEST_HASH_FORMAT, if (this.isEmpty()) hashCode() else this, time.toString())
            }

    fun getPath() = getVersion() + "/" + getMethod()

    companion object {

        const val URL_MASK_VERSION = "%s://%s:%s/%s/%s"
        const val URL_MASK_WITHOUT_VERSION = "%s://%s:%s/%s"
        const val MASK_METHOD = "%s/%s"


        /**
         * Максимальный таймаут запросов [ApiRequest] 25 секунд
         * <br></br>
         * если серверсайд ЕКМП не получил ответ от РЖД,
         * он вернет ошибку код [ErrorCode.BAD_GATEWAY]
         */
        const val FULL_REQUEST_TIMEOUT = 25 * 1000

        /**
         * Таймаут запроса [ApiRequest] по умолчанию
         */
        val DEFAULT_REQUEST_TIMEOUT_IN_MILLS = FULL_REQUEST_TIMEOUT
    }
}