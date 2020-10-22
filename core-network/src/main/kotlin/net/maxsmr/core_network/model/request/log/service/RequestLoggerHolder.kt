package net.maxsmr.core_network.model.request.log.service

import net.maxsmr.core_network.model.request.log.BaseLogRequestData
import net.maxsmr.core_network.model.request.log.service.params.LogSendParams

/**
 * Держит ссылку на [RequestLogger], инициализируется в целевом [Application]
 */
object RequestLoggerHolder {

    lateinit var requestLogger: RequestLogger<LogSendParams,
            BaseLogRequestData.ISourceRequestData,
            BaseLogRequestData<BaseLogRequestData.ISourceRequestData,
                    BaseLogRequestData.IResponseData>>
}