package net.maxsmr.core_common.ui.viewmodel

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Toast
import androidx.annotation.CallSuper
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.agna.ferro.rx.CompletableOperatorFreeze
import com.agna.ferro.rx.MaybeOperatorFreeze
import com.agna.ferro.rx.ObservableOperatorFreeze
import com.agna.ferro.rx.SingleOperatorFreeze
import com.google.android.material.snackbar.Snackbar
import io.reactivex.*
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.internal.functions.Functions
import io.reactivex.internal.observers.LambdaObserver
import io.reactivex.observers.DisposableCompletableObserver
import io.reactivex.observers.DisposableMaybeObserver
import io.reactivex.observers.DisposableSingleObserver
import io.reactivex.subjects.BehaviorSubject
import me.ilich.juggler.change.Remove
import net.maxsmr.commonutils.android.gui.actions.BaseTaggedViewModelAction
import net.maxsmr.commonutils.android.gui.actions.BaseViewModelAction
import net.maxsmr.commonutils.android.gui.actions.dialog.DialogFragmentHideMessageAction
import net.maxsmr.commonutils.android.gui.actions.dialog.DialogFragmentShowMessageAction
import net.maxsmr.commonutils.android.gui.actions.message.AlertDialogMessageAction
import net.maxsmr.commonutils.android.gui.actions.message.BaseMessageAction
import net.maxsmr.commonutils.android.gui.fragments.dialogs.TypedDialogFragment
import net.maxsmr.commonutils.android.live.event.VmListEvent
import net.maxsmr.commonutils.rx.functions.ActionSafe
import net.maxsmr.commonutils.rx.functions.BiFunctionSafe
import net.maxsmr.commonutils.rx.functions.ConsumerSafe
import net.maxsmr.core_common.arch.ErrorHandler
import net.maxsmr.core_common.arch.StringsProvider
import net.maxsmr.core_common.arch.rx.EMPTY_ACTION
import net.maxsmr.core_common.arch.rx.ON_ERROR_MISSING
import net.maxsmr.core_common.arch.rx.callinfo.*
import net.maxsmr.core_common.arch.rx.scheduler.SchedulersProvider
import net.maxsmr.core_common.ui.actions.NavigationAction
import net.maxsmr.core_common.ui.dialog.ProgressDialogFragment
import net.maxsmr.core_common.ui.viewmodel.delegates.getPersistableKey
import kotlin.reflect.KProperty

private const val ARG_CURRENT_SCREEN_DATA = "current_screen_data"

/**
 * Базовая [ViewModel] с возможностью выполнения rx-вызовов,
 * заморозки вызовов, отправки команд через [VmListEvent] и т.д.
 */
