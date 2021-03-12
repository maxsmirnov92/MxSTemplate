package net.maxsmr.mxstemplate.ui.common.viewmodel

import androidx.annotation.CallSuper
import androidx.lifecycle.SavedStateHandle
import io.reactivex.Completable
import io.reactivex.Maybe
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.disposables.Disposable
import io.reactivex.disposables.Disposables
import net.maxsmr.commonutils.gui.actions.dialog.DialogBuilderFragmentShowMessageAction
import net.maxsmr.commonutils.gui.actions.message.text.TextMessage
import net.maxsmr.commonutils.gui.fragments.dialogs.TypedDialogFragment
import net.maxsmr.commonutils.live.event.VmListEvent
import net.maxsmr.commonutils.rx.functions.ActionSafe
import net.maxsmr.commonutils.rx.functions.ConsumerSafe
import net.maxsmr.core_common.BaseApplication
import net.maxsmr.core_common.arch.ErrorHandler
import net.maxsmr.core_common.arch.StringsProvider
import net.maxsmr.core_common.arch.rx.isDisposableActive
import net.maxsmr.core_common.arch.rx.scheduler.SchedulersProvider
import net.maxsmr.core_common.ui.viewmodel.BaseScreenData
import net.maxsmr.core_common.ui.viewmodel.BaseViewModel
import net.maxsmr.core_network.connection.ConnectionProvider
import net.maxsmr.mxstemplate.R
import net.maxsmr.networkutils.ConnectivityChecker
import net.maxsmr.networkutils.NetworkHelper

const val DIALOG_TAG_NO_CONNECTION = "no_connection"

/**
 * [BaseViewModel] с логикой сетевых вызовов, обзор статуса сети
 * и обработкой ошибок в [ErrorHandler]
 */
