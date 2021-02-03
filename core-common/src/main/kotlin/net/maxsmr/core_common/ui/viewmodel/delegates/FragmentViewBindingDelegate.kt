package net.maxsmr.core_common.ui.viewmodel.delegates

import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.viewbinding.ViewBinding
import net.maxsmr.core_common.BuildConfig
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

/**
 * Возвращает конкретный ViewBinding, определяемый возвращаемым типом [viewBindingFactory]
 *
 * @param viewBindingFactory фабрика для создания ViewBinding
 * @param rootViewProvider лямбда, возвращающая View корня байндинга (конкретного ViewBinding).
 * В большинстве случаев для корневого байндинга фрагмента задавать не нужно - по умолчанию используется
 * корневая View фрагмента, совпадающая с корнем байндинга.
 *
 * Имеет смысл задавать:
 * 1. когда в разметке используется include layout, а сам layout в корне содержит merge.
 * В этом случае лучше задать rootViewProvider, возвращающий контейнер, в который помещается merge тэг.
 * Это ускоряет байндинг, т.к. уменьшается вложенность иерархии для поиска вьюх.
 * 1. если имеется родитель с общим для всех детей layout, в одну из View которого добавляются различающиеся разметки "детей".
 * Например, родитель содержит ScrollView, а все его дети добавляют собственную разметку в этот скролл.
 * В этом случае корнем байндинга фрагмента является скролл, а корнем байндинга дочерних фрагментов - другие контейнеры.
 * Попытка забайндить без [rootViewProvider] приведет к крашу, вместо этого надо передавать { scroll.getChildAt(0) } в качестве корня
 */
fun <T : ViewBinding> Fragment.viewBinding(viewBindingFactory: (View) -> T, rootViewProvider: (() -> View)? = null) =
    FragmentViewBindingDelegate(this, rootViewProvider, viewBindingFactory)

/**
 * Делегат для предоставления ViewBinding во фрагменты.
 * Исключает возможные утечки памяти.
 */
class FragmentViewBindingDelegate<T : ViewBinding>(
    private val fragment: Fragment,
    private val rootViewProvider: (() -> View)?,
    private val viewBindingFactory: (View) -> T,
) : ReadOnlyProperty<Fragment, T>, DefaultLifecycleObserver {
    private var binding: T? = null

    init {
        fragment.lifecycle.addObserver(LifecycleObserver())
    }

    override fun getValue(thisRef: Fragment, property: KProperty<*>): T {
        val binding = binding
        if (binding != null) return binding

        if (BuildConfig.DEBUG) {
            //проверяем, что обращение ко View происходит в нужный момент (между onCreateView и onDestroyView),
            // иначе крашим в дебаг сборках, чтобы отловить неправильные кейсы
            val lifecycle = fragment.viewLifecycleOwner.lifecycle
            if (!lifecycle.currentState.isAtLeast(Lifecycle.State.INITIALIZED)) {
                throw IllegalStateException("Should not attempt to get bindings when Fragment views are destroyed.")
            }
        }

        return viewBindingFactory(rootViewProvider?.invoke()
            ?: thisRef.requireView()).also { this.binding = it }
    }


    private inner class LifecycleObserver : DefaultLifecycleObserver {

        override fun onCreate(owner: LifecycleOwner) {
            if (owner === fragment) {
                fragment.viewLifecycleOwnerLiveData.observe(fragment) { viewLifecycleOwner ->
                    viewLifecycleOwner.lifecycle.addObserver(this)
                }
            }
        }

        override fun onDestroy(owner: LifecycleOwner) {
            if (owner !== fragment) {
                binding = null
            }
        }
    }
}