package net.maxsmr.core_network.error.exception.converters

import com.google.gson.Gson
import net.maxsmr.commonutils.model.gson.fromJsonOrNull
import net.maxsmr.core_network.model.response.ResponseObj

class DefaultErrorResponseConverter : BaseErrorResponseConverter<ResponseObj<*>> {

    override fun convert(gson: Gson, url: String, errorBodyString: String): ResponseObj<*>? {
        return gson.fromJsonOrNull(errorBodyString, ResponseObj::class.java)
    }
}