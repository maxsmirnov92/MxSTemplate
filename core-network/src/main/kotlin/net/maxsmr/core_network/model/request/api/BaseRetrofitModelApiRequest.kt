package net.maxsmr.core_network.model.request.api

import net.maxsmr.commonutils.model.gson.toJsonString
import net.maxsmr.core_network.di.networkComponent
import net.maxsmr.core_network.model.request.log.BaseLogRequestData

/**
 * Этот тип должен быть во всех вызываемых ретрофитовских API-методах,
 * регистрироваться в маппере и одновременно является телом запроса
 */
abstract class BaseRetrofitModelApiRequest<LogRequestData : BaseLogRequestData.ISourceRequestData> :
        BaseApiRequest<String, LogRequestData>() {

    val url: String
        get() = with(networkComponent.hostManager()) {
            url(getHost(), getPort(), getVersion(), getMethod(), useHttps())
        }

    // gson не объявлять филдом
    override fun getBody(): String = toJsonString(networkComponent.gson(), this)
}