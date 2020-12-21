package net.maxsmr.core_common.ui.viewmodel.delegates

import androidx.lifecycle.ViewModel
import net.maxsmr.core_common.ui.viewmodel.BaseVmFactory

/**
 * @param factory опциональная фабрика для создания VM
 *
 * @return параметры для создания **не**расшаренной VM
 */
inline fun <reified VM : ViewModel> vmFactoryParams(factory: BaseVmFactory<VM>? = null): VmFactoryParams<VM> {
    return VmFactoryParams(false, VM::class.java, factory)
}

/**
 * @param factory опциональная фабрика для создания VM
 *
 * @return параметры для создания расшаренной VM
 */
inline fun <reified VM : ViewModel> sharedVmFactoryParams(
        factory: BaseVmFactory<VM>? = null
): VmFactoryParams<VM> {
    return VmFactoryParams(true, VM::class.java, factory)
}

/**
 * Класс содержит параметры, необходимые фрагменту для получения инстанса ViewModel.
 * Используется, чтобы в каждом конкретном фрагменте нужно было определить только 1 поле, вместо нескольких.
 */
class VmFactoryParams<out VM : ViewModel> @JvmOverloads constructor(
    val isShared: Boolean,
    val clazz: Class<out VM>,
    val factory: BaseVmFactory<VM>? = null,
)