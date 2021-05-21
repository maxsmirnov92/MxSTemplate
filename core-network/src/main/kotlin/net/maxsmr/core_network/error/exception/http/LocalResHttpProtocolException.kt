package net.maxsmr.core_network.error.exception.http

import android.net.Uri
import net.maxsmr.commonutils.text.EMPTY_STRING
import net.maxsmr.core_network.error.HttpErrorCode
import net.maxsmr.core_network.error.NO_ERROR
import okhttp3.Response

/**
 * [HttpProtocolException] при наличии временного недокачанного ресурса [localUri]
 */
class LocalResHttpProtocolException(
    val localUri: Uri?,
    url: String = EMPTY_STRING,
    method: String = EMPTY_STRING,
    headers: Map<String, String> = mapOf(),
    httpCode: Int = HttpErrorCode.UNKNOWN.code,
    httpMessage: String = EMPTY_STRING,
    innerCode: Int = NO_ERROR,
    innerMessage: String = EMPTY_STRING,
    requestBodyString: String = EMPTY_STRING,
    errorBodyString: String = EMPTY_STRING,
    message: String?,
    cause: Throwable?
) : HttpProtocolException(
    url,
    method,
    headers,
    httpCode,
    httpMessage,
    innerCode,
    innerMessage,
    requestBodyString,
    errorBodyString,
    message,
    cause
) {

    constructor(
        localUri: Uri?,
        builder: Builder
    ) : this(
        localUri,
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

    class Builder(
        private val localUri: Uri?,
        cause: Throwable?,
        rawResponse: Response?
    ) : HttpProtocolException.RawBuilder(rawResponse, cause) {

        override fun build() = LocalResHttpProtocolException(localUri, this)
    }
}