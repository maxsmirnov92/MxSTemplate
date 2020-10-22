package net.maxsmr.mxstemplate.di

import net.maxsmr.mxstemplate.TemplateApp
import net.maxsmr.mxstemplate.di.app.AppComponent
import net.maxsmr.mxstemplate.di.app.DaggerAppComponent

object AppInjector {

    @JvmStatic
    lateinit var component: AppComponent
        private set

    @JvmStatic
    fun init(app: TemplateApp) {
        component = DaggerAppComponent.builder().application(app).build()
        component.inject(app)
    }
}