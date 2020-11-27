package net.maxsmr.mxstemplate.feature.download.info

import com.google.gson.*
import net.maxsmr.commonutils.data.gson.fromJsonObjectString
import net.maxsmr.commonutils.data.gson.getJsonElementAs
import net.maxsmr.commonutils.data.gson.getJsonPrimitive
import net.maxsmr.commonutils.data.gson.toJsonString
import net.maxsmr.commonutils.data.states.Status
import java.lang.reflect.Type

private const val KEY_DOWNLOAD_INFO = "downloadInfo"
private const val KEY_STATUS = "status"

class DownloadInfoTypeAdapter(private val gson: Gson) : JsonSerializer<Map<DownloadInfo, Status>>,
    JsonDeserializer<Map<DownloadInfo, Status>> {

    override fun serialize(src: Map<DownloadInfo, Status>?, typeOfSrc: Type?, context: JsonSerializationContext?): JsonElement = src?.let {
        JsonArray().apply {
            src.forEach {
                val jsonObject = JsonObject()
                jsonObject.addProperty(KEY_DOWNLOAD_INFO, toJsonString(gson, it.key))
                jsonObject.addProperty(KEY_STATUS, it.value.ordinal)
                add(jsonObject)
            }
        }
    } ?: JsonNull.INSTANCE

    override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?): Map<DownloadInfo, Status> {
        val result = mutableMapOf<DownloadInfo, Status>()
        if (json is JsonArray) {
            for (i in 0 until json.size()) {
                val jsonObject = getJsonElementAs(json.get(i), JsonObject::class.java)
                val downloadInfo = fromJsonObjectString(
                    gson,
                    getJsonPrimitive(jsonObject, KEY_DOWNLOAD_INFO, String::class.java),
                    DownloadInfo::class.java
                )
                val status = getJsonPrimitive(jsonObject, KEY_STATUS, Int::class.java)?.let { ordinal ->
                        Status.values().find { it.ordinal == ordinal }
                }
                if (downloadInfo != null && status != null) {
                    result[downloadInfo] = status
                }
            }
        }
        return result
    }
}