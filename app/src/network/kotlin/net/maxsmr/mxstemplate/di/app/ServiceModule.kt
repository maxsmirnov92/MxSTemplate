package net.maxsmr.mxstemplate.di.app

import dagger.Module
import dagger.android.ContributesAndroidInjector
import dagger.android.support.AndroidSupportInjectionModule
import net.maxsmr.mxstemplate.di.PerService

@Module(includes = [AndroidSupportInjectionModule::class])
interface ServiceModule {

    //    @ContributesAndroidInjector
//    @PerService
//    fun logSendIntentService(): LogSendIntentService

//    @PerService
//    @ContributesAndroidInjector
//    fun downloadIntentServiceInjector(): DownloadIntentService
}