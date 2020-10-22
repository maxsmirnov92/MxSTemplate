package net.maxsmr.mxstemplate.di.app

import android.content.Context
import dagger.Binds
import dagger.Module
import dagger.android.support.AndroidSupportInjectionModule
import net.maxsmr.mxstemplate.TemplateApp
import net.maxsmr.mxstemplate.di.PerApplication

@Module(includes = [AndroidSupportInjectionModule::class])
interface AppModule {

    @Binds
    @PerApplication
    fun bindContext(application: TemplateApp): Context

    // TODO

//    @Binds
//    @PerApplication
//    fun bindDatabase(db: AppDataBase): AppDataBase

//    @ContributesAndroidInjector
//    @PerService
//    fun logSendIntentService(): LogSendIntentService

    // для инжекторных методов не может быть абстрактных return-типов
}