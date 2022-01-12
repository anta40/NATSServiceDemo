package com.anta40.app.natsservicedemo

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log

class StartReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent!!.action == Intent.ACTION_BOOT_COMPLETED && getServiceState(context!!) == ServiceState.SERVICE_STARTED) {
            Intent(context, EndlessService::class.java).also {
                it.action = Actions.START_SERVICE.name
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    Log.d("ENDLESS_SERVICE","Starting the service in >=26 Mode from a BroadcastReceiver")
                    context.startForegroundService(it)
                    return
                }
                Log.d("ENDLESS_SERVICE","Starting the service in < 26 Mode from a BroadcastReceiver")
                context.startService(it)
            }
        }
    }
}