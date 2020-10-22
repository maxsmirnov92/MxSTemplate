package net.maxsmr.core_network.model.request.api

import net.maxsmr.core_network.model.request.log.BaseLogRequestData

/**
 * Используется для запросов, требующих пустое тело;
 * в которых дефолтные template-методы
 */
data class EmptyRequest(private val methodName: String) : BaseRetrofitModelApiRequest<BaseLogRequestData.ISourceRequestData>() {

    override fun getMethod(): String = methodName
}