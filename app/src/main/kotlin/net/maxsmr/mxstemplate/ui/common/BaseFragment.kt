package net.maxsmr.mxstemplate.ui.common

import android.os.Bundle
import android.view.View
import androidx.annotation.CallSuper
import androidx.lifecycle.*
import androidx.savedstate.SavedStateRegistryOwner
import com.agna.ferro.rx.MaybeOperatorFreeze
import com.agna.ferro.rx.ObservableOperatorFreeze
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasAndroidInjector
import dagger.android.support.AndroidSupportInjection
import io.reactivex.MaybeOperator
import io.reactivex.ObservableOperator
import me.ilich.juggler.states.State
import net.maxsmr.commonutils.android.gui.actions.BaseViewModelAction
import net.maxsmr.commonutils.android.gui.actions.dialog.DialogFragmentHideMessageAction
import net.maxsmr.commonutils.android.gui.actions.dialog.DialogFragmentShowMessageAction
import net.maxsmr.commonutils.android.gui.actions.message.AlertDialogMessageAction
import net.maxsmr.commonutils.android.gui.actions.message.SnackMessageAction
import net.maxsmr.commonutils.android.gui.actions.message.ToastMessageAction
import net.maxsmr.commonutils.android.gui.fragments.dialogs.holder.DialogFragmentsHolder
import net.maxsmr.commonutils.rx.live.LiveMaybe
import net.maxsmr.commonutils.rx.live.LiveObservable
import net.maxsmr.commonutils.rx.live.LiveSubject
import net.maxsmr.commonutils.rx.live.event.VmEvent
import net.maxsmr.commonutils.rx.live.event.VmListEvent
import net.maxsmr.core_common.ui.actions.NavigationAction
import net.maxsmr.core_common.ui.dialog.CustomViewProgressable
import net.maxsmr.core_common.ui.viewmodel.BaseViewModel
import net.maxsmr.jugglerhelper.fragments.BaseJugglerFragment
import net.maxsmr.mxstemplate.R
import net.maxsmr.mxstemplate.di.ui.BaseVmFactory
import javax.inject.Inject

abstract class BaseFragment<VM : BaseViewModel<*>> : BaseJugglerFragment(), HasAndroidInjector {

    protected abstract val viewModelClass: Class<out VM>

    protected abstract val viewModelFactory: BaseVmFactory<VM>?

    /**
     * Если вам нужна ViewModel уровня Activity (подходящая для передачи данных между
     * фрагментами, например, при реализации мастера из нескольких фрагментов, работающих
     * с общими данными, переопределите данную функцию в true
     */
    protected open val isSharedViewModel: Boolean = false

    protected val dialogFragmentsHolder = DialogFragmentsHolder()

    @Inject
    lateinit var androidInjector: DispatchingAndroidInjector<Any>

    protected open var freezeEventsOnPause = true

    protected lateinit var viewModel: VM

    private val blockingProgress: CustomViewProgressable by lazy {
        CustomViewProgressable(requireContext(), R.layout.layout_progress_without_text)
    }

