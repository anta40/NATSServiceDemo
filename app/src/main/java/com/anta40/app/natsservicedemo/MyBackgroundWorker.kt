package com.anta40.app.natsservicedemo

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import io.nats.client.ConnectionListener
import io.nats.client.Duration
import io.nats.client.Nats
import io.nats.client.Options
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.nio.charset.StandardCharsets

class MyBackgroundWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {

    private val notificationManager =
        ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager


    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        //setForeground(createForegroundInfo())
        return@withContext runCatching {
            setForeground(createForegroundInfo())
            Result.success()
        }.getOrElse {
            Result.failure()
        }
    }


    private suspend fun handleNATS(){
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

        val notification = createForegroundInfo()
        setForeground(notification)

    }

    private fun createForegroundInfo(): ForegroundInfo {
        val id = "132435"
        val channelName = "NATS Notification"
        val title = "Message from NATS"
        val cancel = "Cancel"

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


        val intent = WorkManager.getInstance(applicationContext)
            .createCancelPendingIntent(getId())

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createChannel(id, channelName)
        }

        val notification = NotificationCompat.Builder(applicationContext, id)
            .setContentTitle(title)
            .setTicker(title)
            .setContentText(response)
            .setSmallIcon(R.drawable.ic_notification)
            .setOngoing(true)
            .addAction(android.R.drawable.ic_delete, cancel, intent)
            .build()

        return ForegroundInfo(1357, notification)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createChannel(id: String, channelName: String) {
        notificationManager.createNotificationChannel(
            NotificationChannel(id, channelName, NotificationManager.IMPORTANCE_DEFAULT)
        )
    }

}