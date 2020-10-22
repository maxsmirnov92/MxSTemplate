package net.maxsmr.core_network.error.exception.http

import android.util.Log
import com.google.gson.Gson
import net.maxsmr.commonutils.data.text.EMPTY_STRING
import retrofit2.Call
import retrofit2.HttpException
import net.maxsmr.core_network.error.HttpErrorCode
import net.maxsmr.core_network.error.NO_ERROR
import net.maxsmr.core_network.utils.headersToMap
import net.maxsmr.core_network.utils.requestBodyToString
import net.maxsmr.core_network.error.exception.NetworkException
import net.maxsmr.core_network.error.exception.converters.BaseErrorResponseConverter
import net.maxsmr.core_network.model.response.ResponseObj
import java.io.IOException

const val HTTP_CODE_SUCCESS = 200

/**
 * Базовая ошибка при получении ответа не 2xx с разобранными полями ответа;
 * может содержать в себе исходный [HttpException];
 * не бросается в чистом виде, идёт в составе [BaseWrappedHttpException]
 */
open class HttpProtocolException(
        message: String?,
        cause: Throwable?
) : NetworkException(message, cause) {

    var url: String = EMPTY_STRING
    var method: String = EMPTY_STRING
    var headers: Map<String, String> = mapOf()
    var httpCode: Int = HttpErrorCode.UNKNOWN.code
    var httpMessage: String = EMPTY_STRING
    /**
     * дополнительный внутренний код сервера
     */
    var innerCode: Int = NO_ERROR
    var serverMessage: String = EMPTY_STRING

    var requestBodyString: String = EMPTY_STRING
    var errorBodyString: String = EMPTY_STRING

    private constructor(builder: Builder<out HttpProtocolException, out ResponseObj<*>>) :
            this(prepareMessage(builder.httpMessage, builder.httpCode, builder.url, builder.serverMessage, builder.innerCode), builder.cause) {
        url = builder.url
        method = builder.method
        headers = builder.headers
        httpCode = builder.httpCode
        httpMessage = builder.httpMessage
        innerCode = builder.innerCode
        errorBodyString = builder.errorBodyString
        serverMessage = builder.serverMessage
        requestBodyString = builder.requestBodyString
    }

    /**
     * конструктор копирования из [HttpProtocolException]
     */
    constructor(source: HttpProtocolException) :
            this(prepareMessage(source.httpMessage, source.httpCode, source.url, source.serverMessage, source.innerCode), source.cause) {
        url = source.url
        method = source.method
        headers = source.headers
        httpCode = source.httpCode
        httpMessage = source.httpMessage
        innerCode = source.innerCode
        serverMessage = source.serverMessage
        requestBodyString = source.requestBodyString
        errorBodyString = source.errorBodyString
    }

    fun getHttpErrorCode() = HttpErrorCode.from(httpCode)

    override fun toString(): String {
        return "HttpProtocolException(url='$url', method='$method', headers=$headers, httpCode=$httpCode, httpMessage='$httpMessage', innerCode=$innerCode, serverMessage='$serverMessage', requestBodyString='$requestBodyString', errorBodyString='$errorBodyString')"
    }

    /**
     * Билдер для создания исключений
     * типа [HttpProtocolException] и его наследников
     * @param converter конкретный [BaseErrorResponseConverter] для преобразования тела
     * ошибочного респонса в указанную сущность [Response]
     */
    open class Builder<E : HttpProtocolException, Response : ResponseObj<*>>(
        val cause: HttpException? = null,
        call: Call<*>,
        converter: BaseErrorResponseConverter<Response>? = null,
        gson: Gson? = null
    ) {

        val url: String
        val method: String
        val headers: Map<String, String>
        var httpCode: Int
        var httpMessage: String

        var innerCode: Int
        var serverMessage: String

        val requestBodyString: String
        val errorBodyString: String

        init {

            val rawResponse = cause?.response()

            val request = rawResponse?.raw()?.request()
            url = request?.url()?.toString() ?: EMPTY_STRING
            method = request?.method() ?: EMPTY_STRING
            headers = headersToMap(request?.headers())

            requestBodyString = requestBodyToString(call.request())

            val responseBody = rawResponse?.errorBody()

            errorBodyString = responseBody?.let {
                try {
                    it.string()
                } catch (e: IOException) {
                    Log.e("HttpProtocolException", "An IOException occurred during string(): $e")
                    null
                }
            } ?: EMPTY_STRING

            val response: Response? = if (gson != null) converter?.convert(gson, url, errorBodyString) else null

            if (response != null) {
                response.errorCode.let { errorCode ->
                    if (errorCode != null && errorCode != 0) {
                        innerCode = errorCode
                        serverMessage = response.errorMessage ?: EMPTY_STRING
                    } else {
                        innerCode = NO_ERROR
                        serverMessage = EMPTY_STRING
                    }
                }
            } else {
                innerCode = NO_ERROR
                serverMessage = EMPTY_STRING
            }

            this.httpMessage = cause?.message() ?: EMPTY_STRING
            this.httpCode = cause?.code() ?: HTTP_CODE_SUCCESS
        }

        fun setHttpMessage(message: String): Builder<E, Response> {
            this.httpMessage = message
            return this
        }

        fun setHttpCode(code: Int): Builder<E, Response> {
            this.httpCode = code
            return this
        }

        fun setInnerCode(code: Int): Builder<E, Response> {
            this.innerCode = code
            return this
        }

        fun setServerMessage(message: String): Builder<E, Response> {
            this.serverMessage = message
            return this
        }

        open fun build() = HttpProtocolException(this)
    }

    companion object {

        private fun prepareMessage(httpMessage: String, code: Int, url: String, developerMessage: String?, innerCode: Int): String {
            return " httpCode=" + code + "\n" +
                    ", httpMessage='" + httpMessage + "'" +
                    ", url='" + url + "'" + "\n" +
                    ", innerCode=" + innerCode +
                    ", serverMessage='" + developerMessage + "'"
        }
    }
}
