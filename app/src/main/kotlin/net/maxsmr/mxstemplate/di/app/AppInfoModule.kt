package net.maxsmr.mxstemplate.di.app

import android.content.Context
import dagger.Module
import dagger.Provides
import net.maxsmr.commonutils.getSelfVersionName
import net.maxsmr.mxstemplate.di.PerApplication
import javax.inject.Named

const val DI_NAME_VERSION_NAME = "version_name"

@Module
class AppInfoModule {

    @Provides
    @PerApplication
    @Named(DI_NAME_VERSION_NAME)
    fun provideVersionName(context: Context): String
            = getSelfVersionName(context)
}