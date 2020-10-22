package net.maxsmr.core_network.interceptor

import android.util.Log
import net.maxsmr.core_common.PLATFORM_NAME
import net.maxsmr.core_network.gson.converter.factory.FIELD_API_ORIGINAL_BODY
import net.maxsmr.core_network.model.request.RequestBodyType
import net.maxsmr.core_network.model.request.api.IApiMapper
import net.maxsmr.core_network.utils.appendServiceFields
import net.maxsmr.core_network.utils.getPath
import net.maxsmr.core_network.utils.requestBodyToString
import okhttp3.*
import org.json.JSONException
import org.json.JSONObject import java.io.IOException

private const val LOG_TAG = "ServiceInterceptor"

/**
 * добавляет необходимые для каждого запроса параметры, такие как token
 */
class ServiceInterceptor constructor(
    val versionName: String,
    apiMap: Map<IApiMapper.ApiKeyInfo, IApiMapper.ApiValueInfo>,
    proceedOriginalRequest: Boolean
) : BaseApiInterceptor(apiMap, proceedOriginalRequest) {

    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {

        val originalRequest = chain.request()

        var request: Request = originalRequest

        val path = getPath(request)
        val body = requestBodyToString(request)

        val apiRequestPair = IApiMapper.findApiRequestByOriginalRequestBody(apiMap, body, path)
        val apiRequest = apiRequestPair.second?.request
        if (apiRequest == null) {
            Log.w(LOG_TAG, "No request found in mapper for path $path and hash ${apiRequestPair.first}!")
        } else {
            if (apiRequest.getRequestBodyType() == RequestBodyType.JSON) {

                if (body.isNotEmpty()) {

                    var wrappedJsonBody: JSONObject? = null

                    try {
                        wrappedJsonBody = JSONObject(body)
                    } catch (e: JSONException) {
                        // не является JSONObject
                    }

                    wrappedJsonBody?.let {

                        // исходное тело запроса в JSON-формате
                        var jsonBody: JSONObject? = wrappedJsonBody.optJSONObject(FIELD_API_ORIGINAL_BODY)

                        if (jsonBody != null) {
                            jsonBody = appendServiceFields(apiRequest, jsonBody, versionName, PLATFORM_NAME)
                            if (jsonBody != null) {
                                // вероятно, это тот же объект, но на всякий случай put
                                wrappedJsonBody.put(FIELD_API_ORIGINAL_BODY, jsonBody)
                                // изменённый реквест для продолжения цепочки
                                request = originalRequest.newBuilder()
                                        .method(originalRequest.method(),
                                                RequestBody.create(MediaType.parse("application/json"), wrappedJsonBody.toString().trim()))
                                        .build()
                            }
                        }
                    }

                }
            }
        }
        return proceed(chain, request)
    }
}