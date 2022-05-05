package net.maxsmr.mxstemplate.api.handler

import android.content.Context
import net.maxsmr.core_network.error.exception.ConversionException
import net.maxsmr.core_network.error.exception.NetworkException
import net.maxsmr.core_network.error.exception.NoInternetException
import net.maxsmr.core_network.error.exception.http.NonAuthorizedException
import net.maxsmr.core_network.error.handler.error.BaseNetworkErrorHandler
import net.maxsmr.core_network.session.SessionStorage
import javax.inject.Inject

open class StandardErrorHandler @Inject constructor(): BaseNetworkErrorHandler() {

    override fun handleNetworkException(e: NetworkException) {
        if (e is NonAuthorizedException) {
            handleNonAuthorized()
            return
        }
    }

    override fun handleNoInternetError(e: NoInternetException) {

    }

    override fun handleConversionError(e: ConversionException) {

    }

    override fun handleOtherError(e: Throwable) {

    }

    protected open fun handleNonAuthorized() {
        // чистим сесссию
        SessionStorage.clear()
    }
}