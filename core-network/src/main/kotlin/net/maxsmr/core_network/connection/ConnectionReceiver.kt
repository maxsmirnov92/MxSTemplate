package net.maxsmr.core_network.connection

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import net.maxsmr.networkutils.NetworkHelper.isOnline

/**
 * Receiver событий появления/исчезновния соединения
 */
class ConnectionReceiver(context: Context) : BroadcastReceiver() {

    private val connectionStateSubject = PublishSubject.create<Boolean>()

    var isConnected: Boolean = isOnline(context)
        private set

    override fun onReceive(context: Context, intent: Intent) {
        isConnected = isOnline(context)
        connectionStateSubject.onNext(isConnected)
    }

    fun observeConnectionChanges(): Observable<Boolean> =
        connectionStateSubject.hide()
}