package net.maxsmr.mxstemplate.di.ui

import dagger.Module
import net.maxsmr.mxstemplate.feature.test.di.TestScreenModule

@Module(includes = [
    TestScreenModule::class
])
interface FragmentModules {

    // остальные ContributesAndroidInjector, если не вошли в индивидуальный модуль
}