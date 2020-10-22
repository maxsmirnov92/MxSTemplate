package net.maxsmr.mxstemplate.di.ui.factory

import androidx.lifecycle.ViewModel
import dagger.MapKey
import kotlin.reflect.KClass

@MapKey
@MustBeDocumented
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
@Retention(AnnotationRetention.RUNTIME)
@Deprecated("VM factories must be injected, not VMs")
annotation class ViewModelKey(val value: KClass<out ViewModel>)