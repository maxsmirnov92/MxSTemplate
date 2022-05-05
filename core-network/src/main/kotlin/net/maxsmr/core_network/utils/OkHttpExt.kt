package net.maxsmr.core_network.utils

import android.text.TextUtils
import kotlinx.coroutines.suspendCancellableCoroutine
import net.maxsmr.commonutils.IStreamNotifier
import net.maxsmr.commonutils.copyStreamOrThrow
import net.maxsmr.commonutils.logger.BaseLogger
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder.Companion.formatException
import net.maxsmr.commonutils.text.charsetForNameOrNull
import net.maxsmr.core_network.error.exception.http.HttpProtocolException
import okhttp3.*
import okio.Buffer
import okio.BufferedSource
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.lang.Exception
import java.nio.charset.Charset
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

private val logger = BaseLoggerHolder.instance.getLogger<BaseLogger>("OkHttpExt")

fun OkHttpClient.executeCall(
    requestConfigurator: ((Request.Builder) -> Any?),
): Response? = try {
    executeCallOrThrow(requestConfigurator)
} catch (e: Exception) {
    logger.e( formatException(e, "Execute call"))
    null
}

@Throws(Exception::class)
fun OkHttpClient.executeCallOrThrow(
    requestConfigurator: ((Request.Builder) -> Any?),
): Response {
    val request = Request.Builder()
    requestConfigurator.invoke(request)
    return newCall(request.build()).execute()
}

suspend fun OkHttpClient.newCallSuspended(request: Request): Response = suspendCancellableCoroutine { continuation ->
    val call = newCall(request)
    continuation.invokeOnCancellation {
        call.cancel()
    }
    call.enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            continuation.resumeWithException(RuntimeException("Request failed", e))
        }

        override fun onResponse(call: Call, response: Response) {
            if (response.isSuccessful) {
                continuation.resume(response)
            } else {
                continuation.resumeWithException(HttpProtocolException.Builder(null, response).build())
            }
        }
    })
}

// region: Request

fun Request?.path(): String? =
    this?.let { TextUtils.join("/", it.url.pathSegments) }

fun Request?.copyBodyToString(charset: Charset = Charset.defaultCharset()): String? {
    return try {
        copyBodyToStringOrThrow(charset)
    } catch (e: IOException) {
        logger.e( formatException(e, "Read request body to String"))
        null
    }
}

@JvmOverloads
@Throws(IOException::class)
fun Request?.copyBodyToStringOrThrow(charset: Charset = Charset.defaultCharset()): String? {
    this ?: return null
    val copy = newBuilder().build()
    val buffer = Buffer()
    copy.body?.writeTo(buffer)
    return buffer.readString(charset)
}

// endregion

// region Response: READ

fun Response?.readBodyToByteArray(previousDownloadedSize: Long? = null): ByteArray? = try {
    readBodyToByteArrayOrThrow(previousDownloadedSize)
} catch (e: IOException) {
    logger.e( formatException(e, "Read response body to ByteArray"))
    null
}

@Throws(IOException::class)
fun Response?.readBodyToByteArrayOrThrow(previousDownloadedSize: Long? = null): ByteArray? {
    skipBytesIfSupportedOrThrow(previousDownloadedSize)
    return this?.body?.bytes()
}

fun Response?.readBodyToString(previousDownloadedSize: Long? = null): String? = try {
    readBodyToStringOrThrow(previousDownloadedSize)
} catch (e: IOException) {
    logger.e( formatException(e, "Read response body to String"))
    null
}

@Throws(IOException::class)
fun Response?.readBodyToStringOrThrow(previousDownloadedSize: Long? = null): String? {
    skipBytesIfSupportedOrThrow(previousDownloadedSize)
    return this?.body?.string()
}

fun Response?.readBodyToOutputStream(
    outputStream: OutputStream?,
    previousDownloadedSize: Long? = null,
    notifier: IStreamNotifier? = null,
): ResponseBody? = try {
    readBodyToOutputStreamOrThrow(outputStream, previousDownloadedSize, notifier)
} catch (e: IOException) {
    logger.e( formatException(e, "Read response body to OutputStream"))
    null
}

