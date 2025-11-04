package com.example.lab_week_08

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

class NotificationService : Service() {

    // Notification builder to update notification while service runs
    private lateinit var notificationBuilder: NotificationCompat.Builder

    // Handler to run work on a background thread
    private lateinit var serviceHandler: Handler

    override fun onBind(intent: Intent): IBinder? = null

    override fun onCreate() {
        super.onCreate()

        // Prepare notification and start foreground
        notificationBuilder = createAndStartForegroundNotification()

        // Prepare handler thread so heavy work won't run on main thread
        val handlerThread = HandlerThread("NotificationServiceThread").apply { start() }
        serviceHandler = Handler(handlerThread.looper)
    }

    // Helper: prepare notification & call startForeground(...)
    private fun createAndStartForegroundNotification(): NotificationCompat.Builder {
        val pendingIntent = getPendingIntent()
        val channelId = createNotificationChannel()

        val builder = getNotificationBuilder(pendingIntent, channelId)
            .setOngoing(true)

        // Start as foreground service so the notification appears
        startForeground(NOTIFICATION_ID, builder.build())
        return builder
    }

    private fun getPendingIntent(): PendingIntent {
        val flag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            PendingIntent.FLAG_IMMUTABLE
        else 0

        val intent = Intent(this, MainActivity::class.java)
        return PendingIntent.getActivity(this, 0, intent, flag)
    }

    // Create notification channel for API >= 26
    private fun createNotificationChannel(): String {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = "001"
            val channelName = "001 Channel"
            val channelPriority = NotificationManager.IMPORTANCE_DEFAULT

            val channel = NotificationChannel(channelId, channelName, channelPriority)
            val manager = ContextCompat.getSystemService(this, NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
            return channelId
        }
        // For older devices channel ID can be empty or any string
        return ""
    }

    // Build a base notification builder
    private fun getNotificationBuilder(pendingIntent: PendingIntent, channelId: String) =
        NotificationCompat.Builder(this, channelId)
            .setContentTitle("Second worker process is running")
            .setContentText("Please wait...")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setTicker("Second worker process is running")

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val returnValue = super.onStartCommand(intent, flags, startId)

        val id = intent?.getStringExtra(EXTRA_ID)
            ?: throw IllegalStateException("Channel ID must be provided")

        // Post work to background handler
        serviceHandler.post {
            countDownFromTenToZero(notificationBuilder)
            notifyCompletion(id)
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }

        return returnValue
    }

    // Update notification every second counting down from 10 to 0
    private fun countDownFromTenToZero(builder: NotificationCompat.Builder) {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        for (i in 10 downTo 0) {
            try {
                Thread.sleep(1000L)
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
                break
            }
            builder.setContentText("$i seconds until last warning")
                .setSilent(true)
            notificationManager.notify(NOTIFICATION_ID, builder.build())
        }
    }

    // Post result back to main thread and update LiveData
    private fun notifyCompletion(id: String) {
        Handler(Looper.getMainLooper()).post {
            mutableID.value = id
        }
    }

    companion object {
        const val NOTIFICATION_ID = 0xCA7
        const val EXTRA_ID = "Id"

        private val mutableID = MutableLiveData<String>()
        val trackingCompletion: LiveData<String> get() = mutableID
    }
}
