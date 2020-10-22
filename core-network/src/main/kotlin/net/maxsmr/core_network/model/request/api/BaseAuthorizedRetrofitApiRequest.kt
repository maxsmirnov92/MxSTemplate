package net.maxsmr.core_network.model.request.api

import net.maxsmr.core_network.model.request.log.BaseLogRequestData

abstract class BaseAuthorizedRetrofitApiRequest<LogRequestData: BaseLogRequestData.ISourceRequestData>: BaseRetrofitModelApiRequest<LogRequestData>() {

    override fun isRequireSession(): Boolean {
        return true
    }

    override fun isRequireLanguage(): Boolean {
        return true
    }
}