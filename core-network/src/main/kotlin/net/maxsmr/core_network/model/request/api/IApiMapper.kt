package net.maxsmr.core_network.model.request.api

import net.maxsmr.commonutils.data.text.EMPTY_STRING
import net.maxsmr.core_network.gson.converter.factory.FIELD_API_REQUEST_HASH
import net.maxsmr.core_network.utils.requestBodyToString
import okhttp3.Request
import org.json.JSONObject
import java.io.Serializable

interface IApiMapper {

    val map: MutableMap<ApiKeyInfo, ApiValueInfo>

    data class ApiKeyInfo(
            val hash: String,
            val path: String
    ): Serializable

    data class ApiValueInfo(
        val request: BaseApiRequest<*, *>,
        var shouldRemoveFromMapperAfterComplete: Boolean = false
    ): Serializable

    companion object {

        fun findApiRequest(map: Map<ApiKeyInfo, ApiValueInfo>, keyInfo: ApiKeyInfo) : ApiValueInfo?
                = findApiRequest(map, keyInfo.hash, keyInfo.path)

        fun findApiRequest(map: Map<ApiKeyInfo, ApiValueInfo>, hash: String, path: String): ApiValueInfo? {
            var request: ApiValueInfo? = null
            map.entries.forEach {
                with(it.key) {
                    if (this.path == path &&
                            (hash.isEmpty() || hash == this.hash)) {
                        request = it.value
                        return@forEach
                    }
                }
            }
            return request
        }

        fun findApiRequestByOriginalRequest(map: Map<ApiKeyInfo, ApiValueInfo>, request: Request, path: String): Pair<String, ApiValueInfo?> =
                findApiRequestByOriginalRequestBody(map, requestBodyToString(request), path)

        fun findApiRequestByOriginalRequestBody(map: Map<ApiKeyInfo, ApiValueInfo>, requestBody: String, path: String): Pair<String, ApiValueInfo?> {
            val requestJsonBody = if (requestBody.isNotEmpty()) JSONObject(requestBody) else null
            val hash: String = requestJsonBody?.optString(FIELD_API_REQUEST_HASH) ?: EMPTY_STRING
            val baseApiRequest: ApiValueInfo? = findApiRequest(map, hash, path)
            return Pair(hash, baseApiRequest)
        }

        /**
         * @return информация по исходному [apiRequest]
         */
        fun findApiRequestKeyInfo(map: Map<ApiKeyInfo, ApiValueInfo>, apiRequest: BaseApiRequest<*, *>): ApiKeyInfo? =
                map.entries.find { it.value.request == apiRequest }?.key
    }
}