@Suppress("UNCHECKED_CAST")
abstract class BaseNetworkingViewModel<SD : BaseScreenData>(
    protected val connectionProvider: ConnectionProvider,
    savedStateHandle: SavedStateHandle,
    schedulersProvider: SchedulersProvider,
    stringsProvider: StringsProvider,
    errorHandler: ErrorHandler
) : BaseViewModel<SD>(savedStateHandle, schedulersProvider, stringsProvider, errorHandler) {

    private val networkStatus = ConnectivityChecker(BaseApplication.context)

    /**
     * Подписка на ивенты смены состояния сети, но временная и по месту конкретного вызова
     */
    private var autoReloadDisposable: Disposable = Disposables.disposed()

    /**
     * Подписка на ивенты смены состояния сети
     */
    private var networkStatusObserver: ((Boolean) -> Unit)? = null


    @CallSuper
    override fun onCleared() {
        super.onCleared()
        unsubscribeOnConnectionChanges()
    }

    /**
     * observeForever для LD в этом методе
     */
    @CallSuper
    override fun observeValues() {
        subscribeOnConnectionChanges()
    }

    fun <T : Any> checkConnectionAndRun(targetAction: () -> T?): T? =
        if (checkConnection()) targetAction() else null

    fun checkConnection(): Boolean = NetworkHelper.isOnline(BaseApplication.context).also {
        if (!it) showDialogCommands.setNewEvent(
            DialogBuilderFragmentShowMessageAction(
                DIALOG_TAG_NO_CONNECTION,
                TypedDialogFragment.DefaultTypedDialogBuilder()
                    .setMessage(TextMessage(R.string.message_no_internet))
                    .setButtons(TextMessage(android.R.string.ok)),
                false
            ),
            VmListEvent.AddOptions(DIALOG_TAG_NO_CONNECTION)
        )
    }

    protected open fun handleConnectionChanged(isConnected: Boolean) {
        // override if needed
    }


    //region subscribeIoAutoReload

    /**
     * {@see subscribeIo}
     * автоматически вызовет autoReloadAction при появлении интернета если на момент выполнения
     * observable не было подключения к интернету
     */
    protected fun <T> subscribeIoAutoReload(
        observable: Observable<T>,
        autoReloadAction: ActionSafe,
        onNext: ConsumerSafe<T>,
        onError: ConsumerSafe<Throwable>
    ): Disposable {
        return subscribeIo<T>(initializeAutoReload(observable, autoReloadAction), onNext, onError)
    }

    protected fun <T> subscribeIoAutoReload(
        observable: Observable<T>,
        autoReloadAction: ActionSafe,
        onNext: ConsumerSafe<T>,
        onComplete: ActionSafe,
        onError: ConsumerSafe<Throwable>
    ): Disposable {
        return subscribeIo(
            initializeAutoReload(observable, autoReloadAction),
            onNext,
            onComplete,
            onError
        )
    }

    protected fun <T> subscribeIoAutoReload(
        single: Single<T>,
        autoReloadAction: ActionSafe,
        onSuccess: ConsumerSafe<T>,
        onError: ConsumerSafe<Throwable>
    ): Disposable {
        return subscribeIo(initializeAutoReload(single, autoReloadAction), onSuccess, onError)
    }

    protected fun subscribeIoAutoReload(
        completable: Completable,
        autoReloadAction: ActionSafe,
        onComplete: ActionSafe,
        onError: ConsumerSafe<Throwable>
    ): Disposable {
        return subscribeIo(initializeAutoReload(completable, autoReloadAction), onComplete, onError)
    }

    protected fun <T> subscribeIoAutoReload(
        maybe: Maybe<T>,
        autoReloadAction: ActionSafe,
        onSuccess: ConsumerSafe<T>,
        onComplete: ActionSafe,
        onError: ConsumerSafe<Throwable>
    ): Disposable {
        return subscribeIo(
            initializeAutoReload(maybe, autoReloadAction),
            onSuccess,
            onComplete,
            onError
        )
    }
    //endregion

    //region subscribeIoHandleErrorAutoReload

    /**
     * {@see subscribeIoAutoReload} кроме того автоматически обрабатывает ошибки
     */
    protected fun <T> subscribeIoHandleErrorAutoReload(
        observable: Observable<T>,
        autoReloadAction: ActionSafe,
        onNext: ConsumerSafe<T>,
        onError: ConsumerSafe<Throwable>?
    ): Disposable {
        return subscribeIoHandleError(
            initializeAutoReload(observable, autoReloadAction),
            onNext,
            onError
        )
    }

    protected fun <T> subscribeIoHandleErrorAutoReload(
        observable: Observable<T>,
        autoReloadAction: ActionSafe,
        onNext: ConsumerSafe<T>,
        onComplete: ActionSafe,
        onError: ConsumerSafe<Throwable>?
    ): Disposable {
        return subscribeIoHandleError(
            initializeAutoReload(observable, autoReloadAction),
            onNext,
            onComplete,
            onError
        )
    }

    protected fun <T> subscribeIoHandleErrorAutoReload(
        single: Single<T>,
        autoReloadAction: ActionSafe,
        onSuccess: ConsumerSafe<T>,
        onError: ConsumerSafe<Throwable>?
    ): Disposable {
        return subscribeIoHandleError(
            initializeAutoReload(single, autoReloadAction),
            onSuccess,
            onError
        )
    }

    protected fun subscribeIoHandleErrorAutoReload(
        completable: Completable,
        autoReloadAction: ActionSafe,
        onComplete: ActionSafe,
        onError: ConsumerSafe<Throwable>?
    ): Disposable {
        return subscribeIoHandleError(
            initializeAutoReload(completable, autoReloadAction),
            onComplete,
            onError
        )
    }

    protected fun <T> subscribeIoHandleErrorAutoReload(
        maybe: Maybe<T>,
        autoReloadAction: ActionSafe,
        onSuccess: ConsumerSafe<T>,
        onComplete: ActionSafe,
        onError: ConsumerSafe<Throwable>?
    ): Disposable {
        return subscribeIoHandleError(
            initializeAutoReload(maybe, autoReloadAction),
            onSuccess,
            onComplete,
            onError
        )
    }

    //endregion

    private fun <T> initializeAutoReload(
        observable: Observable<T>,
        reloadAction: ActionSafe
    ): Observable<T> {
        return observable.doOnError(reloadErrorAction(reloadAction))
    }

    private fun <T> initializeAutoReload(single: Single<T>, reloadAction: ActionSafe): Single<T> {
        return single.doOnError(reloadErrorAction(reloadAction))
    }

    private fun initializeAutoReload(
        completable: Completable,
        reloadAction: ActionSafe
    ): Completable {
        return completable.doOnError(reloadErrorAction(reloadAction))
    }

    private fun <T> initializeAutoReload(maybe: Maybe<T>, reloadAction: ActionSafe): Maybe<T> {
        return maybe.doOnError(reloadErrorAction(reloadAction))
    }

    private fun reloadErrorAction(reloadAction: ActionSafe): ConsumerSafe<Throwable> {
        return ConsumerSafe {
            cancelAutoReload()
            if (!connectionProvider.isConnected) {
                autoReloadDisposable = subscribe(connectionProvider.observeConnectionChanges()
                    .filter { it }
                    .firstElement()
                    .toObservable(),
                    ConsumerSafe { reloadAction.run() })
            }
        }
    }

    private fun cancelAutoReload() {
        if (isDisposableActive(autoReloadDisposable)) {
            autoReloadDisposable.dispose()
        }
    }

    private fun subscribeOnConnectionChanges() {
        val observer: (Boolean) -> Unit = {
            handleConnectionChanged(it)
        }
        networkStatus.isConnected.observeForever(observer)
        networkStatusObserver = observer
    }

    private fun unsubscribeOnConnectionChanges() {
        networkStatusObserver?.let {
            networkStatus.isConnected.removeObserver(it)
        }
    }
}