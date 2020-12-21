package net.maxsmr.core_common.ui.viewmodel.delegates

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import net.maxsmr.core_common.ui.viewmodel.BaseViewModel
import kotlin.properties.ReadOnlyProperty
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

fun <VM: ViewModel> VM.getPersistableKey(kProperty: KProperty<*>): String =
    "${this::class.simpleName}.${kProperty.name}"

fun <T> BaseViewModel<*>.persistableLiveData(): PersistableLiveData<T> = PersistableLiveData(state)

fun <T> BaseViewModel<*>.persistableLiveDataInitial(initialValue: T?): PersistableLiveDataInitial<T> =
    PersistableLiveDataInitial(state, initialValue)

fun <T> BaseViewModel<*>.persistableValue(onSetValue: ((T?) -> Unit)? = null): PersistableValue<T> =
    PersistableValue(state, onSetValue)

fun <T> BaseViewModel<*>.persistableValueInitial(
    initialValue: T,
    onSetValue: ((T) -> Unit)? = null,
): PersistableValueInitial<T> = PersistableValueInitial(state, initialValue, onSetValue)


/**
 * Используйте, если нужна LiveData, переживающая смерть процесса приложения
 */
class PersistableLiveData<T>(
    private val state: SavedStateHandle
) : ReadOnlyProperty<ViewModel, MutableLiveData<T>> {

    override fun getValue(thisRef: ViewModel, property: KProperty<*>): MutableLiveData<T> {
        return state.getLiveData(thisRef.getPersistableKey(property))
    }
}


/**
 * Используйте, если нужна LiveData, переживающая смерть процесса приложения, с начальным значением.
 * Начальное значение гарантирует, что LiveData всегда содержит какое-либо значение и field.value
 * не вернет null.
 */
class PersistableLiveDataInitial<T>(
    private val state: SavedStateHandle,
    private val initialValue: T?,
) : ReadOnlyProperty<ViewModel, MutableLiveData<T>> {

    override fun getValue(thisRef: ViewModel, property: KProperty<*>): MutableLiveData<T> {
        return state.getLiveData(thisRef.getPersistableKey(property), initialValue)
    }
}


/**
 * Используйте, если нужно поле, переживающее смерть процесса приложения
 *
 * @param onSetValue доп. действие при смене значения
 */
class PersistableValue<T>(
    private val state: SavedStateHandle,
    private val onSetValue: ((T?) -> Unit)?,
) : ReadWriteProperty<ViewModel, T?> {

    override fun getValue(thisRef: ViewModel, property: KProperty<*>): T? {
        return state.get(thisRef.getPersistableKey(property))
    }

    override fun setValue(thisRef: ViewModel, property: KProperty<*>, value: T?) {
        state.set(thisRef.getPersistableKey(property), value)
        onSetValue?.invoke(value)
    }
}


/**
 * Используйте, если нужно поле, переживающее смерть процесса приложения, с начальным значением.
 * Начальное значение гарантирует, что [getValue] не вернет null.
 *
 * @param initial начальное значение
 * @param onSetValue доп. действие при смене значения
 */
class PersistableValueInitial<T>(
    private val state: SavedStateHandle,
    private val initial: T,
    private val onSetValue: ((T) -> Unit)?,
) : ReadWriteProperty<ViewModel, T> {

    override fun getValue(thisRef: ViewModel, property: KProperty<*>): T {
        return state.get(thisRef.getPersistableKey(property)) ?: initial
    }

    override fun setValue(thisRef: ViewModel, property: KProperty<*>, value: T) {
        state.set(thisRef.getPersistableKey(property), value)
        onSetValue?.invoke(value)
    }
}