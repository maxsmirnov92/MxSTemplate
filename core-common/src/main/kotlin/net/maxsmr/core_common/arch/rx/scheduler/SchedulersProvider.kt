package net.maxsmr.core_common.arch.rx.scheduler

import io.reactivex.Scheduler

interface SchedulersProvider {

    val main: Scheduler

    val worker: Scheduler
}
