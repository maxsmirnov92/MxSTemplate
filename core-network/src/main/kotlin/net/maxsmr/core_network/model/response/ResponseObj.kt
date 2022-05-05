package net.maxsmr.core_network.model.response

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import net.maxsmr.commonutils.entity.EmptyValidable
import net.maxsmr.commonutils.model.gson.fromJsonOrNull
import java.lang.reflect.Type

open class ResponseObj<D> : EmptyValidable {

    @SerializedName("errorCode")
    var errorCode: Int? = null

    @SerializedName("errorMessage")
    var errorMessage: String? = null

    @SerializedName("result")
    var data: D? = null

    @SerializedName("sessionId")
    var session: String? = null

    @SerializedName("rid")
    var rid: String? = null

    @SerializedName("execTime")
    var execTime: Double? = null

    @SerializedName("execTimeExternal")
    var execTimeExternal: Double? = null

    override fun isEmpty() = with(data) {
        this == null || (this is EmptyValidable && this.isEmpty())
    }

    fun hasSession() = session?.isNotEmpty() == true

    fun hasRid() = rid?.isNotEmpty() == true

    companion object {

        fun <D> fromDataClass(
            data: String?,
            typeOfD: Class<D>,
            source: ResponseObj<*>,
            gson: Gson
        ) = fromData<D>(data, typeOfD, source, gson)

        /**
         * @return новый [ResponseObj] на основе исходного [source]
         * и данных из другого ответа [data] с указанным типом [D]
         */
        fun <D> fromData(
            data: String?,
            typeOfD: Type,
            source: ResponseObj<*>,
            gson: Gson
        ): ResponseObj<D>? {
            gson.fromJsonOrNull<D>(data, typeOfD)?.let {
                val result = ResponseObj<D>()
                result.data = it
                result.errorCode = source.errorCode
                result.errorMessage = source.errorMessage
                result.session = source.session
                result.rid = source.rid
                result.execTime = source.execTime
                result.execTimeExternal = source.execTimeExternal
                return result
            }
            return null;
        }
    }
}