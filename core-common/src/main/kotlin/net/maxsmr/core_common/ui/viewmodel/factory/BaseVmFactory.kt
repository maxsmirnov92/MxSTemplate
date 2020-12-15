package net.maxsmr.core_common.ui.viewmodel.factory

import androidx.lifecycle.SavedStateHandle
import me.ilich.juggler.states.State
import net.maxsmr.core_common.ui.viewmodel.BaseViewModel

/**
 * Базовая фабрика для создания VM от [BaseViewModel].
 *
 * Инжектится фабрика модели, а не сама модель;
 * При создании VM может понадобиться не только зависимости из модулей, но и Params, SavedStateHandle и прочее
 * ->  в каждую такую фабрику дописываем @Inject конструктор;
 * Если фабрику можно создать без сторонних зависимостей, то @Inject ни в конструкторе, не в филде lateinit var viewModelFactory не нужен
 * (на самом деле будет нужен всегда, см. [BaseViewModel])
 */
interface BaseVmFactory<out VM: BaseViewModel<*>>  {

    /**
     * Метод создания VM.
     *
     * Параметр [handle] +
     * параметры фрагмента +
     * параметры конструктора конкретной реализации [BaseVmFactory] (внедряемые даггером)
     * = все необходимые для создания [VM] зависимости
     */
    fun create(handle: SavedStateHandle, params: State.Params): VM
}