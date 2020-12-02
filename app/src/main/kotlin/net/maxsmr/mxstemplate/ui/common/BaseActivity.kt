package net.maxsmr.mxstemplate.ui.common

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.Intent.ACTION_LOCALE_CHANGED
import android.content.IntentFilter
import android.content.res.AssetManager
import android.os.Bundle
import android.view.MenuItem
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import dagger.android.AndroidInjection
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasAndroidInjector
import net.maxsmr.core_common.LocaleContextWrapper
import net.maxsmr.jugglerhelper.activities.BaseJugglerActivity
import pub.devrel.easypermissions.EasyPermissions
import java.util.*
import javax.inject.Inject

abstract class BaseActivity : BaseJugglerActivity(), HasAndroidInjector {

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
        LocalBroadcastManager.getInstance(this).registerReceiver(localeChangedReceiver,
                IntentFilter(ACTION_LOCALE_CHANGED))
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        super.onOptionsItemSelected(item)
        return false
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
}