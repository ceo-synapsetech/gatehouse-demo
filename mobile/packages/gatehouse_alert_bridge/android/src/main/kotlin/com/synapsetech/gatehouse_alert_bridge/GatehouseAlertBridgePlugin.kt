package com.synapsetech.gatehouse_alert_bridge

import android.content.Context
import android.content.Intent
import android.os.Build
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel

class GatehouseAlertBridgePlugin : FlutterPlugin, MethodChannel.MethodCallHandler {
    private lateinit var appContext: Context
    private lateinit var channel: MethodChannel

    override fun onAttachedToEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        appContext = binding.applicationContext
        channel = MethodChannel(binding.binaryMessenger, "com.synapsetech.gatehouse/visitor_alert")
        channel.setMethodCallHandler(this)
    }

    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
        when (call.method) {
            "start" -> {
                val requestId = call.argument<Int>("requestId")
                val title = call.argument<String>("title")
                val body = call.argument<String>("body")
                if (requestId == null || title == null || body == null) {
                    result.error("invalid_alert", "Missing visitor alert details.", null)
                    return
                }
                val intent = Intent(appContext, ResidentAlertService::class.java)
                    .setAction(ResidentAlertService.ACTION_START)
                    .putExtra(ResidentAlertService.EXTRA_REQUEST_ID, requestId)
                    .putExtra(ResidentAlertService.EXTRA_TITLE, title)
                    .putExtra(ResidentAlertService.EXTRA_BODY, body)
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        appContext.startForegroundService(intent)
                    } else {
                        appContext.startService(intent)
                    }
                    result.success(null)
                } catch (error: Exception) {
                    result.error("alert_start_failed", error.message, null)
                }
            }
            "stop" -> {
                appContext.stopService(Intent(appContext, ResidentAlertService::class.java))
                result.success(null)
            }
            else -> result.notImplemented()
        }
    }

    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        channel.setMethodCallHandler(null)
    }
}
