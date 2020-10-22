package net.maxsmr.core_common.ui.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import kotlin.properties.ReadOnlyProperty
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/**
 * Используйте, если нужна LiveData, переживающая убийство процесса приложения
 */
class PersistableLiveData<T>(
        private val state: SavedStateHandle,
        private val key: String,
        private val initialValue: T? = null
) : ReadOnlyProperty<ViewModel, MutableLiveData<T>> {

    override fun getValue(thisRef: ViewModel, property: KProperty<*>): MutableLiveData<T> {
        return initialValue?.let {
            state.getLiveData(key, it)
        } ?: state.getLiveData(key) ?: MutableLiveData()
    }
}

/**
 * Используйте, если нужно поле, переживающее убийство процесса приложения
 */
class PersistableValue<T>(
        private val state: SavedStateHandle,
        private val key: String,
        private val initial: T? = null
) : ReadWriteProperty<ViewModel, T?> {

    override fun getValue(thisRef: ViewModel, property: KProperty<*>): T? {
        return state.get(key) ?: initial
    }

    override fun setValue(thisRef: ViewModel, property: KProperty<*>, value: T?) {
        state.set(key, value)
    }
}