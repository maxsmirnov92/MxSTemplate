package net.maxsmr.core_network.utils

import net.maxsmr.core_network.model.request.api.BaseApiRequest
import org.json.JSONException
import org.json.JSONObject

/**
 * Добавить сервисные поля к исходному телу запроса
 * @return исходный или новый [JSONObject] на основе существующего
 */
fun appendServiceFields(
    apiRequest: BaseApiRequest<*, *>,
    apiRequestBody: Any?,
    appVersion: String?,
    appPlatform: String?
): JSONObject? {
    var body: JSONObject? = null
    if (apiRequestBody is JSONObject) {
        body = apiRequestBody
    } else if (apiRequestBody != null) {
        try {
            body = JSONObject(apiRequestBody.toString())
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }
    // TODO fields
    return body
}