package net.maxsmr.mxstemplate.di

import android.content.Context
import dagger.BindsInstance
import dagger.Component
import dagger.android.AndroidInjector
import net.maxsmr.mxstemplate.TemplateApp
import net.maxsmr.mxstemplate.db.AppDataBase
import net.maxsmr.mxstemplate.di.app.AppInfoModule
import net.maxsmr.mxstemplate.di.app.AppModule
import net.maxsmr.mxstemplate.di.app.DatabaseModule
import net.maxsmr.mxstemplate.di.app.SecondaryAppModule
import net.maxsmr.mxstemplate.di.ui.ActivitiesModule

@PerApplication
@Component(modules = [
    AppModule::class,
    ActivitiesModule::class,
    DatabaseModule::class,
    SecondaryAppModule::class,
    AppInfoModule::class
])
interface AppComponent : AndroidInjector<TemplateApp> {

    @Component.Builder
    interface Builder {

        @BindsInstance
        fun application(application: TemplateApp): Builder

        fun build(): AppComponent
    }

    fun database(): AppDataBase

    fun context(): Context

    // раньше тут были перечислены proxy-методы для инжектов в целевые VM, view и т.д.,
    // которые провайдились из модулей этого компонента
}