package net.maxsmr.core_common.arch.rx.callinfo

import io.reactivex.Single
import net.maxsmr.commonutils.rx.functions.ConsumerSafe

class SingleCallInfo<T>(
        val single: Single<T>,
        val onSuccess: ConsumerSafe<T>,
        val onError: ConsumerSafe<Throwable>?,
        isForHandleError: Boolean
) : BaseCallInfo(isForHandleError)