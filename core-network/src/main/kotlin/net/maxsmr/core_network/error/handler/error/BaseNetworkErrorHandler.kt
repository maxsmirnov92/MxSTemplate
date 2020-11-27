package net.maxsmr.core_network.error.handler.error

import android.util.Log
import io.reactivex.exceptions.CompositeException
import net.maxsmr.core_common.arch.ErrorHandler
import net.maxsmr.core_network.error.exception.ConversionException
import net.maxsmr.core_network.error.exception.NetworkException
import net.maxsmr.core_network.error.exception.NoInternetException

/**
 * Базовый класс для обработки ошибок, возникающий при работе с Observable
 * в базовой ViewModel
 */
abstract class BaseNetworkErrorHandler :
    ErrorHandler {

    override fun handleError(err: Throwable) {
        Log.e("BaseNetworkErrorHandler", "handle error: $err")
        when (err) {
            is CompositeException -> handleCompositeException(err)
            is ConversionException -> handleConversionError(err)
            is NetworkException -> handleNetworkException(err)
            is NoInternetException -> handleNoInternetError(err)
            else -> handleOtherError(err)
        }
    }

    protected abstract fun handleNetworkException(e: NetworkException)

    protected abstract fun handleNoInternetError(e: NoInternetException)

    protected abstract fun handleConversionError(e: ConversionException)

    protected abstract fun handleOtherError(e: Throwable)

    /**
     * @param err - CompositeException может возникать при комбинировании Observable
     */
    private fun handleCompositeException(err: CompositeException) {
        val exceptions = err.exceptions
        var networkException: NetworkException? = null
        var otherException: Throwable? = null
        for (e in exceptions) {
            if (e is NetworkException) {
                if (networkException == null) {
                    networkException = e
                }
            } else if (otherException == null) {
                otherException = e
            }
        }
        if (networkException != null) {
            handleError(networkException)
        }
        if (otherException != null) {
            handleOtherError(otherException)
        }
    }
}
