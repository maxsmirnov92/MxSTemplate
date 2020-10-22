package net.maxsmr.mxstemplate.di

import javax.inject.Scope

@Scope
@Retention(AnnotationRetention.RUNTIME)
annotation class PerApplication

@Scope
@Retention(AnnotationRetention.RUNTIME)
annotation class PerActivity

@Scope
@Retention(AnnotationRetention.RUNTIME)
annotation class PerFragment

@Scope
@Retention(AnnotationRetention.RUNTIME)
annotation class PerService