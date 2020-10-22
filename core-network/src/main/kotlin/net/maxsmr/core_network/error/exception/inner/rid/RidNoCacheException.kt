package net.maxsmr.core_network.error.exception.inner.rid

import net.maxsmr.core_network.error.exception.NetworkException
import net.maxsmr.core_network.model.request.SimpleRequestInfo

/**
 * Возникает при отсутствии [BaseRetrofitAsyncApiRequest] в кеше
 */
class RidNoCacheException(
        val info: SimpleRequestInfo,
        message: String?
) : NetworkException(message, null)