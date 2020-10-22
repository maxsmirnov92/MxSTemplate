package net.maxsmr.core_network.error.exception.inner

import net.maxsmr.core_network.error.exception.NetworkException
import net.maxsmr.core_network.model.request.api.BaseRetrofitModelApiRequest

/**
 * Бросается, когда в маппере не был найден запрашиваемый реквест
 * (пока только из ApiRepository, при использовании handleData)
 * @param request который не был найден
 */
class NoRequestFoundException(
        val request: BaseRetrofitModelApiRequest<*>,
        message: String? = null,
        cause: Throwable? = null
): NetworkException(message, cause)