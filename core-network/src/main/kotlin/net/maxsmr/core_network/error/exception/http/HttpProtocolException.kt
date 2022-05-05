package net.maxsmr.core_network.error.exception.http

import android.text.TextUtils
import com.google.gson.Gson
import net.maxsmr.commonutils.text.EMPTY_STRING
import net.maxsmr.core_network.error.NO_ERROR
import net.maxsmr.core_network.error.exception.NetworkException
import net.maxsmr.core_network.error.exception.converters.BaseErrorResponseConverter
import net.maxsmr.core_network.model.response.ResponseObj
import net.maxsmr.core_network.utils.copyBodyToString
import net.maxsmr.core_network.utils.headersToMap
import net.maxsmr.core_network.utils.isResponseOk
import okhttp3.Response
import retrofit2.HttpException

/**
 * Базовая ошибка при получении ответа не 2xx с разобранными полями ответа;
 * может содержать в себе исходный [HttpException];
 * не бросается в чистом виде, идёт в составе [BaseWrappedHttpException]
 */
open class HttpProtocolException(
    val url: String = EMPTY_STRING,
    val method: String = EMPTY_STRING,
    val headers: Map<String, String> = mapOf(),
    val httpCode: Int = HTTP_ERROR_CODE_UNKNOWN,
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

    private constructor(builder: InnerResponseBuilder<out ResponseObj<*>>) : this(
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

    private constructor(builder: Builder) : this(
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

    open class Builder(
        val cause: Throwable?,
        rawResponse: Response?
    ) {

        var httpCode: Int = rawResponse?.code ?: HTTP_ERROR_CODE_UNKNOWN
        var httpMessage: String = rawResponse?.message.orEmpty()

        val url: String
        val method: String
        val headers: Map<String, String>

        val requestBodyString: String
        val errorBodyString: String

        init {
            val request = rawResponse?.request
            url = request?.url?.toString().orEmpty()
            method = request?.method.orEmpty()
            headers = request?.headers.headersToMap()
            requestBodyString = request.copyBodyToString().orEmpty()
            errorBodyString = if (!isResponseOk(httpCode)) {
                rawResponse.copyBodyToString()?.first.orEmpty()
            } else {
                EMPTY_STRING
            }
        }

        open fun build() = HttpProtocolException(this)
    }

    /**
     * Билдер для создания исключений
     * типа [HttpProtocolException] и его наследников
     * @param converter конкретный [BaseErrorResponseConverter] для преобразования тела
     * ошибочного респонса в указанную сущность [Response]
     */
    open class InnerResponseBuilder<Response : ResponseObj<*>>(
        cause: HttpException? = null,
        converter: BaseErrorResponseConverter<Response>? = null,
        gson: Gson? = null
    ) : Builder(
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

    companion object {

        const val HTTP_ERROR_CODE_UNKNOWN = -1

        fun prepareMessage(cause: Throwable?, vararg parts: String): String {
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
