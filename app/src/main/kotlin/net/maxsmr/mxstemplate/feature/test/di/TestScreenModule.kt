package net.maxsmr.mxstemplate.feature.test.di

import dagger.Module
import dagger.android.ContributesAndroidInjector
import net.maxsmr.mxstemplate.di.PerFragment
import net.maxsmr.mxstemplate.feature.test.TestFragment

@Module
interface TestScreenModule {

    @PerFragment
    @ContributesAndroidInjector
    fun testFragment(): TestFragment
}