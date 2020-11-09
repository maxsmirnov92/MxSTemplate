package net.maxsmr.mxstemplate.di

import android.content.Context
import com.squareup.picasso.Picasso
import dagger.BindsInstance
import dagger.Component
import dagger.android.AndroidInjector
import net.maxsmr.core_network.HostManager
import net.maxsmr.core_network.di.NetworkComponent
import net.maxsmr.mxstemplate.TemplateApp
import net.maxsmr.mxstemplate.db.AppDataBase
import net.maxsmr.mxstemplate.di.app.*
import net.maxsmr.mxstemplate.di.network.ApiRequestModule
import net.maxsmr.mxstemplate.di.network.HostManagerModule
import net.maxsmr.mxstemplate.di.network.NetworkModule
import net.maxsmr.mxstemplate.di.network.OkHttpModule
import net.maxsmr.mxstemplate.di.ui.ActivitiesModule
import javax.inject.Named

const val DI_NAME_HOST_MANAGER = "host_manager"

@PerApplication
@Component(modules = [
    AppModule::class,
    ServiceModule::class,
    ActivitiesModule::class,
    DatabaseModule::class,
    SecondaryAppModule::class,
    ApiRequestModule::class,
    AppInfoModule::class,
    HostManagerModule::class,
    NetworkModule::class,
    OkHttpModule::class
])
interface AppComponent : AndroidInjector<TemplateApp>, NetworkComponent {

    @Component.Builder
    interface Builder {

        @BindsInstance
        fun application(application: TemplateApp): Builder

        fun build(): AppComponent
    }

    @Named(DI_NAME_HOST_MANAGER)
    override fun hostManager(): HostManager

    fun database(): AppDataBase

    fun context(): Context

    fun picasso(): Picasso

    // раньше тут были перечислены proxy-методы для инжектов в целевые VM, view и т.д.,
    // которые провайдились из модулей этого компонента
}