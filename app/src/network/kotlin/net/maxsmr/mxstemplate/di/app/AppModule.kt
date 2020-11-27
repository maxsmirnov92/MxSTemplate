package net.maxsmr.mxstemplate.di.app

import android.content.Context
import dagger.Binds
import dagger.Module
import dagger.android.support.AndroidSupportInjectionModule
import net.maxsmr.core_common.arch.ErrorHandler
import net.maxsmr.mxstemplate.TemplateApp
import net.maxsmr.mxstemplate.api.handler.StandardErrorHandler
import net.maxsmr.mxstemplate.di.PerApplication

@Module(includes = [AndroidSupportInjectionModule::class])
interface AppModule {

    @Binds
    @PerApplication
    fun bindContext(application: TemplateApp): Context

    @Binds
    fun bindErrorHandler(handler: StandardErrorHandler): ErrorHandler

//    @Binds
//    @PerApplication
//    fun bindDatabase(db: AppDataBase): AppDataBase

    // для инжекторных методов не может быть абстрактных return-типов
}