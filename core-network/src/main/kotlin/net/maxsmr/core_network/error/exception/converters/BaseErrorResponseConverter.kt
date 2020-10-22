package net.maxsmr.core_network.error.exception.converters

import com.google.gson.Gson
import net.maxsmr.core_network.model.response.ResponseObj

/**
 * Базовый конвертер тела ошибки в указанный [Response]
 */
interface BaseErrorResponseConverter<Response : ResponseObj<*>> {

    fun convert(gson: Gson, url: String, errorBodyString: String): Response?
}