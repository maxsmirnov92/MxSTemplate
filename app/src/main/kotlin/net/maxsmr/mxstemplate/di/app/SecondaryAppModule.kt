package net.maxsmr.mxstemplate.di.app

import android.content.Context
import dagger.Module
import dagger.Provides
import net.maxsmr.core_common.arch.StringsProvider
import net.maxsmr.core_common.arch.rx.scheduler.SchedulersProvider
import net.maxsmr.core_common.arch.rx.scheduler.SchedulersProviderImpl
import net.maxsmr.mxstemplate.di.PerApplication

@Module
class SecondaryAppModule {

    @Provides
    @PerApplication
    fun provideSchedulerProvider(): SchedulersProvider = SchedulersProviderImpl()

    @Provides
    @PerApplication
    fun provideStringsProvider(context: Context): StringsProvider = StringsProvider(context)
}