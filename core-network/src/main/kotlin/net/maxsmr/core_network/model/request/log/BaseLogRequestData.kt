package net.maxsmr.core_network.model.request.log

import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

/**
 * Базовый класс с данными запроса и ответа на него для логирования
 */
abstract class BaseLogRequestData<Source : BaseLogRequestData.ISourceRequestData, Response : BaseLogRequestData.IResponseData> {

    abstract fun requestData(): Source?

    abstract fun responseData(): Response?

    abstract fun id(): Long

    abstract fun toJson(): JSONObject

    /**
     * Маркер для данных исходного, логируемого запроса
     */
    interface ISourceRequestData

    /**
     * Маркер для данных ответа на исходный, логируемый запрос
     */
    interface IResponseData

    companion object {

        fun headersFrom(map: Map<String, String>): String {
            var builder = StringBuilder()
            val iterator = map.keys.iterator()
            while (iterator.hasNext()) {
                val hasNext = iterator.hasNext()
                val key = iterator.next()
                builder = builder.append(key)
                        .append(": ")
                        .append(map[key])
                if (hasNext) {
                    builder = builder.append("\n")
                }
            }
            return builder.toString()
        }

        fun toIds(data: Collection<BaseLogRequestData<*, *>>): List<Long> {
            val result = mutableListOf<Long>()
            data.forEach {
                result.add(it.id())
            }
            return result
        }

        fun formatTime(time: Long) = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.sssZ", Locale.getDefault()).format(Date(time))
    }
}
