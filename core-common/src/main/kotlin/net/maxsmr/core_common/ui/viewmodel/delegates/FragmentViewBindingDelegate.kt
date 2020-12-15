package net.maxsmr.core_common.ui.viewmodel.delegates

import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.viewbinding.ViewBinding
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

fun <T : ViewBinding> Fragment.viewBinding(viewBindingFactory: (View) -> T) =
        FragmentViewBindingDelegate(this, viewBindingFactory)

/**
 * Делегат для предоставления ViewBinding во фрагменты.
 * Исключает возможные утечки памяти.
 */
class FragmentViewBindingDelegate<T : ViewBinding>(
        val fragment: Fragment,
        val viewBindingFactory: (View) -> T
) : ReadOnlyProperty<Fragment, T>, DefaultLifecycleObserver {
    private var binding: T? = null

    init {
        fragment.lifecycle.addObserver(LifecycleObserver())
    }

    override fun getValue(thisRef: Fragment, property: KProperty<*>): T {
        val binding = binding
        if (binding != null) return binding

        val lifecycle = fragment.viewLifecycleOwner.lifecycle
        if (!lifecycle.currentState.isAtLeast(Lifecycle.State.INITIALIZED)) {
            throw IllegalStateException("Should not attempt to get bindings when Fragment views are destroyed.")
        }

        return viewBindingFactory(thisRef.requireView()).also { this.binding = it }
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