package com.anta40.app.natsservicedemo

import android.content.Intent
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager


class MainActivity : AppCompatActivity() {

    lateinit var btnStart: Button
    lateinit var btnStop: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btnStart = findViewById<Button>(R.id.btnStartService)
        btnStop = findViewById<Button>(R.id.btnStopService)

        btnStart.setOnClickListener {
            Log.d("ENDLESS_SERVICE","START THE FOREGROUND SERVICE ON DEMAND")
            actionOnService(Actions.START_SERVICE)

            WorkManager.getInstance(this)
                .beginUniqueWork("MyBackgroundWorker", ExistingWorkPolicy.APPEND_OR_REPLACE,
                    OneTimeWorkRequest.from(MyBackgroundWorker::class.java)).enqueue().state
                .observe(this) { state ->
                    Log.d("NATSDemo", "MyBackgroundWorker: $state")
                }

        }

        btnStop.setOnClickListener {
            Log.d("ENDLESS_SERVICE","START THE FOREGROUND SERVICE ON DEMAND")
            actionOnService(Actions.STOP_SERVICE)
        }
    }

    private fun actionOnService(action: Actions) {
        if (getServiceState(this) == ServiceState.SERVICE_STOPPED && action == Actions.STOP_SERVICE) return
        Intent(this, EndlessService::class.java).also {
            it.action = action.name
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Log.d("ENDLESS_SERVICE","Starting the service in >=26 Mode")
                startForegroundService(it)
                return
            }
            Log.d("ENDLESS_SERVICE", "Starting the service in < 26 Mode")
            startService(it)
        }
    }
}