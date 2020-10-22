package net.maxsmr.core_network.connection

import android.content.Context
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.ConnectivityManager.CONNECTIVITY_ACTION
import android.net.NetworkInfo
import android.telephony.TelephonyManager
import io.reactivex.Observable
import java.util.concurrent.TimeUnit

private val LAST_CONNECTION_QUALITY_RESULT_CACHE_TIME = TimeUnit.MINUTES.toMillis(1)

/**
 * Provider, позволяющий подписаться на событие изменения состояния соединения
 */
class ConnectionProvider(private val context: Context) {

    private val receiver: ConnectionReceiver = ConnectionReceiver(context)

    private var lastConnectionResultFast = false
    private var lastConnectionResultTime: Long = 0

    val isConnected: Boolean
        get() = receiver.isConnected

    val isConnectedFast: Boolean
        get() {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastConnectionResultTime > LAST_CONNECTION_QUALITY_RESULT_CACHE_TIME) {
                lastConnectionResultTime = currentTime
                lastConnectionResultFast = isConnectedFastInternal(context)
            }
            return lastConnectionResultFast
        }

    /**
     * Проверка на подключение к Wi-Fi
     *
     * @return подключен ли девайс к Wi-Fi, или к мобильной сети
     */
    val isConnectedToWifi: Boolean
        get() {
            val info = getNetworkInfo(context)
            return info != null && info.isConnected && info.type == ConnectivityManager.TYPE_WIFI
        }

    init {
        with(IntentFilter()) {
            addAction(CONNECTIVITY_ACTION)
            context.registerReceiver(receiver, this)
        }
    }

    fun observeConnectionChanges(): Observable<Boolean> {
        return receiver.observeConnectionChanges()
    }

    /**
     * Get the network info
     *
     * @param context
     * @return
     */
    private fun getNetworkInfo(context: Context): NetworkInfo? {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        return cm.activeNetworkInfo
    }

    /**
     * Check if there is fast connectivity
     *
     * @param context
     * @return
     */
    private fun isConnectedFastInternal(context: Context): Boolean {
        val info = getNetworkInfo(context)
        return info != null && info.isConnected && isConnectionFastInternal(info.type, info.subtype)
    }

    /**
     * Check if the connection is fast
     *
     * @param type
     * @param subType
     * @return
     */
    private fun isConnectionFastInternal(type: Int, subType: Int): Boolean {
        return if (type == ConnectivityManager.TYPE_WIFI) {
            true
        } else if (type == ConnectivityManager.TYPE_MOBILE) {
            when (subType) {
                TelephonyManager.NETWORK_TYPE_EVDO_0 // ~ 400-1000 kbps
                    , TelephonyManager.NETWORK_TYPE_EVDO_A // ~ 600-1400 kbps
                    , TelephonyManager.NETWORK_TYPE_HSDPA // ~ 2-14 Mbps
                    , TelephonyManager.NETWORK_TYPE_HSPA // ~ 700-1700 kbps
                    , TelephonyManager.NETWORK_TYPE_HSUPA // ~ 1-23 Mbps
                    , TelephonyManager.NETWORK_TYPE_UMTS // ~ 400-7000 kbps
                    ,
                    /*
                     * Above API level 7, make sure to set android:targetSdkVersion
                     * to appropriate level to use these
                     */
                TelephonyManager.NETWORK_TYPE_EHRPD // ~ 1-2 Mbps // API level 11
                    , TelephonyManager.NETWORK_TYPE_EVDO_B // API level 9 // ~ 5 Mbps
                    , TelephonyManager.NETWORK_TYPE_HSPAP // API level 13 // ~ 10-20 Mbps
                    , TelephonyManager.NETWORK_TYPE_LTE // API level 11 // ~ 10+ Mbps
                -> true
                TelephonyManager.NETWORK_TYPE_1xRTT // ~ 50-100 kbps
                    , TelephonyManager.NETWORK_TYPE_CDMA // ~ 14-64 kbps
                    , TelephonyManager.NETWORK_TYPE_EDGE // ~ 50-100 kbps
                    , TelephonyManager.NETWORK_TYPE_GPRS // ~ 100 kbps
                    ,
                    /*
                     * Above API level 7, make sure to set android:targetSdkVersion
                     * to appropriate level to use these
                     */
                TelephonyManager.NETWORK_TYPE_IDEN // API level 8 // ~25 kbps
                    , TelephonyManager.NETWORK_TYPE_UNKNOWN // Unknown
                -> false
                else -> false
            }
        } else {
            false
        }
    }
}