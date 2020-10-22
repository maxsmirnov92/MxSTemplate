package net.maxsmr.mxstemplate.ui.common

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.Intent.ACTION_LOCALE_CHANGED
import android.content.IntentFilter
import android.content.res.AssetManager
import android.os.Bundle
import android.view.View
import androidx.annotation.CallSuper
import androidx.lifecycle.*
import androidx.lifecycle.Observer
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import dagger.android.AndroidInjection
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasAndroidInjector
import net.maxsmr.commonutils.android.gui.actions.BaseViewModelAction
import net.maxsmr.commonutils.android.gui.actions.dialog.DialogFragmentHideMessageAction
import net.maxsmr.commonutils.android.gui.actions.dialog.DialogFragmentShowMessageAction
import net.maxsmr.commonutils.android.gui.actions.message.*
import net.maxsmr.commonutils.android.gui.fragments.dialogs.holder.DialogFragmentsHolder
import net.maxsmr.commonutils.rx.live.LiveSubject
import net.maxsmr.commonutils.rx.live.event.VmEvent
import net.maxsmr.commonutils.rx.live.event.VmListEvent
import net.maxsmr.core_common.utils.LocaleContextWrapper
import net.maxsmr.jugglerhelper.activities.BaseJugglerActivity
import net.maxsmr.core_common.ui.actions.NavigationAction
import net.maxsmr.core_common.ui.viewmodel.BaseViewModel
import pub.devrel.easypermissions.EasyPermissions
import java.util.*
import javax.inject.Inject

abstract class BaseActivity : BaseJugglerActivity(), HasAndroidInjector {

    protected open val dialogFragmentsHolder = DialogFragmentsHolder()

    private val localeChangedReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            startActivity(getIntent())
            finish()
        }
    }

    @Inject
    lateinit var androidInjector: DispatchingAndroidInjector<Any>

    override fun androidInjector(): AndroidInjector<Any> = androidInjector

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)
        dialogFragmentsHolder.init(this, supportFragmentManager)
        LocalBroadcastManager.getInstance(this).registerReceiver(localeChangedReceiver,
                IntentFilter(ACTION_LOCALE_CHANGED))
    }

    override fun onDestroy() {
        super.onDestroy()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(localeChangedReceiver)
    }

    override fun attachBaseContext(newBase: Context?) {
//        val lang = LocaleManager.instance().loadLanguage()
        super.attachBaseContext(LocaleContextWrapper.wrap(newBase, Locale.getDefault()))
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
    }

    override fun getAssets(): AssetManager {
        // RZD-9263 при переопределении конфигурации (например, в LocaleContextWrapper или applyOverrideConfiguration)
        // возвращает другой инстанс в Context.getAssets() и Context.getResources().getAssets()
        // и не находит там нужные строки
        // начиная с версии appCompat 1.3
        return resources.assets
    }

    /**
     * Подписка на данные из [VM]
     */
    @CallSuper
    protected open fun <VM : BaseViewModel<*>> observeViewModel(vm: VM, savedInstanceState: Bundle?) {
        subscribeOnActions(vm)
    }

    /**
     * Подписка на возможные диаложные ивенты с фрагментов на этом экране, пока экран существует
     */
    protected open fun subscribeOnDialogEvents() {
        // override if needed
    }

    protected open fun <VM : BaseViewModel<*>> subscribeOnActions(vm: VM) {
        subscribeOnDialogEvents()
        vm.navigationCommands.observeListEvents { handleNavigationAction(it) }
        vm.toastMessageCommands.observeListEvents { handleToastMessageAction(it) }
        vm.snackMessageCommands.observeListEvents { handleSnackMessageAction(it) }
        vm.showMessageCommands.observeListEvents { handleTypedDialogShowMessageAction(it) }
        vm.hideMessageCommands.observeListEvents { handleTypedDialogHideMessageAction(it) }
        vm.alertDialogMessageCommands.observeListEvents { handleDialogMessageAction(it) }
    }

    protected open fun handleNavigationAction(action: NavigationAction) {
        action.doAction(navigateTo())
    }

    protected open fun handleToastMessageAction(action: ToastMessageAction) {
        action.doAction(this)
    }

    protected open fun handleSnackMessageAction(action: SnackMessageAction) {
        window?.decorView?.findViewById<View>(android.R.id.content)?.let {
            action.view = it
            action.doAction(this)
        }
    }

    protected open fun handleDialogMessageAction(action: AlertDialogMessageAction) {
        action.doAction(this)
    }

    protected open fun handleTypedDialogShowMessageAction(action: DialogFragmentShowMessageAction) {
        action.doAction(dialogFragmentsHolder)
    }

    protected open fun handleTypedDialogHideMessageAction(action: DialogFragmentHideMessageAction) {
        action.doAction(dialogFragmentsHolder)
    }

    protected fun <A : BaseViewModelAction<*>> subscribeOnAction(action: LiveSubject<A>, consumer: (A) -> Unit) {
        action.subscribe(this, onNext = consumer)
    }

    protected inline fun <T> LiveData<T>.observe(owner: LifecycleOwner = this@BaseActivity, crossinline onNext: (T) -> Unit) {
        this.observe(owner, Observer { onNext(it) })
    }

    protected inline fun <T> LiveData<VmEvent<T>>.observeEvents(owner: LifecycleOwner = this@BaseActivity, crossinline onNext: (T) -> Unit) {
        this.observe(owner, Observer {
            it.get()?.let(onNext)
        })
    }

    protected inline fun <T> LiveData<VmListEvent<T>>.observeListEvents(owner: LifecycleOwner = this@BaseActivity, crossinline onNext: (T) -> Unit) {
        this.observe(owner, Observer { event ->
            event.getAll().let { list ->
                list.forEach {
                    onNext(it)
                }
            }
        })
    }

    protected fun <T> LiveSubject<T>.observe(owner: LifecycleOwner = this@BaseActivity, onNext: (T) -> Unit) {
        this.subscribe(owner, onNext = onNext)
    }
}