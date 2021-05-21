package net.maxsmr.core_network.error.exception

class CancelledRuntimeException(
    message: String?,
    cause: Throwable?
): RuntimeException(message, cause)