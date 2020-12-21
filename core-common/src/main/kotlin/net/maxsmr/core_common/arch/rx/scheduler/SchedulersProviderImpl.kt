package net.maxsmr.core_common.arch.rx.scheduler

import io.reactivex.Scheduler
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers

class SchedulersProviderImpl : SchedulersProvider {

    override val main: Scheduler = AndroidSchedulers.mainThread()

    override val worker: Scheduler = Schedulers.io()
}