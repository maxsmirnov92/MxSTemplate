package net.maxsmr.core_common.arch;

/**
 * Интерфейс обработчика ошибок
 */
public interface ErrorHandler {

    void handleError(Throwable err);
}
