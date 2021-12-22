package net.maxsmr.mxstemplate.ui.web.client

import android.webkit.WebResourceResponse
import java.io.ByteArrayInputStream
import java.io.Serializable

interface IWebViewInterceptor : Serializable {

    fun shouldIntercept(url: String?, interceptCondition: (String?) -> InterceptedUrl?): InterceptedUrl?

    fun getStubForInterceptedRequest() = WebResourceResponse(
        "text/html",
        Charsets.UTF_8.name(),
        ByteArrayInputStream(ByteArray(0))
    )

    data class InterceptedUrl(val type: Type, val url: String?) {
        enum class Type {
            OK,
            DECLINE,
            CANCEL
        }
    }
}