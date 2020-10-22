package net.maxsmr.core_network.error

/**
 * Примеры кодов специфичных ошибок сервера
 */
enum class HttpErrorCode(val code: Int) {

    NOT_MODIFIED(304),
    BAD_REQUEST(400),
    NOT_AUTHORIZED(401),
    FORBIDDEN(403),
    NOT_FOUND(404),
    INTERNAL_SERVER_ERROR(500),
    UNKNOWN(-1);

    companion object {

        fun from(code: Int): HttpErrorCode =
                values().firstOrNull { error -> error.code == code } ?: UNKNOWN
    }
}