@Suppress("UNCHECKED_CAST")
abstract class BaseViewModel<SD : BaseScreenData>(
    val state: SavedStateHandle,
    protected val schedulersProvider: SchedulersProvider,
    protected val stringsProvider: StringsProvider,
    /**
     * Выставить в производном классе при необходимости
     * или оставить тот, что из модуля
     */
    protected var errorHandler: ErrorHandler?
) : ViewModel() {

    val navigationCommands: MutableLiveData<VmListEvent<NavigationAction>> = MutableLiveData()

    val toastMessageCommands: MutableLiveData<VmListEvent<BaseMessageAction<Toast, Context>>> = MutableLiveData()

    val snackMessageCommands: MutableLiveData<VmListEvent<BaseMessageAction<Snackbar, View>>> = MutableLiveData()

    val showDialogCommands: MutableLiveData<VmListEvent<DialogFragmentShowMessageAction>> =
        MutableLiveData()

    val hideDialogCommands: MutableLiveData<VmListEvent<DialogFragmentHideMessageAction>> =
        MutableLiveData()

    protected val KProperty<*>.persistableKey: String
        get() = this@BaseViewModel.getPersistableKey(this)

    /**
     * Маппинг селекторов, привязанных к конкретной view:
     * изначальная/пересозданная будет иметь свой селектор, далее применяемый в subscribe
     */
    private val freezeSelectorsMap = mutableMapOf<String, BehaviorSubject<Boolean>>()

    private var currentFreezeSelector: BehaviorSubject<Boolean>? = null

    @Deprecated("use showMessageCommands or hideMessageCommands")
    val alertDialogMessageCommands: MutableLiveData<VmListEvent<AlertDialogMessageAction>> =
        MutableLiveData()

    private val disposables = CompositeDisposable()

    init {
        Handler(Looper.getMainLooper()).post { onInitialized() }
    }

    @CallSuper
    override fun onCleared() {
        super.onCleared()
        disposables.clear()
    }

    /**
     * Должен быть вызван последней view, которая пользует данную VM
     */
    fun notifyResumed(tag: String) {
        getCurrentSelector(tag).onNext(false)
    }

    fun notifyPaused(tag: String) {
        getCurrentSelector(tag).onNext(true)
    }

    fun getCurrentSelector(tag: String): BehaviorSubject<Boolean> {
        var selector = freezeSelectorsMap[tag]
        if (selector == null) {
            selector = BehaviorSubject.createDefault(false)
            freezeSelectorsMap[tag] = selector
        }
        currentFreezeSelector = selector
        return selector
    }

    /**
     * Опциональное восстановление из [Bundle] по инициативе view
     */
    @CallSuper
    fun restoreFromBundle(savedInstanceState: Bundle?) {
        restoreFromScreenData(savedInstanceState?.getSerializable(ARG_CURRENT_SCREEN_DATA) as? SD)
        onLoad(savedInstanceState != null)
    }

    /**
     * Опциональное сохранение в [Bundle] по инициативе view
     */
    fun saveToBundle(outState: Bundle) {
        outState.putSerializable(ARG_CURRENT_SCREEN_DATA, saveToScreenData())
    }

    fun doClose() {
        navigationCommands.setNewEvent(NavigationAction(Remove.closeCurrentActivity(), null))
    }

    /**
     * Метод вызывается после выполнения init блока конкретного класса. Полезен для задания логики
     * в подклассах BaseViewModel, имеющих собственных наследников.
     *
     * Кейс: абстрактный класс А с open методом d(). Его наследник B переопределяет метод d() с
     * обращением к своим полям. Если метод d() вызывается в init блоке A, то на этот момент
     * поля класса B еще не проинициализированы и при обращении к ним можно получить краш или баг.
     */
    @CallSuper
    protected open fun onInitialized() {
        observeValues()
    }

    /**
     * observeForever для LD в этом методе
     */
    protected open fun observeValues() {
        // override if needed
    }

    /**
     * Восстановить из [SD] данные в поля VM
     */
    protected open fun restoreFromScreenData(data: SD?) {
        // override if needed
    }

    /**
     * Сохранить текущее состояние модели в [SD];
     * нужно для тех случаев, когда VM не переживает пересоздание экрана, но при этом остаётся [Bundle]
     * (например при "don't keep activities"); по факту будет применено перед onStop
     */
    protected open fun saveToScreenData(): SD? {
        return null
    }

    /**
     * Для выполнения каких-то инициализирующих запросов в зав-ти от пересозданности view
     * (или в отдельных случаях от [SD])
     * @param viewRecreated была ли view пересоздана в этот раз
     */
    protected open fun onLoad(viewRecreated: Boolean) {
        // do nothing
    }

    /**
     * @return true, если обработка в [errorHandler] для данной ошибки нужна
     */
    protected open fun shouldHandleError(e: Throwable, callInfo: BaseCallInfo) = true

    /**
     * Стандартная обработка ошибки из consumer'а
     *
     * Переопределяем в случае если нужно специфичная обработка
     *
     * @param e ошибка
     */
    @CallSuper
    open fun handleError(e: Throwable, callInfo: BaseCallInfo) {
        if (shouldHandleError(e, callInfo)) {
            errorHandler?.handleError(e)
        }
    }

    protected fun showOrHideProgress(show: Boolean, tag: String) {
        if (show) {
            showDialogCommands.value = showDialogCommands.newEvent(
                DialogFragmentShowMessageAction(
                    tag,
                    ProgressDialogFragment.ProgressDialogBuilder().build(),
                    false
                )
            )
        } else {
            hideDialogCommands.value =
                hideDialogCommands.newEvent(DialogFragmentHideMessageAction(tag))
        }
    }

    protected fun showOkErrorDialog(tag: String, errorMessage: String) {
        showDialogCommands.value = showDialogCommands.newEvent(
            DialogFragmentShowMessageAction(
                tag,
                TypedDialogFragment.DefaultTypedDialogBuilder()
                    .setMessage(errorMessage)
                    .setButtons(stringsProvider.getString(android.R.string.ok), null, null)
                    .build()
            )
        )
    }

    protected fun <T> subscribe(
        observable: Observable<T>,
        operator: ObservableOperatorFreeze<T>?,
        observer: LambdaObserver<T>
    ): Disposable {
        val disposable =
            if (operator != null) {
                observable.observeOn(schedulersProvider.main)
                    .lift<Any>(operator)
                    .subscribeWith(observer as Observer<Any>) as Disposable
            } else {
                observable.observeOn(schedulersProvider.main)
                    .subscribeWith(observer) as Disposable
            }
        disposables.add(disposable)
        return disposable
    }

    protected fun <T> subscribe(
        single: Single<T>,
        operator: SingleOperatorFreeze<T>?,
        observer: DisposableSingleObserver<T>
    ): Disposable {
        val disposable = if (operator != null) {
            single.observeOn(schedulersProvider.main)
                .lift<Any>(operator)
                .subscribeWith(observer as DisposableSingleObserver<Any>)
        } else {
            single.observeOn(schedulersProvider.main)
                .subscribeWith(observer as DisposableSingleObserver<Any>)
        }
        disposables.add(disposable)
        return disposable
    }

    protected fun subscribe(
        completable: Completable,
        operator: CompletableOperatorFreeze?,
        observer: DisposableCompletableObserver
    ): Disposable {
        val disposable = if (operator != null) {
            completable.observeOn(schedulersProvider.main)
                .lift(operator)
                .subscribeWith(observer)
        } else {
            completable.observeOn(schedulersProvider.main)
                .subscribeWith(observer)
        }
        disposables.add(disposable)
        return disposable
    }

    protected fun <T> subscribe(
        maybe: Maybe<T>,
        operator: MaybeOperatorFreeze<T>?,
        observer: DisposableMaybeObserver<T>
    ): Disposable {
        val disposable = if (operator != null) {
            maybe.observeOn(schedulersProvider.main)
                .lift<Any>(operator)
                .subscribeWith(observer as DisposableMaybeObserver<Any>)
        } else {
            maybe.observeOn(schedulersProvider.main)
                .subscribeWith(observer as DisposableMaybeObserver<Any>)
        }
        disposables.add(disposable)
        return disposable
    }

    protected fun <T> subscribe(
        observable: Observable<T>,
        operator: ObservableOperatorFreeze<T>?,
        onNext: ConsumerSafe<T>,
        onError: ConsumerSafe<Throwable>
    ): Disposable {
        return subscribe(observable, operator, onNext, EMPTY_ACTION, onError)
    }

    protected fun <T> subscribe(
        observable: Observable<T>,
        operator: ObservableOperatorFreeze<T>?,
        onNext: ConsumerSafe<T>,
        onComplete: ActionSafe,
        onError: ConsumerSafe<Throwable>
    ): Disposable {
        return subscribe(
            observable,
            operator,
            LambdaObserver<T>(onNext, onError, onComplete, Functions.emptyConsumer<Disposable>())
        )
    }

    protected fun <T> subscribe(
        single: Single<T>,
        operator: SingleOperatorFreeze<T>?,
        onSuccess: ConsumerSafe<T>,
        onError: ConsumerSafe<Throwable>
    ): Disposable {
        return subscribe(single, operator, object : DisposableSingleObserver<T>() {
            override fun onSuccess(t: T) {
                onSuccess.accept(t)
            }

            override fun onError(e: Throwable) {
                onError.accept(e)
            }
        })
    }

    protected fun <T> subscribe(
        maybe: Maybe<T>,
        operator: MaybeOperatorFreeze<T>?,
        onSuccess: ConsumerSafe<T>,
        onComplete: ActionSafe,
        onError: ConsumerSafe<Throwable>
    ): Disposable {
        return subscribe(maybe, operator, object : DisposableMaybeObserver<T>() {
            override fun onSuccess(t: T) {
                onSuccess.accept(t)
            }

            override fun onError(e: Throwable) {
                onError.accept(e)
            }

            override fun onComplete() {
                onComplete.run()
            }
        })
    }


    protected fun subscribe(
        completable: Completable,
        operator: CompletableOperatorFreeze?,
        onComplete: ActionSafe,
        onError: ConsumerSafe<Throwable>
    ): Disposable {
        return subscribe(completable, operator, object : DisposableCompletableObserver() {
            override fun onComplete() {
                onComplete.run()
            }

            override fun onError(e: Throwable) {
                onError.accept(e)
            }
        })
    }

    /**
     * @param replaceFrozenEventPredicate - used for reduce num element in freeze buffer
     * @see ObservableOperatorFreeze
     */
    protected fun <T> subscribe(
        observable: Observable<T>,
        replaceFrozenEventPredicate: BiFunctionSafe<T, T, Boolean>,
        observer: LambdaObserver<T>
    ): Disposable {

        return subscribe<T>(observable, createObservableOperatorFreeze(replaceFrozenEventPredicate), observer)
    }


    /**
     * @param replaceFrozenEventPredicate - used for reduce num element in freeze buffer
     * @see @link .subscribe
     * @see @link OperatorFreeze
     */
    protected fun <T> subscribe(
        observable: Observable<T>,
        replaceFrozenEventPredicate: BiFunctionSafe<T, T, Boolean>,
        onNext: ConsumerSafe<T>,
        onError: ConsumerSafe<Throwable>
    ): Disposable {

        return subscribe(
            observable,
            createObservableOperatorFreeze(replaceFrozenEventPredicate),
            onNext,
            onError
        )
    }

    /**
     * @see @link .subscribe
     */
    protected fun <T> subscribe(
        observable: Observable<T>,
        observer: LambdaObserver<T>
    ): Disposable {

        return subscribe<T>(observable, createObservableOperatorFreeze(), observer)
    }

    /**
     * @see @link .subscribe
     */
    protected fun <T> subscribe(
        observable: Observable<T>,
        onNext: ConsumerSafe<T>
    ): Disposable {

        return subscribe(observable, createObservableOperatorFreeze(), onNext, ON_ERROR_MISSING)
    }


    /**
     * @see @link .subscribe
     */
    protected fun <T> subscribe(
        observable: Observable<T>,
        onNext: ConsumerSafe<T>,
        onError: ConsumerSafe<Throwable>
    ): Disposable {

        return subscribe(observable, onNext, EMPTY_ACTION, onError)
    }


    /**
     * @see @link .subscribe
     */
    protected fun <T> subscribe(
        observable: Observable<T>,
        onNext: ConsumerSafe<T>,
        onComplete: ActionSafe,
        onError: ConsumerSafe<Throwable>
    ): Disposable {

        return subscribe(observable, createObservableOperatorFreeze(), onNext, onComplete, onError)
    }

    protected fun <T> subscribe(
        single: Single<T>,
        onNext: ConsumerSafe<T>
    ): Disposable {
        return subscribe(single, createSingleOperatorFreeze(), onNext, ON_ERROR_MISSING)
    }

    protected fun <T> subscribe(
        single: Single<T>,
        onSuccess: ConsumerSafe<T>,
        onError: ConsumerSafe<Throwable>
    ): Disposable {

        return subscribe(single, createSingleOperatorFreeze(), onSuccess, onError)
    }

    protected fun subscribe(
        completable: Completable,
        onComplete: ActionSafe,
        onError: ConsumerSafe<Throwable>
    ): Disposable {
        return subscribe(completable, createCompletableOperatorFreeze(), onComplete, onError)
    }


    protected fun <T> subscribe(
        maybe: Maybe<T>,
        onSuccess: ConsumerSafe<T>,
        onComplete: ActionSafe,
        onError: ConsumerSafe<Throwable>
    ): Disposable {
        return subscribe(maybe, createMaybeOperatorFreeze(), onSuccess, onComplete, onError)
    }

    /**
     * Subscribe and take only last emitted value from frozen predicate.
     * This is very useful in situations when your screen is long time inactive and it should react
     * only to last emitted value.
     *
     * For example, to prevent saving all the copies of data in storage when screen becomes visible,
     * and save only last emitted copy instead.
     *
     * @param observable observable to subscribe
     * @param onNext     action to call when new portion of data is emitted
     * @param onError    action to call when the error is occurred
     * @param <T>        type of observable element
     * @return Disposable
    </T> */
    protected fun <T> subscribeTakeLastFrozen(
        observable: Observable<T>,
        onNext: ConsumerSafe<T>,
        onError: ConsumerSafe<Throwable>
    ): Disposable {
        return subscribe(observable, createTakeLastFrozenPredicate<T>(), onNext, onError)
    }

    /**
     * Subscribe and take only last emitted value from frozen predicate.
     * This is very useful in situations when your screen is long time inactive and it should react
     * only to last emitted value.
     *
     * For example, to prevent saving all the copies of data in storage when screen becomes visible,
     * and save only last emitted copy instead.
     *
     * @param observable observable to subscribe
     * @param onNext     action to call when new portion of data is emitted
     * @param <T>        type of observable element
     * @return Disposable
    </T> */
    protected fun <T> subscribeTakeLastFrozen(
        observable: Observable<T>,
        onNext: ConsumerSafe<T>
    ): Disposable {
        return subscribe(observable, createTakeLastFrozenPredicate<T>(), onNext, ON_ERROR_MISSING)
    }

    /**
     * Subscribe and take only last emitted value from frozen predicate.
     * This is very useful in situations when your screen is long time inactive and it should react
     * only to last emitted value.
     *
     * For example, to prevent saving all the copies of data in storage when screen becomes visible,
     * and save only last emitted copy instead.
     *
     * @param observable observable to subscribe
     * @param observer observer that receives data
     * @param <T>        type of observable element
     * @return Disposable
    </T> */
    protected fun <T> subscribeTakeLastFrozen(
        observable: Observable<T>,
        observer: LambdaObserver<T>
    ): Disposable {
        return subscribe(observable, createTakeLastFrozenPredicate<T>(), observer)
    }

    //endregion

    /**
     * Subscribe subscriber to the observable without applying [ObservableOperatorFreeze]
     * When screen finally destroyed, all subscriptions would be automatically unsubscribed.
     *
     * @return subscription
     */
    protected fun <T> subscribeWithoutFreezing(
        observable: Observable<T>,
        subscriber: LambdaObserver<T>
    ): Disposable {

        val disposable = observable.subscribeWith(subscriber)
        disposables.add(disposable)
        return disposable
    }

    protected fun <T> subscribeWithoutFreezing(
        single: Single<T>,
        subscriber: DisposableSingleObserver<T>
    ): Disposable {

        val disposable = single.subscribeWith(subscriber)
        disposables.add(disposable)
        return disposable
    }

    protected fun subscribeWithoutFreezing(
        completable: Completable,
        subscriber: DisposableCompletableObserver
    ): Disposable {

        val disposable = completable.subscribeWith(subscriber)
        disposables.add(disposable)
        return disposable
    }

    protected fun <T> subscribeWithoutFreezing(
        maybe: Maybe<T>,
        subscriber: DisposableMaybeObserver<T>
    ): Disposable {

        val disposable = maybe.subscribeWith(subscriber)
        disposables.add(disposable)
        return disposable
    }

    /**
     * @see @link .subscribeWithoutFreezing
     */
    protected fun <T> subscribeWithoutFreezing(
        observable: Observable<T>,
        onNext: ConsumerSafe<T>,
        onError: ConsumerSafe<Throwable>
    ): Disposable {
        return subscribeWithoutFreezing(
            observable,
            LambdaObserver<T>(
                onNext,
                onError,
                Functions.EMPTY_ACTION,
                Functions.emptyConsumer<Disposable>()
            )
        )
    }

    protected fun <T> subscribeWithoutFreezing(
        single: Single<T>,
        onSuccess: ConsumerSafe<T>,
        onError: ConsumerSafe<Throwable>
    ): Disposable {
        return subscribeWithoutFreezing(single, object : DisposableSingleObserver<T>() {
            override fun onSuccess(t: T) {
                onSuccess.accept(t)
            }

            override fun onError(e: Throwable) {
                onError.accept(e)
            }
        })
    }

    protected fun subscribeWithoutFreezing(
        completable: Completable,
        onComplete: ActionSafe,
        onError: ConsumerSafe<Throwable>
    ): Disposable {
        return subscribeWithoutFreezing(completable, object : DisposableCompletableObserver() {
            override fun onComplete() {
                onComplete.run()
            }

            override fun onError(e: Throwable) {
                onError.accept(e)
            }
        })
    }

    protected fun <T> subscribeWithoutFreezing(
        maybe: Maybe<T>,
        onSuccess: ConsumerSafe<T>,
        onComplete: ActionSafe,
        onError: ConsumerSafe<Throwable>
    ): Disposable {
        return subscribeWithoutFreezing(maybe, object : DisposableMaybeObserver<T>() {

            override fun onSuccess(t: T) {
                onSuccess.accept(t)
            }

            override fun onComplete() {
                onComplete.run()
            }

            override fun onError(e: Throwable) {
                onError.accept(e)
            }
        })
    }

    /**
     * Predicate that takes only the last emitted value from the frozen buffer
     *
     * @param <T> observable element type
     * @return predicate
     * @see com.agna.ferro.rx.ObservableOperatorFreeze
    </T> */
    protected fun <T> createTakeLastFrozenPredicate(): BiFunctionSafe<T, T, Boolean> {
        return BiFunctionSafe { _, _ -> true }
    }

    //region subscribeIoHandleError

    protected fun <T> subscribeIoHandleError(
        observable: Observable<T>,
        onNext: ConsumerSafe<T>,
        onError: ConsumerSafe<Throwable>? = null
    ): Disposable {
        val lastSubscribedCallInfo =
            ObservableCallInfo(false, observable, onNext, null, onError, true)
        return subscribe(
            observable.subscribeOn(schedulersProvider.worker),
            onNext,
            { e -> handleError(e, onError, lastSubscribedCallInfo) })
    }

    protected fun <T> subscribeIoHandleError(
        observable: Observable<T>,
        onNext: ConsumerSafe<T>,
        onComplete: ActionSafe,
        onError: ConsumerSafe<Throwable>?
    ): Disposable {
        val lastSubscribedCallInfo =
            ObservableCallInfo(false, observable, onNext, onComplete, onError, true)
        return subscribe(
            observable.subscribeOn(schedulersProvider.worker),
            onNext,
            onComplete,
            { e -> handleError(e, onError, lastSubscribedCallInfo) })
    }

    protected fun <T> subscribeIoHandleError(
        single: Single<T>,
        onSuccess: ConsumerSafe<T>,
        onError: ConsumerSafe<Throwable>? = null
    ): Disposable {
        val lastSubscribedCallInfo = SingleCallInfo(single, onSuccess, onError, true)
        return subscribe(
            single.subscribeOn(schedulersProvider.worker),
            onSuccess,
            { e -> handleError(e, onError, lastSubscribedCallInfo) })
    }

    protected fun subscribeIoHandleError(
        completable: Completable,
        onComplete: ActionSafe,
        onError: ConsumerSafe<Throwable>? = null
    ): Disposable {
        val lastSubscribedCallInfo = CompletableCallInfo(completable, onComplete, onError, true)
        return subscribe(
            completable.subscribeOn(schedulersProvider.worker),
            onComplete,
            { e -> handleError(e, onError, lastSubscribedCallInfo) })
    }

    protected fun <T> subscribeIoHandleError(
        maybe: Maybe<T>,
        onSuccess: ConsumerSafe<T>,
        onComplete: ActionSafe,
        onError: ConsumerSafe<Throwable>?
    ): Disposable {
        val lastSubscribedCallInfo = MaybeCallInfo(maybe, onSuccess, onComplete, onError, true)
        return subscribe(
            maybe.subscribeOn(schedulersProvider.worker),
            onSuccess,
            onComplete,
            { e -> handleError(e, onError, lastSubscribedCallInfo) })
    }


    //endregion

    //region subscribeIoTakeLastFrozen

    /**
     * Subscribe and take only last emitted value from frozen predicate.
     * This is very useful in situations when your screen is long time inactive and it should react
     * only to last emitted value.
     *
     *
     * For example, to prevent saving all the copies of data in storage when screen becomes visible,
     * and save only last emitted copy instead.
     *
     *
     * Subscription is processed in worker thread and all errors are handled by [.handleError]}.
     *
     * @param observable observable to subscribe
     * @param onNext     action to call when new portion of data is emitted
     * @param onError    action to call when the error is occurred
     * @param <T>        type of observable element
     * @return Disposable
    </T> */
    protected fun <T> subscribeIoHandleErrorTakeLastFrozen(
        observable: Observable<T>,
        onNext: ConsumerSafe<T>,
        onError: ConsumerSafe<Throwable>? = null
    ): Disposable {
        val lastSubscribedCallInfo =
            ObservableCallInfo(true, observable, onNext, null, onError, true)
        return subscribeTakeLastFrozen(
            observable.subscribeOn(schedulersProvider.worker),
            onNext,
            { e -> handleError(e, onError, lastSubscribedCallInfo) })
    }

    //endregion

    //region subscribeIo
    protected fun <T> subscribeIo(
        observable: Observable<T>,
        onNext: ConsumerSafe<T>,
        onError: ConsumerSafe<Throwable>
    ): Disposable {
//        lastSubscribedCallInfo = ObservableCallInfo(true, observable, onNext, null, onError, false)
        return subscribe(observable.subscribeOn(schedulersProvider.worker), onNext, onError)
    }

    protected fun <T> subscribeIo(
        single: Single<T>,
        onSuccess: ConsumerSafe<T>,
        onError: ConsumerSafe<Throwable>
    ): Disposable {
//        lastSubscribedCallInfo = SingleCallInfo(single, onSuccess, onError, false)
        return subscribe(single.subscribeOn(schedulersProvider.worker), onSuccess, onError)
    }

    protected fun subscribeIo(
        completable: Completable,
        onComplete: ActionSafe,
        onError: ConsumerSafe<Throwable>
    ): Disposable {
//        lastSubscribedCallInfo = CompletableCallInfo(completable, onComplete, onError, false)
        return subscribe(completable.subscribeOn(schedulersProvider.worker), onComplete, onError)
    }

    protected fun <T> subscribeIo(
        observable: Observable<T>,
        onNext: ConsumerSafe<T>,
        onComplete: ActionSafe,
        onError: ConsumerSafe<Throwable>
    ): Disposable {
//        lastSubscribedCallInfo = ObservableCallInfo(false, observable, onNext, onComplete, onError, false)
        return subscribe(
            observable.subscribeOn(schedulersProvider.worker),
            onNext,
            onComplete,
            onError
        )
    }

    protected fun <T> subscribeIo(
        maybe: Maybe<T>,
        onSuccess: ConsumerSafe<T>,
        onComplete: ActionSafe,
        onError: ConsumerSafe<Throwable>
    ): Disposable {
//        lastSubscribedCallInfo = MaybeCallInfo(maybe, onSuccess, onComplete, onError, false)
        return subscribe(
            maybe.subscribeOn(schedulersProvider.worker),
            onSuccess,
            onComplete,
            onError
        )
    }
    //endregion

    //region subscribeIoTakeLastFrozen

    /**
     * Subscribe and take only last emitted value from frozen predicate.
     * This is very useful in situations when your screen is long time inactive and it should react
     * only to last emitted value.
     *
     *
     * For example, to prevent saving all the copies of data in storage when screen becomes visible,
     * and save only last emitted copy instead.
     *
     * @param observable observable to subscribe
     * @param onNext     action to call when new portion of data is emitted
     * @param onError    action to call when the error is occurred
     * @param <T>        type of observable element
     * @return Disposable
    </T> */
    protected fun <T> subscribeIoTakeLastFrozen(
        observable: Observable<T>,
        onNext: ConsumerSafe<T>,
        onError: ConsumerSafe<Throwable>
    ): Disposable {
//        lastSubscribedCallInfo = ObservableCallInfo(true, observable, onNext, null, onError, false)
        return subscribeTakeLastFrozen(
            observable.subscribeOn(schedulersProvider.worker),
            onNext,
            onError
        )
    }

    /**
     * Subscribe and take only last emitted value from frozen predicate.
     * This is very useful in situations when your screen is long time inactive and it should react
     * only to last emitted value.
     *
     *
     * For example, to prevent saving all the copies of data in storage when screen becomes visible,
     * and save only last emitted copy instead.
     *
     * @param observable observable to subscribe
     * @param onNext     action to call when new portion of data is emitted
     * @param <T>        type of observable element
     * @return Disposable
    </T> */
    protected fun <T> subscribeIoTakeLastFrozen(
        observable: Observable<T>,
        onNext: ConsumerSafe<T>
    ): Disposable {
        return subscribeTakeLastFrozen(observable.subscribeOn(schedulersProvider.worker), onNext)
    }

    /**
     * Subscribe and take only last emitted value from frozen predicate.
     * This is very useful in situations when your screen is long time inactive and it should react
     * only to last emitted value.
     *
     *
     * For example, to prevent saving all the copies of data in storage when screen becomes visible,
     * and save only last emitted copy instead.
     *
     *
     * Subscription is handled in worker thread.
     *
     * @param observable observable to subscribe
     * @param observer   observer that receives data
     * @param <T>        type of observable element
     * @return Disposable
    </T> */
    protected fun <T> subscribeIoTakeLastFrozen(
        observable: Observable<T>,
        observer: LambdaObserver<T>
    ): Disposable {
        return subscribeTakeLastFrozen(
            observable.subscribeOn(schedulersProvider.worker),
            observer
        )
    }
    //endregion

    protected fun subscribeIo(callInfo: BaseCallInfo) {
        if (callInfo.isForHandleError) {
            when (callInfo) {
                is CompletableCallInfo -> {
                    subscribeIoHandleError(
                        callInfo.completable,
                        callInfo.onComplete,
                        callInfo.onError
                    )
                }
                is MaybeCallInfo<*> -> {
                    subscribeIoHandleError(
                        callInfo.maybe as Maybe<Any>,
                        callInfo.onSuccess as ConsumerSafe<Any>,
                        callInfo.onComplete,
                        callInfo.onError
                    )
                }
                is ObservableCallInfo<*> -> {
                    if (!callInfo.isTakeLastFrozen) {
                        val onComplete = callInfo.onComplete
                        if (onComplete == null) {
                            subscribeIoHandleError(
                                callInfo.observable as Observable<Any>,
                                callInfo.onNext as ConsumerSafe<Any>,
                                callInfo.onError
                            )
                        } else {
                            subscribeIoHandleError(
                                callInfo.observable as Observable<Any>,
                                callInfo.onNext as ConsumerSafe<Any>,
                                onComplete,
                                callInfo.onError
                            )
                        }
                    } else {
                        subscribeIoHandleErrorTakeLastFrozen(
                            callInfo.observable as Observable<Any>,
                            callInfo.onNext as ConsumerSafe<Any>,
                            callInfo.onError
                        )
                    }
                }
                is SingleCallInfo<*> -> {
                    subscribeIoHandleError(
                        callInfo.single as Single<Any>,
                        callInfo.onSuccess as ConsumerSafe<Any>,
                        callInfo.onError
                    )
                }
            }
        } else {
            when (callInfo) {
                is CompletableCallInfo -> {
                    subscribeIo(
                        callInfo.completable,
                        callInfo.onComplete,
                        requireNotNull(callInfo.onError)
                    )
                }
                is MaybeCallInfo<*> -> {
                    subscribeIo(
                        callInfo.maybe as Maybe<Any>,
                        callInfo.onSuccess as ConsumerSafe<Any>,
                        callInfo.onComplete,
                        requireNotNull(callInfo.onError)
                    )
                }
                is ObservableCallInfo<*> -> {
                    if (!callInfo.isTakeLastFrozen) {
                        val onComplete = callInfo.onComplete
                        if (onComplete == null) {
                            subscribeIo(
                                callInfo.observable as Observable<Any>,
                                callInfo.onNext as ConsumerSafe<Any>,
                                requireNotNull(callInfo.onError)
                            )
                        } else {
                            subscribeIo(
                                callInfo.observable as Observable<Any>,
                                callInfo.onNext as ConsumerSafe<Any>,
                                onComplete,
                                requireNotNull(callInfo.onError)
                            )
                        }
                    } else {
                        subscribeIoTakeLastFrozen(
                            callInfo.observable as Observable<Any>,
                            callInfo.onNext as ConsumerSafe<Any>,
                            requireNotNull(callInfo.onError)
                        )
                    }
                }
                is SingleCallInfo<*> -> {
                    subscribeIo(
                        callInfo.single as Single<Any>,
                        callInfo.onSuccess as ConsumerSafe<Any>,
                        requireNotNull(callInfo.onError)
                    )
                }
            }
        }
    }

    protected fun <T> createObservableOperatorFreeze(replaceFrozenEventPredicate: BiFunctionSafe<T, T, Boolean>): ObservableOperatorFreeze<T>? {
        return if (currentFreezeSelector != null)  ObservableOperatorFreeze(currentFreezeSelector, replaceFrozenEventPredicate) else null
    }

    protected fun <T> createObservableOperatorFreeze(): ObservableOperatorFreeze<T>? {
        return if (currentFreezeSelector != null) ObservableOperatorFreeze(currentFreezeSelector) else null
    }

    protected fun <T> createSingleOperatorFreeze(): SingleOperatorFreeze<T>? {
        return if (currentFreezeSelector != null) SingleOperatorFreeze(currentFreezeSelector) else null
    }

    protected fun createCompletableOperatorFreeze(): CompletableOperatorFreeze? {
        return if (currentFreezeSelector != null)  CompletableOperatorFreeze(currentFreezeSelector) else null
    }

    protected fun <T> createMaybeOperatorFreeze(): MaybeOperatorFreeze<T>? {
        return if (currentFreezeSelector != null)  MaybeOperatorFreeze(currentFreezeSelector) else null
    }

    protected fun <Action, Actor> Collection<BaseTaggedViewModelAction<Actor>>.findActionByTag(
        tag: String,
        clazz: Class<Action>
    ) = find { it.tag == tag && clazz.isInstance(it) } as? Action

    protected fun <A> MutableCollection<out BaseTaggedViewModelAction<A>>.removeActionsByTag(tag: String) =
        removeAll { it.tag == tag }

    @JvmOverloads
    protected fun <T: BaseViewModelAction<*>> LiveData<VmListEvent<T>>.newEvent(value: T, options: VmListEvent.AddOptions = VmListEvent.AddOptions()): VmListEvent<T> {
        return this.value?.new(value, options) ?: VmListEvent(value, options)
    }

    protected fun <T: BaseViewModelAction<*>> LiveData<VmListEvent<T>>.newEvent(collection: Map<T, VmListEvent.AddOptions>): VmListEvent<T> {
        return this.value?.new(collection) ?: VmListEvent(collection)
    }

    @JvmOverloads
    protected fun <T: BaseViewModelAction<*>> MutableLiveData<VmListEvent<T>>.setNewEvent(
        value: T,
        options: VmListEvent.AddOptions = VmListEvent.AddOptions(),
        setOrPost: Boolean = true
    ): VmListEvent<T> {
        val event = newEvent(value, options)
        if (setOrPost) {
            this.value = event
        } else {
            postValue(event)
        }
        return event
    }

    protected fun <T: BaseViewModelAction<*>> MutableLiveData<VmListEvent<T>>.setNewEvent(
        collection: Map<T, VmListEvent.AddOptions>,
        setOrPost: Boolean = true
    ): VmListEvent<T> {
        val event = newEvent(collection)
        if (setOrPost) {
            this.value = event
        } else {
            postValue(event)
        }
        return event
    }

    private fun handleError(
        e: Throwable,
        onError: ConsumerSafe<Throwable>?,
        callInfo: BaseCallInfo
    ) {
        handleError(e, callInfo)
        onError?.accept(e)
    }
}