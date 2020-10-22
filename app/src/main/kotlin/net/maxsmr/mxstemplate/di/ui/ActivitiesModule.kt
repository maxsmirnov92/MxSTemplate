package net.maxsmr.mxstemplate.di.ui

import dagger.Module
import dagger.android.ContributesAndroidInjector
import net.maxsmr.mxstemplate.di.PerActivity
import net.maxsmr.mxstemplate.ui.MainActivity

@Module
interface ActivitiesModule {

    @ContributesAndroidInjector(modules = [
        FragmentModules::class
    ])
    @PerActivity
    fun mainActivity(): MainActivity
}