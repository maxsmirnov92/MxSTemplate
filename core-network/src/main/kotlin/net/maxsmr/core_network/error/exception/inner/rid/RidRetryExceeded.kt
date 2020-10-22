package net.maxsmr.core_network.error.exception.inner.rid

import net.maxsmr.commonutils.data.text.EMPTY_STRING
import net.maxsmr.core_network.error.exception.NetworkException

class RidRetryExceeded(
        val count: Int,
        val limit: Int,
        message: String = EMPTY_STRING,
        cause: Throwable? = null
): NetworkException(message, cause)