@Throws(IOException::class)
fun Response?.readBodyToOutputStreamOrThrow(
    outputStream: OutputStream?,
    previousDownloadedSize: Long? = null,
    notifier: IStreamNotifier? = null,
): ResponseBody? {
    outputStream ?: return null
    val responseBody = this?.body ?: return null
    skipBytesIfSupportedOrThrow(previousDownloadedSize)
    responseBody.byteStream().copyToOutputStreamOrThrow(outputStream, notifier, responseBody.contentLength())
    return responseBody
}

// endregion

// region Response: COPY

/**
 * Вычитывает тело запроса в массив байт, не изменяя исходный [InputStream]
 */
fun Response?.copyBodyToByteArray(): ByteArray? = try {
    copyBodyToByteArrayOrThrow()
} catch (e: IOException) {
    logger.e( formatException(e, "Copy response body to ByteArray"))
    null
}

@Throws(IOException::class)
fun Response?.copyBodyToByteArrayOrThrow(): ByteArray? {
    this ?: return null
    return cloneBufferOrThrow()?.readByteArray()
}

/**
 * Вычитывает тело запроса в строку, не изменяя исходный [InputStream]
 */
fun Response?.copyBodyToString(): Pair<String, Charset>? = try {
    copyBodyToStringOrThrow()
} catch (e: IOException) {
    logger.e( formatException(e, "Copy response body to String"))
    null
}

@Throws(IOException::class)
fun Response?.copyBodyToStringOrThrow(): Pair<String, Charset>? {
    this ?: return null
    val charset = charsetForNameOrNull(header("Content-Encoding")) ?: Charset.defaultCharset()
    return cloneBufferOrThrow()?.let {
        Pair(it.readString(charset), charset)
    }
}

/**
 * Вычитывает тело запроса в [OutputStream], не изменяя исходный [InputStream]
 */
fun Response?.copyBodyToOutputStream(
    outputStream: OutputStream?,
    notifier: IStreamNotifier? = null,
): ResponseBody? = try {
    copyBodyToOutputStreamOrThrow(outputStream, notifier)
} catch (e: IOException) {
    logger.e( formatException(e, "Copy response body to OutputStream"))
    null
}

@Throws(IOException::class)
fun Response?.copyBodyToOutputStreamOrThrow(
    outputStream: OutputStream?,
    notifier: IStreamNotifier? = null,
): ResponseBody? {
    outputStream ?: return null
    val responseBody = this?.body ?: return null
    val buffer = cloneBufferOrThrow() ?: return null
    buffer.inputStream().copyToOutputStreamOrThrow(outputStream, notifier, responseBody.contentLength())
    return responseBody
}

@Throws(IOException::class)
fun Response.cloneBufferOrThrow(): Buffer? {
    val source: BufferedSource = body?.source() ?: return null
    // request the entire body.
    source.request(Long.MAX_VALUE)
    // clone buffer before reading from it
    return source.buffer.clone()
}

// endregion

fun isResponseOk(responseCode: Int): Boolean = responseCode in 200..299

fun Headers?.headersToMap(): Map<String, String> {
    val result = mutableMapOf<String, String>()
    if (this != null) {
        for (i in 0 until size) {
            val name = name(i)
            result[name] = this[name].orEmpty()
        }
    }
    return result
}

fun Response.isResumeDownloadSupported(): Boolean {
    val acceptHeader = this.header("Accept-Ranges").orEmpty()
    return acceptHeader.isNotEmpty() && !acceptHeader.equals("none", ignoreCase = true)
}

@Throws(IOException::class)
private fun Response?.skipBytesIfSupportedOrThrow(downloadedSize: Long?) {
    this ?: return
    if (downloadedSize != null && downloadedSize > 0) {
        if (isResumeDownloadSupported()) {
            body?.source()?.skip(downloadedSize)
        }
    }
}

@Throws(IOException::class)
private fun InputStream.copyToOutputStreamOrThrow(
    outputStream: OutputStream,
    notifier: IStreamNotifier?,
    contentLength: Long,
) {
    copyStreamOrThrow(outputStream,
        if (notifier != null) {
            object : IStreamNotifier {
                override fun onProcessing(
                    inputStream: InputStream,
                    outputStream: OutputStream,
                    bytesWrite: Long,
                    bytesLeft: Long,
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
}
