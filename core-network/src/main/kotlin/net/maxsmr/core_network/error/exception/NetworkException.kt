package net.maxsmr.core_network.error.exception

/**
 * Базовый класс для всех ошибок, возникающих при работе с сервером
 */
abstract class NetworkException(
        message: String?, cause: Throwable?
) : RuntimeException(message, cause)