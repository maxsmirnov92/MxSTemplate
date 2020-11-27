package net.maxsmr.core_network.error.exception.http

import android.text.TextUtils
import com.google.gson.Gson
import net.maxsmr.commonutils.data.text.EMPTY_STRING
import retrofit2.HttpException
import net.maxsmr.core_network.error.HttpErrorCode
import net.maxsmr.core_network.error.NO_ERROR
import net.maxsmr.core_network.utils.headersToMap
import net.maxsmr.core_network.utils.requestBodyToString
import net.maxsmr.core_network.error.exception.NetworkException
import net.maxsmr.core_network.error.exception.converters.BaseErrorResponseConverter
import net.maxsmr.core_network.model.response.ResponseObj
import net.maxsmr.core_network.utils.isResponseOk
import net.maxsmr.core_network.utils.responseBodyToString
import okhttp3.Response
import java.lang.RuntimeException

/**
 * Базовая ошибка при получении ответа не 2xx с разобранными полями ответа;
 * может содержать в себе исходный [HttpException];
 * не бросается в чистом виде, идёт в составе [BaseWrappedHttpException]
 */
class HttpProtocolException(
    val url: String = EMPTY_STRING,
    val method: String = EMPTY_STRING,
    val headers: Map<String, String> = mapOf(),
    val httpCode: Int = HttpErrorCode.UNKNOWN.code,
    val httpMessage: String = EMPTY_STRING,
    /**
     * дополнительный внутренний код сервера
     */
    val innerCode: Int = NO_ERROR,
    val innerMessage: String = EMPTY_STRING,

    val requestBodyString: String = EMPTY_STRING,
    val errorBodyString: String = EMPTY_STRING,
    message: String?,
    cause: Throwable?
) : NetworkException(message, cause) {

    val httpErrorCode = HttpErrorCode.from(httpCode)

    private constructor(builder: Builder<out ResponseObj<*>>) : this(
        builder.url,
        builder.method,
        builder.headers,
        builder.httpCode,
        builder.httpMessage,
        builder.innerCode,
        builder.innerMessage,
        builder.requestBodyString,
        builder.errorBodyString,
        prepareMessage(
            builder.cause,
            builder.url,
            builder.method,
            builder.httpMessage,
            builder.httpCode.toString(),
            builder.innerCode.toString(),
            builder.innerMessage,
            builder.errorBodyString
        ),
        builder.cause
    )

    private constructor(builder: RawBuilder) : this(
        builder.url,
        builder.method,
        builder.headers,
        builder.httpCode,
        builder.httpMessage,
        NO_ERROR,
        EMPTY_STRING,
        builder.requestBodyString,
        builder.errorBodyString,
        prepareMessage(
            builder.cause,
            builder.url,
            builder.method,
            builder.httpMessage,
            builder.httpCode.toString(),
            builder.errorBodyString
        ), builder.cause
    )

    /**
     * конструктор копирования из [HttpProtocolException]
     */
    constructor(source: HttpProtocolException) : this(
                source.url,
                source.method,
                source.headers,
                source.httpCode,
                source.httpMessage,
                source.innerCode,
                source.innerMessage,
                source.requestBodyString,
                source.errorBodyString,
                source.message,
                source.cause
            )

    override fun toString(): String {
        return "HttpProtocolException(url='$url'," +
                "method='$method'," +
                "headers=$headers," +
                "httpCode=$httpCode," +
                "httpMessage='$httpMessage'," +
                "innerCode=$innerCode," +
                "innerMessage='$innerMessage'," +
                "requestBodyString='$requestBodyString'," +
                "errorBodyString='$errorBodyString')"
    }

    abstract class BaseBuilder(
        var httpCode: Int,
        var httpMessage: String,
        val cause: Throwable?,
        rawResponse: Response?
    ) {

        val url: String
        val method: String
        val headers: Map<String, String>

        val requestBodyString: String
        val errorBodyString: String

        init {

            val request = rawResponse?.request()
            url = request?.url()?.toString() ?: EMPTY_STRING
            method = request?.method() ?: EMPTY_STRING
            headers = headersToMap(request?.headers())

            requestBodyString = if (request != null) {
                try {
                    requestBodyToString(request)
                } catch (e: RuntimeException) {
                    EMPTY_STRING
                }
            } else {
                EMPTY_STRING
            }

            errorBodyString = if (!isResponseOk(httpCode)) {
                try {
                    responseBodyToString(rawResponse)
                } catch (e: RuntimeException) {
                    EMPTY_STRING
                }
            } else {
                EMPTY_STRING
            }
        }

        abstract fun build(): HttpProtocolException
    }

    /**
     * Билдер для создания исключений
     * типа [HttpProtocolException] и его наследников
     * @param converter конкретный [BaseErrorResponseConverter] для преобразования тела
     * ошибочного респонса в указанную сущность [Response]
     */
    open class Builder<Response : ResponseObj<*>>(
        cause: HttpException? = null,
        converter: BaseErrorResponseConverter<Response>? = null,
        gson: Gson? = null
    ) : BaseBuilder(
        cause?.code() ?: HttpErrorCode.UNKNOWN.code,
        cause?.message() ?: EMPTY_STRING,
        cause,
        cause?.response()?.raw()
    ) {

        var innerCode: Int
        var innerMessage: String

        init {

            val response: Response? =
                if (gson != null) converter?.convert(gson, url, errorBodyString) else null

            if (response != null) {
                response.errorCode.let { errorCode ->
                    if (errorCode != null && errorCode != 0) {
                        innerCode = errorCode
                        innerMessage = response.errorMessage ?: EMPTY_STRING
                    } else {
                        innerCode = NO_ERROR
                        innerMessage = EMPTY_STRING
                    }
                }
            } else {
                innerCode = NO_ERROR
                innerMessage = EMPTY_STRING
            }


        }

        override fun build() = HttpProtocolException(this)
    }

    open class RawBuilder(rawResponse: Response?, cause: Throwable?) : BaseBuilder(
        rawResponse?.code() ?: HttpErrorCode.UNKNOWN.code,
        rawResponse?.message() ?: EMPTY_STRING,
        cause,
        rawResponse,
    ) {

        override fun build() = HttpProtocolException(this)
    }

    companion object {

        private fun prepareMessage(cause: Throwable?, vararg parts: String): String {
            val partsList = mutableListOf<String>()
            cause?.localizedMessage?.let {
                partsList.add(it)
            }
            partsList.addAll(parts)
            return TextUtils.join(", ", partsList.filter {
                it.isNotEmpty()
            })
        }
    }
}
