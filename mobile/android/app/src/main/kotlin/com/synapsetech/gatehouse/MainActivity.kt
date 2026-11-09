package com.synapsetech.gatehouse

import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.os.Bundle
import io.flutter.embedding.android.FlutterActivity

class MainActivity : FlutterActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (
            BuildConfig.FLAVOR == "resident" &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
        ) {
            val channel = NotificationChannel(
                "resident_visitor_approvals",
                "Visitor approvals",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = "Visitor and delivery requests waiting for your decision."
            }
            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }
    }
}
