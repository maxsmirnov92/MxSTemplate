package net.maxsmr.core_network.error.exception

class NoInternetException(
        message: String? = null,
        cause: Throwable? = null
) : NetworkException(message, cause)