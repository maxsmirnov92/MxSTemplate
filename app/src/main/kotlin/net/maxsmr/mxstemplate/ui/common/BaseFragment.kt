package net.maxsmr.mxstemplate.ui.common

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.annotation.CallSuper
import androidx.lifecycle.*
import androidx.savedstate.SavedStateRegistryOwner
import com.agna.ferro.rx.MaybeOperatorFreeze
import com.agna.ferro.rx.ObservableOperatorFreeze
import com.google.android.material.snackbar.Snackbar
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasAndroidInjector
import dagger.android.support.AndroidSupportInjection
import io.reactivex.MaybeOperator
import io.reactivex.ObservableOperator
import me.ilich.juggler.states.State
import net.maxsmr.commonutils.gui.actions.BaseViewModelAction
import net.maxsmr.commonutils.gui.actions.dialog.DialogBuilderFragmentShowMessageAction
import net.maxsmr.commonutils.gui.actions.dialog.DialogFragmentHideMessageAction
import net.maxsmr.commonutils.gui.actions.message.AlertDialogMessageAction
import net.maxsmr.commonutils.gui.actions.message.BaseMessageAction
import net.maxsmr.commonutils.gui.fragments.dialogs.TypedDialogFragment
import net.maxsmr.commonutils.gui.fragments.dialogs.holder.DialogFragmentsHolder
import net.maxsmr.commonutils.live.event.VmEvent
import net.maxsmr.commonutils.live.event.VmListEvent
import net.maxsmr.commonutils.rx.live.LiveMaybe
import net.maxsmr.commonutils.rx.live.LiveObservable
import net.maxsmr.commonutils.rx.live.LiveSubject
import net.maxsmr.core_common.permissions.DialogHolderPermissionsRationaleHandler
import net.maxsmr.core_common.ui.actions.NavigationAction
import net.maxsmr.core_common.ui.viewmodel.BaseViewModel
import net.maxsmr.core_common.ui.viewmodel.BaseVmFactory
import net.maxsmr.core_common.ui.viewmodel.delegates.VmFactoryParams
import net.maxsmr.jugglerhelper.fragments.BaseJugglerFragment
import net.maxsmr.permissionchecker.BasePermissionsRationaleHandler
import net.maxsmr.permissionchecker.PermissionHandle
import pub.devrel.easypermissions.EasyPermissions
import javax.inject.Inject

abstract class BaseFragment<VM : BaseViewModel<*>> : BaseJugglerFragment(), HasAndroidInjector {

    /**
     * Возвращает все необходимые параметры для получения инстанса [viewModel]
     */
    protected abstract val vmFactoryParams: VmFactoryParams<VM>

    protected val dialogFragmentsHolder = DialogFragmentsHolder().apply {
        // DialogFragment может быть показан только один в общем случае
        showRule = DialogFragmentsHolder.ShowRule.SINGLE
    }

    @Inject
    lateinit var androidInjector: DispatchingAndroidInjector<Any>

    /**
     * Отображатель пользовательских rationale-диалогов для api 28
     */
    lateinit var rationaleHandler: BasePermissionsRationaleHandler
    /**
     * При первом запросе разрешений из PermissionsWrapper запоминаем ссылку на [PermissionHandle],
     * чтобы в дальнейшем отчитаться о результате из onRequestPermissionsResult;
     * в таком случае аннотацию [AfterPermissionGranted] на свой метод ставить не надо,
     * а в другом случае (без EasyPermissions.onRequestPermissionsResult, который там вызывается) оно просто не сработает
     */
    var permissionHandle: PermissionHandle? = null

    protected open lateinit var permissionsRationaleHandler: BasePermissionsRationaleHandler

    protected open var freezeEventsOnPause = true

    protected lateinit var viewModel: VM
        private set

    override fun androidInjector(): AndroidInjector<Any> = androidInjector

    override fun onAttach(context: Context) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        with(savedInstanceState) {
            super.onCreate(this)
            viewModel = createViewModel(this)
            beforeRestoreFromBundle(this)
            viewModel.restoreFromBundle(this)
        }
    }

    @CallSuper
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dialogFragmentsHolder.init(viewLifecycleOwner, childFragmentManager)
        // FIXME сделать DialogHolder реализацию
