package net.maxsmr.core_network.error.handler.error;

/**
 * Интерфейс обработчика ошибок
 */
public interface ErrorHandler {

    void handleError(Throwable err);
}
