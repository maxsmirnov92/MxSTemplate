package net.maxsmr.mxstemplate.ui.web.client

import android.annotation.TargetApi
import android.graphics.Bitmap
import android.os.Build
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.annotation.CallSuper
import androidx.annotation.RequiresApi
import net.maxsmr.commonutils.isAtLeastLollipop
import net.maxsmr.mxstemplate.ui.web.client.IWebViewInterceptor.InterceptedUrl

open class InterceptWebViewClient @JvmOverloads constructor(private val webViewInterceptor: IWebViewInterceptor? = null) : WebViewClient() {

    var onPageStarted: ((String, Bitmap?) -> Unit)? = null

    var onPageFinished: ((String) -> Unit)? = null

    override fun shouldInterceptRequest(view: WebView?, url: String?): WebResourceResponse? {
        return if (shouldInterceptCommand(view, url, ::shouldInterceptFromOverrideUrl)) {
            webViewInterceptor?.getStubForInterceptedRequest()
        } else {
            null
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
        return if (shouldInterceptCommand(view, request?.url?.toString(), ::shouldInterceptFromRequest)) {
            webViewInterceptor?.getStubForInterceptedRequest()
        } else {
            null
        }
    }

    // вызовется < 24 api
    @CallSuper
    override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
        return shouldInterceptCommand(view, url, ::shouldInterceptFromOverrideUrl)
    }

    @RequiresApi(Build.VERSION_CODES.N)
    @CallSuper
    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
        return shouldInterceptCommand(view, request?.url?.toString(), ::shouldInterceptFromOverrideUrl)
    }

    override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
        shouldInterceptCommand(view, url, ::shouldInterceptFromPageStart)
        onPageStarted?.invoke(url.orEmpty(), favicon)
    }

    override fun onPageFinished(view: WebView?, url: String?) {
        shouldInterceptCommand(view, url, ::shouldInterceptFromPageFinish)
        onPageFinished?.invoke(url.orEmpty())
    }

    open fun shouldInterceptFromRequest(url: String?): InterceptedUrl? = null

    open fun shouldInterceptFromOverrideUrl(url: String?): InterceptedUrl? = null

    open fun shouldInterceptFromPageStart(url: String?): InterceptedUrl? = null

    open fun shouldInterceptFromPageFinish(url: String?): InterceptedUrl? = null

    protected open fun onFirstInterceptedUrl(view: WebView?, interceptedUrlType: InterceptedUrl) {}

    fun cancelLoading(webView: WebView?) {
        webView?.post {
            webView.stopLoading()
            //RZD-6326 непонятный краш внутри WebView при загрузке какой-то страницы или выполнении js после возврата с ПШ по ссылке.
            webView.loadUrl("about:blank")
        }
    }

    private fun shouldInterceptCommand(
        view: WebView?,
        url: String?,
        interceptedFunc: (String?) -> InterceptedUrl?,
    ): Boolean {
        val interceptedUrlType = webViewInterceptor?.shouldIntercept(url) { _url -> interceptedFunc(_url) }
        return if (interceptedUrlType != null) {
            onFirstInterceptedUrl(view, interceptedUrlType)
            true
        } else {
            false
        }
    }
}