package net.maxsmr.mxstemplate.di.app

import android.content.Context
import dagger.BindsInstance
import dagger.Component
import dagger.android.AndroidInjector
import net.maxsmr.mxstemplate.TemplateApp
import net.maxsmr.mxstemplate.db.AppDataBase
import net.maxsmr.mxstemplate.di.PerApplication
import net.maxsmr.mxstemplate.di.ui.ActivitiesModule
import javax.inject.Named

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