package net.maxsmr.core_network.downloader

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.annotation.MainThread
import androidx.core.app.NotificationCompat

class NotificationWrapper(val context: Context) {

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        ?: throw NullPointerException("NotificationManager is null")

    @MainThread
    fun showNotification(
        notificationId: Int,
        channelName: String,
        channelId: String = context.packageName,
        notificationConfigurator: (NotificationCompat.Builder) -> Unit
    ) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(notificationChannel)
        }

        val builder = NotificationCompat.Builder(context, channelId).apply {
            notificationConfigurator(this)
        }

        notificationManager.notify(notificationId, builder.build())
    }
}