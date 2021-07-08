package net.maxsmr.core_network.interceptor

import android.util.Log
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response

private const val LOG_TAG = "FailRetryInterceptor"

class FailRetryInterceptor(
        val maxTries: Int
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request: Request = chain.request()

        // try the request
        var response = chain.proceed(request)

        var retryCount = 0

        while (!response.isSuccessful && maxTries > 0 && retryCount < maxTries) {
            retryCount++
            Log.d(LOG_TAG, "Request failed (code: ${response.code}), next retry: $retryCount/$maxTries")

            // retry the request
            response = chain.proceed(request)
        }

        // otherwise just pass the original response on
        return response
    }
}