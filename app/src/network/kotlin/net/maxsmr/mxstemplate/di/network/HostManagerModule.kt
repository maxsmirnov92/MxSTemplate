package net.maxsmr.mxstemplate.di.network

import android.content.Context
import dagger.Module
import dagger.Provides
import net.maxsmr.core_network.HostManager
import net.maxsmr.mxstemplate.api.HostManagerImpl
import net.maxsmr.mxstemplate.di.PerApplication
import net.maxsmr.mxstemplate.di.DI_NAME_HOST_MANAGER
import javax.inject.Named

@Module
class HostManagerModule {

    @Provides
    @PerApplication
    @Named(DI_NAME_HOST_MANAGER)
    fun provideHostManager(context: Context): HostManager = HostManagerImpl()

}