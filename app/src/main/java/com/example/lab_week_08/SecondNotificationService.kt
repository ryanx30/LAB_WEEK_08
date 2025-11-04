package com.example.lab_week_08

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.*

class SecondNotificationService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Default)
    private val channelId = "SECOND_NOTIFICATION_CHANNEL"
    private val notificationId = 2

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createNotificationChannel()

        // Build the initial notification
        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Second Notification Service Running")
            .setContentText("Processing...")
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        startForeground(notificationId, notification)

        // Run a countdown process asynchronously
        serviceScope.launch {
            for (i in 5 downTo 1) {
                delay(1000L)
                updateNotification("Finishing in $i seconds...")
            }

            // Stop after completion
            updateNotification("Second Service Done!")
            trackingCompletion.postValue("SECOND_SERVICE_DONE")
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }

        return START_NOT_STICKY
    }

    private fun updateNotification(message: String) {
        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("SecondNotificationService")
            .setContentText(message)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(notificationId, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Second Notification Channel",
                NotificationManager.IMPORTANCE_HIGH
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        val trackingCompletion = MutableLiveData<String>()
    }
}
