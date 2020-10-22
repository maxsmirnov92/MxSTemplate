package net.maxsmr.core_network.calladapter

import androidx.annotation.CallSuper
import com.google.gson.Gson
import io.reactivex.*
import net.maxsmr.core_common.arch.StringsProvider
import net.maxsmr.core_network.R
import org.reactivestreams.Publisher
import retrofit2.Call
import retrofit2.CallAdapter
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import net.maxsmr.core_network.error.HttpErrorCode
import net.maxsmr.core_network.error.exception.NetworkException
import net.maxsmr.core_network.error.exception.NoInternetException
import net.maxsmr.core_network.error.exception.http.*
import net.maxsmr.core_network.error.exception.converters.DefaultErrorResponseConverter
import net.maxsmr.core_network.error.handler.error.http.BaseHttpErrorHandler
import net.maxsmr.core_network.model.response.ResponseObj
import java.io.IOException
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.net.ConnectException

@Suppress("UNCHECKED_CAST")
class CallAdapterFactory(
    private val stringsProvider: StringsProvider,
    private val errorHandler: BaseHttpErrorHandler? = null,
    private val gson: Gson
) : CallAdapter.Factory() {

    private val rxJavaCallAdapterFactory = RxJava2CallAdapterFactory.create()
    private var resultCallAdapter: ResultCallAdapter<*>? = null

    override fun get(returnType: Type, annotations: Array<Annotation>, retrofit: Retrofit): CallAdapter<*, *>? {
        val rxCallAdapter = rxJavaCallAdapterFactory.get(returnType, annotations, retrofit)
        resultCallAdapter = ResultCallAdapter(rxCallAdapter as CallAdapter<out Any, Any>, returnType)
        return resultCallAdapter
    }

    /**
     * @return созданный [HttpProtocolException] по исходному [HttpException] от ретрофита
     */
    protected fun createHttpProtocolExceptionByCause(cause: HttpException, call: Call<*>) =
            // в кач-ве примера используется AuthAccessTokenErrorResponseConverter
            HttpProtocolException.Builder<HttpProtocolException, ResponseObj<*>>(
                    cause,
                    call,
                    DefaultErrorResponseConverter(),
                    gson
            ).build()

    /**
     * Метод обработки ошибки [HttpException]
     * Здесь определеяется поведение на различные коды ошибок. Например:
     * * c кодом 401 и если пользователь был авторизован - сбрасывает все данные пользователя и открывает экран авторизации
     * * c кодом 400 перезапрашивает токен и повторяет предыдущий запрос
     * @return [Observable] с конкретной ошибкой от [BaseWrappedHttpException]
     */
    protected fun <Response : Any?, E : BaseWrappedHttpException?> onHttpException(e: HttpException, call: Call<Response>): Observable<E> {

        val httpError = createHttpProtocolExceptionByCause(e, call)

        val wrappedError = when (httpError.getHttpErrorCode()) {
            HttpErrorCode.NOT_MODIFIED -> NotModifiedException(stringsProvider.getString(R.string.http_not_modified_error_text), httpError)
            HttpErrorCode.BAD_REQUEST -> BadRequestError(stringsProvider.getString(R.string.http_bad_request_error_text), httpError)
            HttpErrorCode.NOT_AUTHORIZED -> NonAuthorizedException(stringsProvider.getString(R.string.http_not_authorized_error_text), httpError)
            HttpErrorCode.NOT_FOUND -> NotFoundError(stringsProvider.getString(R.string.http_not_found_error_text), httpError)
            HttpErrorCode.INTERNAL_SERVER_ERROR -> InternalServerError(stringsProvider.getString(R.string.http_internal_server_error_text), httpError)
            HttpErrorCode.FORBIDDEN -> ForbiddenError(stringsProvider.getString(R.string.http_forbidden_error_text), httpError)
            HttpErrorCode.UNKNOWN -> OtherHttpException(stringsProvider.getString(R.string.http_other_error_text), httpError)
        }
        handleBaseError(wrappedError)

        return Observable.error<E>(wrappedError)
    }

    /**
     * метод позволяет выполнить call(например, неудавшийся запрос)
     * необходим для предков класса [CallAdapterFactory]
     */
    protected fun <Response> adaptCallAdapter(call: Call<Response>): Any =
            (resultCallAdapter as ResultCallAdapter<Response>?)?.adapt(call)
                    ?: throw IllegalStateException("ResultCallAdapter not initialized")


    @CallSuper
    protected fun handleBaseError(e: NetworkException) {
        // здесь можно обработать ситуации по типу рефрешей токена в случае 401 и т.д.
        e.cause?.let {
            if (it is HttpProtocolException) {
                errorHandler?.handle(it)
            }
        }
    }

    inner class ResultCallAdapter<Response>(
            private val rxCallAdapter: CallAdapter<Response, Any>,
            returnType: Type
    ) : CallAdapter<Response, Any> {

        private val responseType: Type = if (returnType is ParameterizedType) {
            getParameterUpperBound(0, returnType)
        } else {
            returnType
        }

        override fun responseType(): Type {
            return responseType
        }

        override fun adapt(call: Call<Response>): Any {
            return when (val observable = rxCallAdapter.adapt(call)) {
                is Flowable<*> -> observable.onErrorResumeNext { e: Throwable ->
                    handleNetworkError(e, call).toFlowable(BackpressureStrategy.LATEST) as Publisher<out Nothing>
                }
                is Maybe<*> -> observable.onErrorResumeNext { e: Throwable ->
                    handleNetworkError(e, call).singleElement() as MaybeSource<out Nothing>
                }
                is Single<*> -> observable.onErrorResumeNext { e: Throwable ->
                    handleNetworkError(e, call).singleOrError() as SingleSource<out Nothing>
                }
                is Completable -> observable.onErrorResumeNext { e: Throwable ->
                    handleNetworkError(e, call).ignoreElements()
                }
                else -> (observable as Observable<*>).onErrorResumeNext { e: Throwable ->
                    handleNetworkError(e, call) as ObservableSource<out Nothing>
                }
            }
        }

        private fun handleNetworkError(e: Throwable, call: Call<Response>): Observable<out Exception> {
            return when (e) {
                is ConnectException -> Observable.error(e)
                is IOException -> Observable.error(NoInternetException(cause = e))
                is HttpException -> onHttpException(e, call)
                else -> Observable.error(e)
            }
        }
    }
}
