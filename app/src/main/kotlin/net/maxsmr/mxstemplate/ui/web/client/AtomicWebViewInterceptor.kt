package net.maxsmr.mxstemplate.ui.web.client

import net.maxsmr.mxstemplate.ui.web.client.IWebViewInterceptor.InterceptedUrl
import java.util.concurrent.atomic.AtomicBoolean

class AtomicWebViewInterceptor : IWebViewInterceptor {

    private val isIntercepted = AtomicBoolean(false)

    /**
     * @return [InterceptedUrl] если [interceptCondition] вернул значение впервые иначе null
     */
    override fun shouldIntercept(url: String?, interceptCondition: (String?) -> InterceptedUrl?): InterceptedUrl? {
        if (isIntercepted.get()) return null
        val interceptedUrlType = interceptCondition.invoke(url)
        if (interceptedUrlType != null) {
            if (isIntercepted.compareAndSet(false, true)) {
                return interceptedUrlType
            }
        }
        return null
    }
}