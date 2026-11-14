package com.cronorep

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator

/**
 * Runs during the countdown only. It holds a wake lock so the watch
 * cannot sleep through the end of the rest time, then it vibrates and stops.
 */
class RestTimerService : Service() {

    private val handler = Handler(Looper.getMainLooper())
    private var wakeLock: PowerManager.WakeLock? = null

    private val buzz = Runnable {
        vibrateRestOver()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private val warningTick = Runnable {
        vibrate(VibrationEffect.createOneShot(TICK_MS, TICK_AMPLITUDE))
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val delayMs = intent?.getLongExtra(EXTRA_DELAY_MS, -1L) ?: -1L
        if (delayMs < 0) {
            stopSelf()
            return START_NOT_STICKY
        }

        startInForeground()

        handler.removeCallbacksAndMessages(null)
        releaseWakeLock()
        wakeLock = getSystemService(PowerManager::class.java)
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "cronorep:rest")
            .apply { acquire(delayMs + 10_000L) }
        // Soft ticks in the last 3 seconds, then the strong vibration.
        for (secondsLeft in 1..WARNING_TICKS) {
            val tickAt = delayMs - secondsLeft * 1000L
            if (tickAt > 0) handler.postDelayed(warningTick, tickAt)
        }
        handler.postDelayed(buzz, delayMs)

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        releaseWakeLock()
        super.onDestroy()
    }

    private fun releaseWakeLock() {
        wakeLock?.let { if (it.isHeld) it.release() }
        wakeLock = null
    }

    private fun startInForeground() {
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Rest timer", NotificationManager.IMPORTANCE_LOW)
        )
        val openApp = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE
        )
        val notification = Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentTitle("Rest timer is running")
            .setContentIntent(openApp)
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun vibrateRestOver() {
        vibrate(VibrationEffect.createOneShot(END_MS, END_AMPLITUDE))
    }

    private fun vibrate(effect: VibrationEffect) {
        // Alarm usage lets the vibration play when the app is not on screen.
        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        @Suppress("DEPRECATION")
        getSystemService(Vibrator::class.java).vibrate(effect, attributes)
    }

    companion object {
        private const val CHANNEL_ID = "rest_timer"
        private const val NOTIFICATION_ID = 1
        private const val EXTRA_DELAY_MS = "delay_ms"
        private const val WARNING_TICKS = 3
        private const val TICK_MS = 30L
        private const val TICK_AMPLITUDE = 70 // of 255: soft
        private const val END_MS = 1200L
        private const val END_AMPLITUDE = 255 // maximum

        fun start(context: Context, delayMs: Long) {
            context.startForegroundService(
                Intent(context, RestTimerService::class.java).putExtra(EXTRA_DELAY_MS, delayMs)
            )
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, RestTimerService::class.java))
        }
    }
}
