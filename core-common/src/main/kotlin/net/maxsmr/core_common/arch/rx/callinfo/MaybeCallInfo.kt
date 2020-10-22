package net.maxsmr.core_common.arch.rx.callinfo

import io.reactivex.Maybe
import net.maxsmr.commonutils.rx.functions.ActionSafe
import net.maxsmr.commonutils.rx.functions.ConsumerSafe

class MaybeCallInfo<T>(
        val maybe: Maybe<T>,
        val onSuccess: ConsumerSafe<T>,
        val onComplete: ActionSafe,
        val onError: ConsumerSafe<Throwable>?,
        isForHandleError: Boolean
) : BaseCallInfo(isForHandleError)