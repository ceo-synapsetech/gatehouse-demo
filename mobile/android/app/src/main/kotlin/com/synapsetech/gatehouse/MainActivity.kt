package com.synapsetech.gatehouse

import android.app.NotificationChannel
import android.app.NotificationManager
import android.media.AudioAttributes
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import io.flutter.embedding.android.FlutterActivity

class MainActivity : FlutterActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (
            !packageName.endsWith(".security") &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
        ) {
            val channel = NotificationChannel(
                "resident_visitor_alerts_v2",
                "Urgent visitor approvals",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = "Urgent visitor requests needing your decision. Plays a gentle alert tone."
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 200, 150, 200)
                setSound(
                    Settings.System.DEFAULT_NOTIFICATION_URI,
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build(),
                )
            }
            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }
    }
}
