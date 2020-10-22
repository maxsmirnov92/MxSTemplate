package net.maxsmr.core_network.utils

import android.text.TextUtils
import net.maxsmr.commonutils.data.StreamUtils.revectorStream
import net.maxsmr.commonutils.data.text.EMPTY_STRING
import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okio.Buffer
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream

fun executeCall(okHttpClient: OkHttpClient, requestConfigurator: (Request.Builder) -> Unit): Response {
    val request = Request.Builder()
    requestConfigurator(request)
    return okHttpClient.newCall(request.build()).execute()
}

fun getPath(request: Request): String =
        TextUtils.join("/", request.url().pathSegments())

fun requestBodyToString(request: Request): String {
    try {
        val copy = request.newBuilder().build()
        val buffer = Buffer()
        copy.body()?.writeTo(buffer)
        return buffer.readUtf8() ?: EMPTY_STRING
    } catch (e: IOException) {
        throw RuntimeException("Cannot convert request body to string")
    }
}

fun responseBodyToString(response: Response?): String {
    try {
        return response?.body()?.string() ?: EMPTY_STRING
    } catch (e: IOException) {
        throw RuntimeException("Cannot convert response body to string")
    }
}

fun responseBodyToOutputStream(response: Response?, outputStream: OutputStream?) {
    try {
        revectorStream(response?.body()?.byteStream(), outputStream)
    } catch (e : IOException) {
        throw RuntimeException("Cannot revector response body to outputStream")
    }
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