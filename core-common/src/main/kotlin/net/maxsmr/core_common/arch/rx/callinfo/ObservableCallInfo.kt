package net.maxsmr.core_common.arch.rx.callinfo

import io.reactivex.Observable
import net.maxsmr.commonutils.rx.functions.ActionSafe
import net.maxsmr.commonutils.rx.functions.ConsumerSafe

class ObservableCallInfo<T>(
        val isTakeLastFrozen: Boolean,
        val observable: Observable<T>,
        val onNext: ConsumerSafe<T>,
        val onComplete: ActionSafe?,
        val onError: ConsumerSafe<Throwable>?,
        isForHandleError: Boolean
) : BaseCallInfo(isForHandleError)