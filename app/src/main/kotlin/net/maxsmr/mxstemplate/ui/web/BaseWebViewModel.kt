package net.maxsmr.mxstemplate.ui.web

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import net.maxsmr.commonutils.text.EMPTY_STRING
import net.maxsmr.core_common.arch.ErrorHandler
import net.maxsmr.core_common.arch.StringsProvider
import net.maxsmr.core_common.arch.rx.scheduler.SchedulersProvider
import net.maxsmr.core_common.ui.viewmodel.BaseScreenData
import net.maxsmr.core_common.ui.viewmodel.BaseViewModel

open class BaseWebViewModel<SD : BaseScreenData>(
    state: SavedStateHandle,
    schedulersProvider: SchedulersProvider,
    stringsProvider: StringsProvider,
    /**
     * Выставить в производном классе при необходимости
     * или оставить тот, что из модуля
     */
    errorHandler: ErrorHandler?
    ): BaseViewModel<SD>(state, schedulersProvider, stringsProvider, errorHandler) {

    val loadedWebViewUrl = MutableLiveData(EMPTY_STRING)
}