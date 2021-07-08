package net.maxsmr.core_common

import android.app.Activity
import android.app.Application
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import androidx.annotation.CallSuper
import androidx.multidex.MultiDexApplication
import io.reactivex.plugins.RxJavaPlugins
import net.maxsmr.commonutils.activity.ActiveActivityHolder
import net.maxsmr.commonutils.logger.BaseLogger
import net.maxsmr.commonutils.logger.LogcatLogger
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder
import java.util.*

const val VERSION_NOT_SET = -1
const val PLATFORM_NAME = "Android"

abstract class BaseApplication : MultiDexApplication(), Application.ActivityLifecycleCallbacks {

    companion object {

        @JvmStatic
        lateinit var context: BaseApplication
            private set
    }

    protected val activeActivityHolder = ActiveActivityHolder()

    override fun onCreate() {

        BaseLoggerHolder.initInstance { object: BaseLoggerHolder() {
            override fun createLogger(className: String): BaseLogger {
                return LogcatLogger(className)
            }
        } }


        super.onCreate()
        context = this


        registerActivityLifecycleCallbacks(this)
        RxJavaPlugins.setErrorHandler { Log.e("Application", "Rx error occurred: $it", it) }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // LocaleManager.instance().loadLanguage().getLocale()
        LocaleContextWrapper.updateConfigurationLocaleLegacy(this, Locale.getDefault())
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        // do nothing
    }

    override fun onActivityStarted(activity: Activity) {
        // do nothing
    }

    @CallSuper
    override fun onActivityResumed(activity: Activity) {
        activeActivityHolder.activity = activity
    }

    @CallSuper
    override fun onActivityPaused(activity: Activity) {
        activeActivityHolder.clearActivity()
    }

    override fun onActivityStopped(activity: Activity) {
        // do nothing
    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
        // do nothing
    }

    override fun onActivityDestroyed(activity: Activity) {
        // do nothing
    }


    open fun isRuOnlyLocale(): Boolean {
        return true
    }


    open fun getProtocolVersion(): Int {
        return VERSION_NOT_SET
    }


}