    override fun androidInjector(): AndroidInjector<Any> = androidInjector

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidSupportInjection.inject(this)
        with(savedInstanceState) {
            super.onCreate(this)
            viewModel = createViewModel(this)
            beforeRestoreFromBundle(this)
            viewModel.restoreFromBundle(this)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dialogFragmentsHolder.init(viewLifecycleOwner, childFragmentManager)
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

    protected abstract fun onViewCreated(view: View, savedInstanceState: Bundle?, viewModel: VM)

    @CallSuper
    protected open fun createViewModel(savedInstanceState: Bundle?): VM {
        val owner: ViewModelStoreOwner = if (isSharedViewModel) requireActivity() else this
        return createViewModelFactory(savedInstanceState)?.let {
            ViewModelProvider(owner, it)[viewModelClass]
        } ?: ViewModelProvider(owner)[viewModelClass]
    }

    protected open fun beforeRestoreFromBundle(savedInstanceState: Bundle?) {
        // override if needed
    }
    @CallSuper
    protected open fun subscribeOnActions(viewModel: VM) {
        subscribeOnDialogEvents()
        viewModel.navigationCommands.observeListEvents { handleNavigationAction(it) }
        viewModel.toastMessageCommands.observeListEvents { handleToastMessageAction(it) }
        viewModel.snackMessageCommands.observeListEvents { handleSnackMessageAction(it) }
        viewModel.showMessageCommands.observeListEvents { handleTypedDialogShowMessageAction(it) }
        viewModel.hideMessageCommands.observeListEvents { handleTypedDialogHideMessageAction(it) }
        viewModel.alertDialogMessageCommands.observeListEvents { handleDialogMessageAction(it) }
    }

    protected open fun handleNavigationAction(action: NavigationAction) {
        action.doAction(navigateTo())
    }

    protected open fun handleToastMessageAction(action: ToastMessageAction) {
        action.doAction(requireContext())
    }

    protected open fun handleSnackMessageAction(action: SnackMessageAction) {
        getViewForSnack(action)?.let { view ->
            action.view = view
            action.doAction(requireContext())
        }
    }

    protected open fun handleDialogMessageAction(action: AlertDialogMessageAction) {
        action.doAction(requireContext())
    }

    protected open fun handleTypedDialogShowMessageAction(action: DialogFragmentShowMessageAction) {
        action.doAction(dialogFragmentsHolder)
    }

    protected open fun handleTypedDialogHideMessageAction(action: DialogFragmentHideMessageAction) {
        action.doAction(dialogFragmentsHolder)
    }

    protected open fun getViewForSnack(action: SnackMessageAction): View? = view

    /**
     * Подписка на возможные диаложные ивенты с фрагментов на этом экране, пока экран существует
     */
    protected open fun subscribeOnDialogEvents() {
        // override if needed
    }

    protected fun <A : BaseViewModelAction<*>> subscribeOnAction(action: LiveSubject<A>, consumer: (A) -> Unit) {
        action.subscribe(viewLifecycleOwner, onNext = consumer)
    }

    protected fun setBlockingProgressVisible(visible: Boolean) {
        if (visible) blockingProgress.onStart() else blockingProgress.onStop()
    }

    private fun createViewModelFactory(savedInstanceState: Bundle?): WrapperVmFactory<VM>? {
        val owner: LifecycleOwner = if (isSharedViewModel) requireActivity() else this
        return viewModelFactory?.let {
            WrapperVmFactory(it, getParamsOrThrow(), owner as SavedStateRegistryOwner, savedInstanceState)
        }
    }

    @JvmOverloads
    protected inline fun <T> LiveData<T>.observe(owner: LifecycleOwner = viewLifecycleOwner, crossinline onNext: (T) -> Unit) {
        this.observe(owner, Observer { onNext(it) })
    }

    @JvmOverloads
    protected inline fun <T> LiveData<VmEvent<T>>.observeEvents(owner: LifecycleOwner = viewLifecycleOwner, crossinline onNext: (T) -> Unit) {
        this.observe(owner, Observer {
            it.get()?.let(onNext)
        })
    }

    @JvmOverloads
    protected inline fun <T> LiveData<VmListEvent<T>>.observeListEvents(owner: LifecycleOwner = viewLifecycleOwner, crossinline onNext: (T) -> Unit) {
        this.observe(owner, Observer { event ->
            event.getAll().let { list ->
                list.forEach {
                    onNext(it)
                }
            }
        })
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

    @JvmOverloads
    protected fun <T> LiveMaybe<T>.subscribeMaybe(
        owner: LifecycleOwner = viewLifecycleOwner,
        operator: MaybeOperator<T, T>? = MaybeOperatorFreeze(viewModel.getCurrentSelector(this::class.java.name)),
        emitOnce: Boolean = false,
        onNext: (T) -> Unit
    ) {
        subscribe(owner, operator, emitOnce, onNext)
    }

    /**
     * Оборачивает [factory], предоставляя ей [SavedStateHandle] в метод [BaseVmFactory.create].
     *
     * @param factory фабрика, содержащая остальные параметры для создания VM (помимо [SavedStateHandle])
     */
    private class WrapperVmFactory<VM : BaseViewModel<*>>(
        private val factory: BaseVmFactory<VM>,
        private val params: State.Params,
        owner: SavedStateRegistryOwner,
        defaultArgs: Bundle?
    ): AbstractSavedStateViewModelFactory(owner, defaultArgs) {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(key: String, modelClass: Class<T>, handle: SavedStateHandle): T {
            return factory.create(handle, params) as T
        }
    }
}