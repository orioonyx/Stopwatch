package com.kyungeun.timer

import android.annotation.SuppressLint
import android.app.*
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import java.util.*

class TimerService : Service() {
    companion object {
        // Channel ID for notifications
        const val CHANNEL_ID = "Timer_Notifications"

        // Service Actions
        const val START = "START"
        const val PAUSE = "PAUSE"
        const val RESET = "RESET"
        const val GET_STATUS = "GET_STATUS"
        const val MOVE_TO_FOREGROUND = "MOVE_TO_FOREGROUND"
        const val MOVE_TO_BACKGROUND = "MOVE_TO_BACKGROUND"

        // Intent Extras
        const val TIMER_ACTION = "TIMER_ACTION"
        const val TIME_ELAPSED = "TIME_ELAPSED"
        const val IS_TIMER_RUNNING = "IS_TIMER_RUNNING"

        // Intent Actions
        const val TIMER_TICK = "TIMER_TICK"
        const val TIMER_STATUS = "TIMER_STATUS"
    }

    private var timeElapsed: Int = 0
    private var isTimerRunning = false

    private var updateTimer = Timer()
    private var timer = Timer()

    private lateinit var notificationManager: NotificationManager

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createChannel()
        getNotificationManager()

        when (intent?.getStringExtra(TIMER_ACTION)!!) {
            START -> startTimer()
            PAUSE -> pauseTimer()
            RESET -> resetTimer()
            GET_STATUS -> sendStatus()
            MOVE_TO_FOREGROUND -> moveToForeground()
            MOVE_TO_BACKGROUND -> moveToBackground()
        }

        return START_STICKY
    }

    private fun moveToForeground() {
        startForeground(1, buildNotification())

        if (isTimerRunning) {
            updateTimer = Timer()
            updateTimer.scheduleAtFixedRate(object : TimerTask() {
                override fun run() {
                    updateNotification()
                }
            }, 0, 1000)
        }
    }

    private fun moveToBackground() {
        updateTimer.cancel()
        stopForeground(true)
    }

    private fun startTimer() {
        isTimerRunning = true

        sendStatus()

        timer = Timer()
        timer.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                val timerIntent = Intent()
                timerIntent.action = TIMER_TICK

                timeElapsed++

                timerIntent.putExtra(TIME_ELAPSED, timeElapsed)
                sendBroadcast(timerIntent)
            }
        }, 0, 1000)
    }

    private fun pauseTimer() {
        timer.cancel()
        isTimerRunning = false
        sendStatus()
    }

    private fun resetTimer() {
        pauseTimer()
        timeElapsed = 0
        sendStatus()
    }

    private fun sendStatus() {
        val statusIntent = Intent()
        statusIntent.action = TIMER_STATUS
        statusIntent.putExtra(IS_TIMER_RUNNING, isTimerRunning)
        statusIntent.putExtra(TIME_ELAPSED, timeElapsed)
        sendBroadcast(statusIntent)
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannel(
                CHANNEL_ID,
                "TIMER",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationChannel.setSound(null, null)
            notificationChannel.setShowBadge(true)
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(notificationChannel)
        }
    }

    private fun getNotificationManager() {
        notificationManager = ContextCompat.getSystemService(
            this,
            NotificationManager::class.java
        ) as NotificationManager
    }

    @SuppressLint("UnspecifiedImmutableFlag")
    private fun buildNotification(): Notification {
        val title = if (isTimerRunning) {
            "Timer is running!"
        } else {
            "Timer is paused!"
        }

        val hours = timeElapsed % 86400 / 3600
        val minutes = timeElapsed % 86400 % 3600 / 60
        val seconds = timeElapsed % 86400 % 3600 % 60
        val time = String.format("%02d:%02d:%02d", hours, minutes, seconds)

        val intent = Intent(this, MainActivity::class.java)
        val pIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_ONE_SHOT)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setOngoing(true)
            .setContentText(
                time
            )
            .setColorized(true)
            .setColor(Color.parseColor("#a8d4ff"))
            .setSmallIcon(R.drawable.ic_timer)
            .setOnlyAlertOnce(true)
            .setContentIntent(pIntent)
            .setAutoCancel(true)
            .build()
    }

    private fun updateNotification() {
        notificationManager.notify(
            1,
            buildNotification()
        )
    }

    override fun onDestroy() {
        timer.cancel()
        super.onDestroy()
    }
}