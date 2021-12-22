package net.maxsmr.mxstemplate.ui.web

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Bundle
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebView
import androidx.annotation.CallSuper
import net.maxsmr.commonutils.gui.loadDataBase64
import net.maxsmr.commonutils.text.EMPTY_STRING
import net.maxsmr.mxstemplate.ui.common.BaseFragment
import net.maxsmr.mxstemplate.ui.web.client.InterceptWebViewClient
import java.nio.charset.Charset

abstract class BaseWebViewFragment<VM : BaseWebViewModel<*>> : BaseFragment<VM>() {

    abstract val webView: WebView

    protected open val shouldInterceptOnUpPressed: Boolean = true

    protected open val shouldInterceptOnBackPressed: Boolean = true

    protected var isWebViewInitialized = false
        private set

    @CallSuper
    override fun onViewCreated(view: View, savedInstanceState: Bundle?, viewModel: VM) {
        setupWebView(savedInstanceState)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        val bundle = Bundle()
        webView.saveState(bundle)
        outState.putBundle(ARG_WEB_VIEW_STATE, bundle)
    }

    override fun onUpPressed(): Boolean = if (shouldInterceptOnUpPressed && webView.canGoBack()) {
        webView.goBack()
        true
    } else {
        super.onUpPressed()
    }

    override fun onBackPressed(): Boolean = if (shouldInterceptOnBackPressed && webView.canGoBack()) {
        webView.goBack()
        true
    } else {
        super.onBackPressed()
    }

    override fun onResume() {
        super.onResume()
        webView.onResume()
    }

    override fun onPause() {
        super.onPause()
        webView.onPause()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        destroyWebView()
    }

    protected open fun createWebViewClient(): InterceptWebViewClient? = InterceptWebViewClient()

    protected open fun createWebChromeClient(): WebChromeClient? = null

    @SuppressLint("SetJavaScriptEnabled")
    protected open fun onSetupWebView() {
        with(webView.settings) {
            loadWithOverviewMode = true
            mediaPlaybackRequiresUserGesture = false
            useWideViewPort = true
            setSupportZoom(true)
            builtInZoomControls = true
            displayZoomControls = true
            javaScriptEnabled = true
            javaScriptCanOpenWindowsAutomatically = true
        }
    }

    @CallSuper
    protected open fun onDestroyWebView() {
        loadUrl(EMPTY_STRING)
        webView.clearHistory()
        webView.clearCache(true)
        webView.pauseTimers()
        webView.destroy()
    }

    @CallSuper
    protected open fun onPageStarted(url: String, favicon: Bitmap?) {
        if (url.isEmpty()) {
            viewModel.loadedWebViewUrl.value = EMPTY_STRING
        }
    }

    @CallSuper
    protected open fun onPageFinished(url: String) {
        viewModel.loadedWebViewUrl.value = url
    }

    protected open fun onWebViewStateRestored() {}

    protected open fun onWebViewFirstInit() {}

    protected fun loadUrl(url: String) {
        if (isWebViewInitialized) {
            webView.loadUrl(url)
        }
    }

    @JvmOverloads
    protected fun loadDataBase64(data: String, charset: Charset = Charsets.UTF_8) {
        if (isWebViewInitialized) {
            webView.loadDataBase64(data, charset)
        }
    }

    @JvmOverloads
    protected fun loadDataWithBaseUrl(baseUrl: String?, data: String, charset: Charset = Charsets.UTF_8, historyUrl: String? = null) {
        if (isWebViewInitialized) {
            webView.loadDataWithBaseURL(
                baseUrl,
                data,
                "text/html",
                charset.name(),
                historyUrl
            )
        }
    }

    private fun setupWebView(savedInstanceState: Bundle?) {
        destroyWebView()
        createWebViewClient()?.let {
            it.onPageStarted = { url, bitmap ->
                onPageStarted(url, bitmap)
            }
            it.onPageFinished = { url ->
                onPageFinished(url)
            }
            webView.webViewClient = it
        }
        createWebChromeClient()?.let {
            webView.webChromeClient = it
        }
        onSetupWebView()
        isWebViewInitialized = true

        savedInstanceState?.getBundle(ARG_WEB_VIEW_STATE)?.let {
            webView.restoreState(it)
            onWebViewStateRestored()
        } ?: onWebViewFirstInit()
    }

    private fun destroyWebView() {
        if (isWebViewInitialized) {
            onDestroyWebView()
            isWebViewInitialized = false
        }
    }

    companion object {

        private const val ARG_WEB_VIEW_STATE = "web_view_state"
    }
}