//        rationaleHandler =
        initPermissionsRationaleHandler()
        subscribeOnActions(viewModel)
        onViewCreated(view, savedInstanceState, viewModel)
    }

    override fun onResume() {
        super.onResume()
        viewModel.notifyResumed(this::class.java.name)
    }

    override fun onPause() {
        super.onPause()
        if (freezeEventsOnPause) {
            viewModel.notifyPaused(this::class.java.name)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        viewModel.saveToBundle(outState)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        permissionHandle?.onRequestPermissionsResult(requestCode, permissions, grantResults) ?:
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
    }

    protected abstract fun onViewCreated(view: View, savedInstanceState: Bundle?, viewModel: VM)

    @CallSuper
    protected open fun createViewModel(savedInstanceState: Bundle?): VM {
        val params = vmFactoryParams
        val owner = if (params.isShared) requireActivity() else this
        val factory = params.factory?.let {
            WrapperVmFactory(it, getParamsOrThrow(), owner, savedInstanceState)
        }
        return factory?.let {
            ViewModelProvider(owner, it)[params.clazz]
        } ?: ViewModelProvider(owner)[params.clazz]
    }

    protected open fun beforeRestoreFromBundle(savedInstanceState: Bundle?) {
        // override if needed
    }

    protected open fun initPermissionsRationaleHandler() {
        permissionsRationaleHandler = DialogHolderPermissionsRationaleHandler(dialogFragmentsHolder, viewLifecycleOwner)
    }

    @CallSuper
    protected open fun subscribeOnActions(viewModel: VM) {
        subscribeOnDialogEvents()
        viewModel.navigationCommands.observeListEvents { action, _ -> handleNavigationAction(action.item) }
        viewModel.toastMessageCommands.observeListEvents { action, listener ->
            handleToastMessageAction(action, listener, false)
            // слушать dismiss-колбеки здесь не надо, т.к. уже очистили свою очередь - показ разруливается системой
        }
        viewModel.snackMessageCommands.observeListEvents(removeEvents = false) { action, listener ->
            handleSnackMessageAction(action, listener, true)
        }
        viewModel.showDialogCommands.observeListEvents(removeEvents = false) { action, listener ->
            handleTypedDialogShowMessageAction(action, listener, true)
        }
        viewModel.hideDialogCommands.observeListEvents { action, _ -> handleTypedDialogHideMessageAction(action.item) }
        viewModel.alertDialogMessageCommands.observeListEvents { action, _ -> handleDialogMessageAction(action.item) }
    }

    protected open fun handleNavigationAction(action: NavigationAction) {
        action.doAction(navigateTo())
    }

    protected open fun handleToastMessageAction(
        actionInfo: VmListEvent.ItemInfo<BaseMessageAction<Toast, Context>>,
        listener: NextActionListener<BaseMessageAction<Toast, Context>>,
        listenDismissEvents: Boolean
    ) {
        val action = actionInfo.item
        action.doAction(requireContext())
        if (listenDismissEvents && action.show) {
            action.lastShowed?.addCallback(object : Toast.Callback() {
                override fun onToastHidden() {
                    listener.onNext(listOf(actionInfo))
                }
            })
        }
    }

    protected open fun handleSnackMessageAction(
        actionInfo: VmListEvent.ItemInfo<BaseMessageAction<Snackbar, View>>,
        listener: NextActionListener<BaseMessageAction<Snackbar, View>>,
        listenDismissEvents: Boolean
    ) {
        val action = actionInfo.item
        getViewForSnack(action).let { view ->
            action.doAction(view)
            if (listenDismissEvents && action.show) {
                action.lastShowed?.addCallback(object : Snackbar.Callback() {
                    override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                        // при пропадании этого снека (по кнопке, таймауту etc.) спрашиваем следующую порцию
                        listener.onNext(listOf(actionInfo))
                    }
                })
            }
        }
    }

    @Deprecated("use DialogBuilderFragmentShowMessageAction", replaceWith = ReplaceWith("handleTypedDialogShowMessageAction"))
    protected open fun handleDialogMessageAction(action: AlertDialogMessageAction) {
        action.doAction(requireContext())
    }

    protected open fun handleTypedDialogShowMessageAction(
        actionInfo: VmListEvent.ItemInfo<DialogBuilderFragmentShowMessageAction<*,*>>,
        listener: NextActionListener<DialogBuilderFragmentShowMessageAction<*,*>>,
        listenDismissEvents: Boolean
    ) {
        val action = actionInfo.item
        action.doAction(dialogFragmentsHolder)
        if (listenDismissEvents) {
            dialogFragmentsHolder.dismissLiveEvents(action.tag, TypedDialogFragment::class.java, null).subscribeObservableOnce {
                // при пропадании этого диалога, спрашиваем следующую порцию
                listener.onNext(listOf(actionInfo))
            }
        }
    }

    protected open fun handleTypedDialogHideMessageAction(action: DialogFragmentHideMessageAction) {
        action.doAction(dialogFragmentsHolder)
    }

    protected open fun getViewForSnack(action: BaseMessageAction<Snackbar, View>): View = requireView()

    /**
     * Подписка на возможные диаложные ивенты с фрагментов на этом экране, пока экран существует
     */
    protected open fun subscribeOnDialogEvents() {
        // override if needed
    }

    protected fun <A : BaseViewModelAction<*>> subscribeOnAction(action: LiveSubject<A>, consumer: (A) -> Unit) {
        action.subscribe(viewLifecycleOwner, onNext = consumer)
    }

    @JvmOverloads
    protected inline fun <T> LiveData<T>.observe(owner: LifecycleOwner = viewLifecycleOwner, crossinline onNext: (T) -> Unit) {
        this.observe(owner, Observer { onNext(it) })
    }

    @JvmOverloads
    protected inline fun <T> LiveData<VmEvent<T>>.observeEvents(
        owner: LifecycleOwner = viewLifecycleOwner,
        crossinline onNext: (T) -> Unit
    ) {
        this.observe(owner, Observer {
            it.get()?.let(onNext)
        })
    }

    /**
     * @param removeEvents true, если при вычитывании [T] сразу требуется удаление из очереди - запоминание происходит в другом месте;
     * false - если удаление по тегу произойдёт только после обработки текущего(их) выведеденного - после скрытия
     */
    @JvmOverloads
    protected fun <T : BaseViewModelAction<*>> LiveData<VmListEvent<T>>.observeListEvents(
        owner: LifecycleOwner = viewLifecycleOwner,
        removeEvents: Boolean = true,
        onNext: (VmListEvent.ItemInfo<T>, NextActionListener<T>) -> Unit
    ) {
        this.observe(owner, Observer { events ->
            drainVmListEvents(events, removeEvents, onNext)
        })
    }

    private fun <T : BaseViewModelAction<*>> drainVmListEvents(
        listEvent: VmListEvent<T>,
        removeEvents: Boolean = true,
        onNext: (VmListEvent.ItemInfo<T>, NextActionListener<T>) -> Unit
    ) {
        listEvent.getAllBeforeSingle(removeEvents).let { list ->
            list.forEach {
                val listener = object : NextActionListener<T> {
                    override fun onNext(handledEvents: List<VmListEvent.ItemInfo<T>>) {
                        if (!removeEvents) {
                            handledEvents.forEach { event ->
                                listEvent.removeAllByTag(event.tag)
                            }
                        }
                        drainVmListEvents(listEvent, removeEvents, onNext)
                    }
                }
                onNext(it, listener)
            }
        }
    }

    @JvmOverloads
    protected fun <T> LiveObservable<T>.subscribeObservable(
        owner: LifecycleOwner = viewLifecycleOwner,
        operator: ObservableOperator<T, T>? = ObservableOperatorFreeze(viewModel.getCurrentSelector(this::class.java.name)),
        emitOnce: Boolean = false,
        onNext: (T) -> Unit
    ) {
        subscribe(owner, operator, emitOnce, onNext)
    }

    protected fun <T> LiveObservable<T>.subscribeObservableOnce(
        owner: LifecycleOwner = viewLifecycleOwner,
        operator: ObservableOperator<T, T>? = ObservableOperatorFreeze(viewModel.getCurrentSelector(this::class.java.name)),
        emitOnce: Boolean = false,
        onNext: (T) -> Unit
    ) {
        subscribe(owner, operator, emitOnce) {
            onNext(it)
            unsubscribe(owner)
        }
    }

    @JvmOverloads
    protected fun <T> LiveMaybe<T>.subscribeMaybe(
        owner: LifecycleOwner = viewLifecycleOwner,
        operator: MaybeOperator<T, T>? = MaybeOperatorFreeze(viewModel.getCurrentSelector(this::class.java.name)),
        emitOnce: Boolean = false,
        onNext: (T) -> Unit
    ) {
        subscribe(owner, operator, emitOnce, onNext)
    }

    @JvmOverloads
    protected fun <T> LiveMaybe<T>.subscribeMaybeOnce(
        owner: LifecycleOwner = viewLifecycleOwner,
        operator: MaybeOperator<T, T>? = MaybeOperatorFreeze(viewModel.getCurrentSelector(this::class.java.name)),
        emitOnce: Boolean = false,
        onNext: (T) -> Unit
    ) {
        subscribe(owner, operator, emitOnce) {
            onNext(it)
            unsubscribe(owner)
        }
    }

    /**
     * Клиенский код должен вызввать onNext по готовности вычитать оставшиеся ивенты
     * из очереди в порядке приоритета
     */
    interface NextActionListener<T : BaseViewModelAction<*>> {

        fun onNext(handledEvents: List<VmListEvent.ItemInfo<T>>)
    }

    /**
     * Оборачивает [factory], предоставляя ей [SavedStateHandle] в метод [BaseVmFactory.create].
     *
     * @param factory фабрика, содержащая остальные параметры для создания VM (помимо [SavedStateHandle])
     */
    private class WrapperVmFactory<VM : BaseViewModel<*>>(
        private val factory: BaseVmFactory<VM>,
        private val params: State.Params?,
        owner: SavedStateRegistryOwner,
        defaultArgs: Bundle?
    ) : AbstractSavedStateViewModelFactory(owner, defaultArgs) {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(key: String, modelClass: Class<T>, handle: SavedStateHandle): T {
            return factory.create(handle, params) as T
        }
    }
}