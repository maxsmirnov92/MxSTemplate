package net.maxsmr.core_common.arch.rx.callinfo

import io.reactivex.Completable
import net.maxsmr.commonutils.rx.functions.ActionSafe
import net.maxsmr.commonutils.rx.functions.ConsumerSafe

class CompletableCallInfo(
        val completable: Completable,
        val onComplete: ActionSafe,
        val onError: ConsumerSafe<Throwable>?,
        isForHandleError: Boolean
): BaseCallInfo(isForHandleError)