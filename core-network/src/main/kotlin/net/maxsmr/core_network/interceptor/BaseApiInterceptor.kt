package net.maxsmr.core_network.interceptor

import net.maxsmr.commonutils.model.isJsonFieldJson
import net.maxsmr.core_network.gson.converter.factory.FIELD_API_ORIGINAL_BODY
import net.maxsmr.core_network.model.request.api.IApiMapper
import net.maxsmr.core_network.utils.requestBodyToString
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONException
import org.json.JSONObject

/**
 * Базовый интерцептор с обёрткой для proceed
 * @param apiMap информация о зарегистрированных [BaseApiRequest]
 * @param proceedOriginalRequest нужно ли отдать оригинальное тело из [FIELD_API_ORIGINAL_BODY]
 */
abstract class BaseApiInterceptor(
    protected val apiMap: Map<IApiMapper.ApiKeyInfo, IApiMapper.ApiValueInfo>,
    protected val proceedOriginalRequest: Boolean
) : Interceptor {

    protected fun proceed(chain: Interceptor.Chain, originalRequest: Request): Response {
        var request = originalRequest
        if (proceedOriginalRequest) {
            // если это последний в цепочке, то отдаём оригинальный json, если есть
            val body = requestBodyToString(request)

            if (body.isNotEmpty()) {
                var wrappedJsonBody: JSONObject? = null
                try {
                    wrappedJsonBody = JSONObject(body)
                } catch (e: JSONException) {
                    // не является JSONObject
                }

                wrappedJsonBody?.let {
                    if (isJsonFieldJson(wrappedJsonBody, FIELD_API_ORIGINAL_BODY)) {
                        request = request.newBuilder()
                            .method(
                                request.method,
                                wrappedJsonBody.get(FIELD_API_ORIGINAL_BODY).toString()
                                    .toRequestBody("application/json".toMediaTypeOrNull())
                            )
                            .build()
                    }
                }
            }
        }
        return chain.proceed(request)
    }
}