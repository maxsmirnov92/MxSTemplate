package net.maxsmr.mxstemplate.ui.common

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.Intent.ACTION_LOCALE_CHANGED
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.res.AssetManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.view.MenuItem
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import dagger.android.AndroidInjection
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasAndroidInjector
import net.maxsmr.commonutils.gui.fragments.dialogs.holder.DialogFragmentsHolder
import net.maxsmr.core_common.BaseApplication
import net.maxsmr.core_common.LocaleContextWrapper
import net.maxsmr.jugglerhelper.activities.BaseJugglerActivity
import net.maxsmr.mxstemplate.R
import net.maxsmr.mxstemplate.ui.common.permissions.DialogHolderDeniedPermissionsHandler
import net.maxsmr.permissionchecker.BaseDeniedPermissionsHandler
import net.maxsmr.permissionchecker.PermissionsHelper
import pub.devrel.easypermissions.EasyPermissions
import java.util.*
import javax.inject.Inject

abstract class BaseActivity : BaseJugglerActivity(), HasAndroidInjector, EasyPermissions.PermissionCallbacks {

    val dialogFragmentsHolder = DialogFragmentsHolder().apply {
        // DialogFragment может быть показан только один в общем случае
        showRule = DialogFragmentsHolder.ShowRule.SINGLE
    }

    protected open val permissionsHelper = PermissionsHelper(BaseApplication.context.getSharedPreferences(PREFS_NAME_PERMANENTLY_DENIED, Context.MODE_PRIVATE))

    private val localeChangedReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            startActivity(getIntent())
            finish()
        }
    }

    /**
     * При первом запросе разрешений из PermissionsWrapper запоминаем [PermissionsHelper.ResultListener] по данному коду,
     * чтобы в дальнейшем отчитаться о результате из onRequestPermissionsResult или onActivityResult;
     */
    private val permissionResultListeners = mutableMapOf<Int, PermissionsHelper.ResultListener?>()

    @Inject
    lateinit var androidInjector: DispatchingAndroidInjector<Any>

    /**
     * Отображатель пользовательских permanently denied диалогов;
     * лежит на уровне базовой активити для получения доступа не только из фрагмента, но и из View
     */
    protected lateinit var permanentlyDeniedPermissionsHandler: BaseDeniedPermissionsHandler

    override fun androidInjector(): AndroidInjector<Any> = androidInjector

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)
        dialogFragmentsHolder.init(this, supportFragmentManager)
        initPermanentlyDeniedPermissionsHandler()
        LocalBroadcastManager.getInstance(this).registerReceiver(
            localeChangedReceiver,
            IntentFilter(ACTION_LOCALE_CHANGED)
        )
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        super.onOptionsItemSelected(item)
        return false
    }

    override fun onDestroy() {
        super.onDestroy()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(localeChangedReceiver)
    }

    override fun attachBaseContext(newBase: Context) {
//        val lang = LocaleManager.instance().loadLanguage()
        super.attachBaseContext(LocaleContextWrapper.wrap(newBase, Locale.getDefault()))
    }

    override fun applyOverrideConfiguration(overrideConfiguration: Configuration) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && Build.VERSION.SDK_INT <= Build.VERSION_CODES.N_MR1) {
//            val lang: LocaleManager.Lang = LocaleManager.instance().loadLanguage()
            overrideConfiguration.setLocale(Locale.getDefault())
        }
        super.applyOverrideConfiguration(overrideConfiguration)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        permissionResultListeners.remove(requestCode)?.onRequestPermissionsResult(permissions, grantResults)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        permissionResultListeners.remove(requestCode)?.onActivityResult()
    }

    final override fun onPermissionsGranted(requestCode: Int, perms: MutableList<String>) {
        notifyPermissionsResult(requestCode, true)
    }

    final override fun onPermissionsDenied(requestCode: Int, perms: MutableList<String>) {
        notifyPermissionsResult(requestCode, false)
    }

    override fun getAssets(): AssetManager {
        // при переопределении конфигурации (например, в LocaleContextWrapper или applyOverrideConfiguration)
        // возвращает другой инстанс в Context.getAssets() и Context.getResources().getAssets()
        // и не находит там нужные строки
        // начиная с версии appCompat 1.3
        return resources.assets
    }

    protected open fun initPermanentlyDeniedPermissionsHandler() {
        permanentlyDeniedPermissionsHandler = DialogHolderDeniedPermissionsHandler(dialogFragmentsHolder, this)
    }

    @JvmOverloads
    fun doOnPermissionsResult(
        code: Int,
        permissions: Collection<String>,
        rationale: String = getString(R.string.dialog_message_permission_request_rationale),
        shouldShowPermanentlyDeniedDialog: Boolean = true,
        onDenied: ((Set<String>) -> Unit)? = null,
        onNegativePermanentlyDeniedAction:  ((Set<String>) -> Unit)? = onDenied,
        onAllGranted: () -> Unit,
    ): PermissionsHelper.ResultListener? {
        return permissionsHelper.doOnPermissionsResult(
            this,
            rationale,
            code,
            permissions.toSet(),
            if (shouldShowPermanentlyDeniedDialog) permanentlyDeniedPermissionsHandler else null,
            onDenied,
            onNegativePermanentlyDeniedAction,
            onAllGranted
        ).apply {
            permissionResultListeners[code] = this
        }
    }

    private fun notifyPermissionsResult(requestCode: Int, isAllGranted: Boolean) {
        permissionResultListeners.remove(requestCode)?.let {
            it.onRequestPermissionsResult(it.allPermissions.toTypedArray(),
                it.allPermissions.map {
                    if (isAllGranted) PackageManager.PERMISSION_GRANTED else PackageManager.PERMISSION_DENIED }.toIntArray())
        }
    }

    companion object {

        private const val PREFS_NAME_PERMANENTLY_DENIED = "PermanentlyDeniedPrefs"
    }
}