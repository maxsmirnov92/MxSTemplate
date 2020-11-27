package net.maxsmr.mxstemplate.feature.test

import android.os.Bundle
import android.view.View
import androidx.lifecycle.SavedStateHandle
import me.ilich.juggler.states.State
import net.maxsmr.core_common.arch.StringsProvider
import net.maxsmr.core_common.arch.rx.scheduler.SchedulersProvider
import net.maxsmr.mxstemplate.di.ui.BaseVmFactory
import net.maxsmr.mxstemplate.ui.common.BaseFragment
import javax.inject.Inject

class TestFragment : BaseFragment<TestViewModel>() {

    override val viewModelClass: Class<TestViewModel> = TestViewModel::class.java

    @Inject
    override lateinit var viewModelFactory: Factory

    override val layoutId: Int = net.maxsmr.mxstemplate.R.layout.fragment_test

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
        viewModel: TestViewModel
    ) {
    }

    class Factory @Inject constructor(
        private val schedulersProvider: SchedulersProvider,
        private val stringsProvider: StringsProvider
    ) : BaseVmFactory<TestViewModel> {

        override fun create(handle: SavedStateHandle, params: State.Params): TestViewModel {
            return TestViewModel(
                handle,
                schedulersProvider,
                stringsProvider,
                null
            )
        }
    }

    companion object {

        fun instance() = TestFragment()
    }
}