package net.maxsmr.mxstemplate.feature.test

import androidx.lifecycle.SavedStateHandle
import net.maxsmr.core_common.arch.ErrorHandler
import net.maxsmr.core_common.arch.StringsProvider
import net.maxsmr.core_common.arch.rx.scheduler.SchedulersProvider
import net.maxsmr.core_common.ui.viewmodel.BaseViewModel
import net.maxsmr.core_common.ui.viewmodel.BaseScreenData

// TODO BaseHandleableViewModel

class TestViewModel constructor(
    savedState: SavedStateHandle,
    schedulersProvider: SchedulersProvider,
    stringsProvider: StringsProvider,
    errorHandler: ErrorHandler?
) : BaseViewModel<BaseScreenData>(savedState, schedulersProvider, stringsProvider, errorHandler)