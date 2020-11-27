package net.maxsmr.core_network.utils

import android.text.TextUtils
import net.maxsmr.commonutils.data.*
import net.maxsmr.commonutils.data.text.EMPTY_STRING
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder.throwRuntimeException
import okhttp3.*
import okio.Buffer
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

@Throws(RuntimeException::class)
fun executeCall(okHttpClient: OkHttpClient, requestConfigurator: (Request.Builder) -> Any?): Response {
    val request = Request.Builder()
    requestConfigurator(request)
    return try {
        okHttpClient.newCall(request.build()).execute()
    } catch (e: Exception) {
        throw RuntimeException("Request execute failed", e)
    }
}

fun getPath(request: Request): String =
    TextUtils.join("/", request.url().pathSegments())

@Throws(RuntimeException::class)
fun requestBodyToString(request: Request): String {
    try {
        val copy = request.newBuilder().build()
        val buffer = Buffer()
        copy.body()?.writeTo(buffer)
        return buffer.readUtf8() ?: EMPTY_STRING
    } catch (e: IOException) {
        throw RuntimeException("Cannot convert request body to string", e)
    }
}

@Throws(RuntimeException::class)
fun responseBodyToByteArray(response: Response?, previousDownloadedSize: Long? = null): ByteArray {
    val responseBody = response?.body() ?: throw NullPointerException("responseBody is null")
    skipBytesIfSupported(response, previousDownloadedSize)
    try {
        return responseBody.bytes()
    } catch (e: IOException) {
        throw RuntimeException("Cannot convert response body to bytes", e)
    }
}

@Throws(RuntimeException::class)
fun responseBodyToString(response: Response?, previousDownloadedSize: Long? = null): String {
    val responseBody = response?.body() ?: throw NullPointerException("responseBody is null")
    skipBytesIfSupported(response, previousDownloadedSize)
    try {
        return responseBody.string() ?: EMPTY_STRING
    } catch (e: IOException) {
        throw RuntimeException("Cannot convert response body to string", e)
    }
}

/**
 * @param previousDownloadedSize кол-во ранее загруженных байт
 */
@Throws(RuntimeException::class)
fun responseBodyToOutputStream(
    response: Response?,
    outputStream: OutputStream?,
    previousDownloadedSize: Long? = null,
    notifier: IStreamNotifier? = null
): ResponseBody {
    if (outputStream == null) {
        throw NullPointerException("outputStream is null")
    }
    skipBytesIfSupported(response, previousDownloadedSize)
    val responseBody = response?.body() ?: throw NullPointerException("responseBody is null")
    val contentLength = responseBody.contentLength()
    try {
        val inputStream = responseBody.byteStream()
        copyStream(inputStream, outputStream,
            if (notifier != null) {
                object : IStreamNotifier {
                    override fun onProcessing(
                        inputStream: InputStream,
                        outputStream: OutputStream,
                        bytesWrite: Long,
                        bytesLeft: Long
                    ): Boolean {
                        return notifier.onProcessing(
                            inputStream,
                            outputStream,
                            bytesWrite,
                            if (contentLength != -1L && contentLength > bytesWrite) {
                                contentLength - bytesWrite
                            } else {
                                bytesLeft
                            }
                        )
                    }
                }
            } else {
                null
            })
    } catch (e: IOException) {
        throw RuntimeException("Cannot copy response body to outputStream", e)
    }
    return responseBody
}

fun isResumeDownloadSupported(response: Response?): Boolean {
    val acceptHeader = response?.header("Accept-Ranges") ?: EMPTY_STRING
    return acceptHeader.isNotEmpty() && !acceptHeader.equals("none", ignoreCase = true)
}

fun headersToMap(headers: Headers?): Map<String, String> {
    val result = mutableMapOf<String, String>()
    if (headers != null) {
        for (i in 0 until headers.size()) {
            val name = headers.name(i)
            result[name] = headers.get(name) ?: EMPTY_STRING
        }
    }
    return result
}

fun isResponseOk(responseCode: Int): Boolean =
    responseCode in 200..299

@Throws(RuntimeException::class)
private fun skipBytesIfSupported(response: Response?, downloadedSize: Long?) {
    if (downloadedSize != null && downloadedSize > 0) {
        val responseBody = response?.body()
        val source = responseBody?.source() ?: throw RuntimeException("response body source is null")
        if (isResumeDownloadSupported(response)) {
            try {
                source.skip(downloadedSize)
            } catch (e: IOException) {
                throwRuntimeException(e, "skip")
            }
        }
    }
}
