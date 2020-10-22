package net.maxsmr.mxstemplate

import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasAndroidInjector
import net.maxsmr.core_common.BaseApplication
import net.maxsmr.mxstemplate.di.AppInjector
import javax.inject.Inject

private const val ACTION_LOCALE_CHANGED: String = "locale_changed"

class TemplateApp : BaseApplication(), HasAndroidInjector {

    @Inject
    @Volatile
    lateinit var androidInjector: DispatchingAndroidInjector<Any>

    override fun onCreate() {
        AppInjector.init(this)
        super.onCreate()

    }

    override fun androidInjector(): AndroidInjector<Any> = androidInjector
}