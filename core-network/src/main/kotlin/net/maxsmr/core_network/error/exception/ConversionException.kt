package net.maxsmr.core_network.error.exception

open class ConversionException(
        message: String? = null,
        cause: Throwable? = null
) : NetworkException(message, cause)