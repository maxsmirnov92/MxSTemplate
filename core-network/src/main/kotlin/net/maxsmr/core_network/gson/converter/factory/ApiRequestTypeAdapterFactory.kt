package net.maxsmr.core_network.gson.converter.factory

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import net.maxsmr.core_network.model.request.api.IApiMapper
import net.maxsmr.core_network.model.request.api.BaseRetrofitModelApiRequest

const val FIELD_API_ORIGINAL_BODY = "API_ORIGINAL_BODY"
const val FIELD_API_REQUEST_HASH = "API_REQUEST_HASH"

/**
 * Дописываниет hash в json запроса
 * для его дальнейшего извлечения при поиске в [IApiMapper]
 */
class ApiRequestTypeAdapterFactory(
    val apiMapper: IApiMapper,
    shouldConsumeDocument: Boolean
) : BaseCustomizedTypeAdapterFactory<BaseRetrofitModelApiRequest<*>>(BaseRetrofitModelApiRequest::class.java, shouldConsumeDocument) {

    override fun onWrite(source: BaseRetrofitModelApiRequest<*>, toSerialize: JsonElement): JsonElement {
        super.onWrite(source, toSerialize)
        val wrapped = JsonObject()
        // далее при необходимости изменения исходного тела обращаемся к FIELD_API_ORIGINAL_BODY в составе корневого объекта
        wrapped.add(FIELD_API_ORIGINAL_BODY, toSerialize)
            IApiMapper.findApiRequestKeyInfo(apiMapper.map, source)?.let {
                // исходный реквест ранее был зарегистрирован
                wrapped.add(FIELD_API_REQUEST_HASH, JsonPrimitive(it.hash))
            }
        return wrapped
    }
}