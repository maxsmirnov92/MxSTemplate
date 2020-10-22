package net.maxsmr.core_network.error.handler.error.http

import net.maxsmr.core_network.error.exception.http.HttpProtocolException

/**
 * Базовый класс обработки ошибок сервера на уровне http;
 * Применяется в [CallAdapterFactory]
 */
interface BaseHttpErrorHandler {

    fun handle(e: HttpProtocolException)
}
