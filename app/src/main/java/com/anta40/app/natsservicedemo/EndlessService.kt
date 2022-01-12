package com.anta40.app.natsservicedemo

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.SystemClock
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import io.nats.client.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.*


class EndlessService: Service() {

    private var wakeLock: PowerManager.WakeLock? = null
    private var isServiceStarted = false

    override fun onBind(p0: Intent?): IBinder? {
        Log.d("ENDLESS_SERVICE","Some component want to bind with the service")
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("ENDLESS_SERVICE","onStartCommand executed with startId: $startId")
        if (intent != null) {
            val action = intent.action
            Log.d("ENDLESS_SERVICE","using an intent with action $action")
            when (action) {
                Actions.START_SERVICE.name -> startService()
                Actions.STOP_SERVICE.name -> stopService()
                else -> Log.d("ENDLESS_SERVICE","This should never happen. No action in the received intent")
            }
        } else {
            Log.d("ENDLESS_SERVICE",
                "with a null intent. It has been probably restarted by the system."
            )
        }
        // by returning this we make sure the service is restarted if the system kills the service
        return START_STICKY
    }

    override fun onCreate() {
        super.onCreate()
        Log.d("ENDLESS_SERVICE","The service has been created".toUpperCase())
        val notification = createNotification("hello world")
        startForeground(1, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("ENDLESS_SERVICE","The service has been destroyed".toUpperCase())
        Toast.makeText(this, "Service destroyed", Toast.LENGTH_SHORT).show()
    }

    override fun onTaskRemoved(rootIntent: Intent) {
        val restartServiceIntent = Intent(applicationContext, EndlessService::class.java).also {
            it.setPackage(packageName)
        };
        val restartServicePendingIntent: PendingIntent = PendingIntent.getService(this, 1, restartServiceIntent, PendingIntent.FLAG_ONE_SHOT);
        applicationContext.getSystemService(Context.ALARM_SERVICE);
        val alarmService: AlarmManager = applicationContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager;
        alarmService.set(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime() + 1000, restartServicePendingIntent);
    }


    private fun startService() {
        if (isServiceStarted) return
        Log.d("ENDLESS_SERVICE","Starting the foreground service task")
        Toast.makeText(this, "Service starting its task", Toast.LENGTH_SHORT).show()
        isServiceStarted = true
        setServiceState(this, ServiceState.SERVICE_STARTED)

        // we need this lock so our service gets not affected by Doze Mode
        wakeLock =
            (getSystemService(Context.POWER_SERVICE) as PowerManager).run {
                newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "EndlessService::lock").apply {
                    acquire()
                }
            }

        // we're starting a loop in a coroutine
        GlobalScope.launch(Dispatchers.IO) {
            while (isServiceStarted) {
                launch(Dispatchers.IO) {
                    handleNATS()
                }
                delay(1 * 60 * 1000)
            }
            Log.d("ENDLESS_SERVICE","End of the loop for the service")
        }
    }

    private fun stopService() {
        Log.d("ENDLESS_SERVICE", "Stopping the foreground service")
        Toast.makeText(this, "Service stopping", Toast.LENGTH_SHORT).show()
        try {
            wakeLock?.let {
                if (it.isHeld) {
                    it.release()
                }
            }
            stopForeground(true)
            stopSelf()
        } catch (e: Exception) {
            Log.d("ENDLESS_SERVICE","Service stopped without being started: ${e.message}")
        }
        isServiceStarted = false
        setServiceState(this, ServiceState.SERVICE_STOPPED)
    }

    private fun handleNATS(){

        val oBuilder =  Options.Builder()
        oBuilder.server(Constants.NATS.BASE_URL).
            reconnectWait(Duration.ofMinutes(3)).maxReconnects(-1)

        oBuilder.connectionListener(ConnectionListener { conn, type ->
            if (type == ConnectionListener.Events.DISCONNECTED || type == ConnectionListener.Events.CLOSED) {
                Log.d("NATS", "nats server connection $type")
            } else {
                Log.d("NATS", "nats server connection $type")
            }
        })

        val opts: Options = oBuilder.build()
        val nc = Nats.connect(opts)
        val sub = nc.subscribe("req_auth");

        Log.d("NATS","Subscribing to "+sub.getSubject());
        val msg = sub.nextMessage(Duration.ZERO);
        val response = String(msg.data, StandardCharsets.UTF_8)
        Log.d("NATS","Got request "+response+" from "+msg.getReplyTo());

        val notification = createNotification(response)
        startForeground(132435, notification)

    }

    private fun createNotification(notifMsg: String): Notification {
        val notificationChannelId = "ENDLESS SERVICE CHANNEL"

        // depending on the Android API that we're dealing with we will have
        // to use a specific method to create the notification
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                notificationChannelId,
                "Endless Service notifications channel",
                NotificationManager.IMPORTANCE_HIGH
            ).let {
                it.description = "Endless Service channel"
                it.enableLights(true)
                it.lightColor = Color.RED
                it.enableVibration(true)
                it.vibrationPattern = longArrayOf(100, 200, 300, 400, 500, 400, 300, 200, 400)
                it
            }
            notificationManager.createNotificationChannel(channel)
        }

        val pendingIntent: PendingIntent = Intent(this, MainActivity::class.java).let { notificationIntent ->
            PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_MUTABLE)
        }

        val builder: Notification.Builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) Notification.Builder(
            this,
            notificationChannelId
        ) else Notification.Builder(this)

        return builder
            .setContentTitle("NATS Demo")
            .setContentText(notifMsg)
            .setContentIntent(pendingIntent)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setTicker("Ticker text")
            .setPriority(Notification.PRIORITY_HIGH) // for under android 26 compatibility
            .build()
    }
}