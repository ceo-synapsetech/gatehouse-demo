package com.synapsetech.gatehouse_alert_bridge

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.IBinder

class ResidentAlertService : Service() {
    private var player: MediaPlayer? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action != ACTION_START) {
            stopSelf(startId)
            return START_NOT_STICKY
        }

        val requestId = intent.getIntExtra(EXTRA_REQUEST_ID, 0)
        val title = intent.getStringExtra(EXTRA_TITLE) ?: "Visitor approval needed"
        val body = intent.getStringExtra(EXTRA_BODY) ?: "A visitor request is waiting for your decision."

        createServiceChannel()
        startForeground(SERVICE_NOTIFICATION_ID, buildServiceNotification(title, body, requestId))
        startLoopingTone()
        return START_NOT_STICKY
    }

    private fun createServiceChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            SERVICE_CHANNEL_ID,
            "Active visitor alert",
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "Keeps the visitor alert sound active while a request is pending."
            setSound(null, null)
            enableVibration(false)
            setShowBadge(false)
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun buildServiceNotification(title: String, body: String, requestId: Int): Notification {
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, SERVICE_CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }
        return builder
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Visitor alert is ringing")
            .setContentText("Open Gatehouse to review: $title")
            .setStyle(Notification.BigTextStyle().bigText(body))
            .setCategory(Notification.CATEGORY_SERVICE)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()
    }

    private fun startLoopingTone() {
        stopTone()
        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            ?: return
        try {
            player = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build(),
                )
                setDataSource(this@ResidentAlertService, soundUri)
                isLooping = true
                prepare()
                start()
            }
        } catch (_: Exception) {
            stopTone()
        }
    }

    private fun stopTone() {
        player?.run {
            try {
                if (isPlaying) stop()
            } catch (_: IllegalStateException) {
                // The player may have been interrupted by Android.
            }
            release()
        }
        player = null
    }

    override fun onDestroy() {
        stopTone()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_START = "com.synapsetech.gatehouse.alert.START"
        const val EXTRA_REQUEST_ID = "requestId"
        const val EXTRA_TITLE = "title"
        const val EXTRA_BODY = "body"
        private const val SERVICE_CHANNEL_ID = "gatehouse_active_alert_service"
        private const val SERVICE_NOTIFICATION_ID = 9101
    }
}
