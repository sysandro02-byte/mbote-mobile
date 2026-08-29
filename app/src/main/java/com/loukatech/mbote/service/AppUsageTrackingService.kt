package com.loukatech.mbote.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.loukatech.mbote.R
import com.loukatech.mbote.model.MboteNotification
import com.loukatech.mbote.model.NotificationType
import kotlinx.coroutines.*
import java.util.UUID

class AppUsageTrackingService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Default + Job())
    private var trackingJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "AppUsageTrackingService créé")
        startForegroundTrackingNotification()
        startTracking()
    }

    private fun startForegroundTrackingNotification() {
        val notificationManager = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager.createNotificationChannel(
                NotificationChannel(
                    TRACKING_CHANNEL_ID,
                    "Suivi du temps d’utilisation",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Mesure localement le temps passé dans MBoté"
                    setShowBadge(false)
                }
            )
        }
        val notification = NotificationCompat.Builder(this, TRACKING_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("MBoté")
            .setContentText("Suivi du temps d’utilisation actif")
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
        startForeground(TRACKING_NOTIFICATION_ID, notification)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            when (it.action) {
                ACTION_START -> {
                    Log.d(TAG, "Démarrage du suivi")
                    startTracking()
                }
                ACTION_STOP -> {
                    Log.d(TAG, "Arrêt du suivi")
                    stopTracking()
                    stopSelf()
                }
                ACTION_RECORD_SCROLL -> {
                    val count = it.getIntExtra(EXTRA_SCROLL_COUNT, 1)
                    incrementScrollTime(this, count)
                }
                ACTION_TRIGGER_WEEKLY_RECAP -> {
                    triggerWeeklyRecapNotification(this)
                }
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
        Log.d(TAG, "AppUsageTrackingService détruit")
    }

    private fun startTracking() {
        if (trackingJob?.isActive == true) return

        trackingJob = serviceScope.launch {
            while (isActive) {
                delay(5000) // Accumulate every 5 seconds
                incrementUsageTime(this@AppUsageTrackingService, 5)
                checkWeeklyRecapTrigger(this@AppUsageTrackingService)
            }
        }
    }

    private fun stopTracking() {
        trackingJob?.cancel()
        trackingJob = null
    }

    private fun checkWeeklyRecapTrigger(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lastReport = prefs.getLong(KEY_LAST_WEEKLY_REPORT, 0L)
        val now = System.currentTimeMillis()

        // 7 days in milliseconds: 7 * 24 * 60 * 60 * 1000 = 604800000
        val sevenDaysMs = 604800000L
        if (lastReport == 0L) {
            // Initialize first report timestamp for 7 days in the future
            prefs.edit().putLong(KEY_LAST_WEEKLY_REPORT, now).apply()
        } else if (now - lastReport >= sevenDaysMs) {
            triggerWeeklyRecapNotification(context)
        }
    }

    companion object {
        private const val TAG = "UsageTrackingService"
        private const val PREFS_NAME = "mbote_prefs"
        private const val TRACKING_CHANNEL_ID = "mbote_usage_tracking"
        private const val TRACKING_NOTIFICATION_ID = 1042

        const val ACTION_START = "com.loukatech.mbote.action.START_USAGE_TRACKING"
        const val ACTION_STOP = "com.loukatech.mbote.action.STOP_USAGE_TRACKING"
        const val ACTION_RECORD_SCROLL = "com.loukatech.mbote.action.RECORD_SCROLL"
        const val ACTION_TRIGGER_WEEKLY_RECAP = "com.loukatech.mbote.action.TRIGGER_WEEKLY_RECAP"

        const val EXTRA_SCROLL_COUNT = "extra_scroll_count"

        private const val KEY_TOTAL_USAGE_SECONDS = "key_total_usage_seconds"
        private const val KEY_TOTAL_SCROLL_SECONDS = "key_total_scroll_seconds"
        private const val KEY_LAST_WEEKLY_REPORT = "key_last_weekly_report"

        fun start(context: Context) {
            try {
                val intent = Intent(context, AppUsageTrackingService::class.java).apply {
                    action = ACTION_START
                }
                ContextCompat.startForegroundService(context, intent)
            } catch (e: Exception) {
                Log.e(TAG, "Erreur lors du démarrage du service", e)
            }
        }

        fun stop(context: Context) {
            try {
                val intent = Intent(context, AppUsageTrackingService::class.java).apply {
                    action = ACTION_STOP
                }
                context.startService(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Erreur lors de l'arrêt du service", e)
            }
        }

        fun recordScroll(context: Context) {
            incrementScrollTime(context, 1)
        }

        fun triggerRecap(context: Context) {
            triggerWeeklyRecapNotification(context)
        }

        fun getStats(context: Context): Pair<Long, Long> {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val usageSec = prefs.getLong(KEY_TOTAL_USAGE_SECONDS, 0L)
            val scrollSec = prefs.getLong(KEY_TOTAL_SCROLL_SECONDS, 0L)
            return Pair(usageSec, scrollSec)
        }

        private fun incrementUsageTime(context: Context, seconds: Long) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val current = prefs.getLong(KEY_TOTAL_USAGE_SECONDS, 0L)
            prefs.edit().putLong(KEY_TOTAL_USAGE_SECONDS, current + seconds).apply()
        }

        private fun incrementScrollTime(context: Context, seconds: Int) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val current = prefs.getLong(KEY_TOTAL_SCROLL_SECONDS, 0L)
            prefs.edit().putLong(KEY_TOTAL_SCROLL_SECONDS, current + seconds).apply()
        }

        private fun triggerWeeklyRecapNotification(context: Context) {
            val stats = getStats(context)
            val usageHours = stats.first / 3600
            val usageMinutes = (stats.first % 3600) / 60

            val scrollHours = stats.second / 3600
            val scrollMinutes = (stats.second % 3600) / 60

            val usageStr = if (usageHours > 0) "${usageHours}h ${usageMinutes}m" else "${usageMinutes}m"
            val scrollStr = if (scrollHours > 0) "${scrollHours}h ${scrollMinutes}m" else "${scrollMinutes}m"

            val notification = MboteNotification(
                id = UUID.randomUUID().toString(),
                type = NotificationType.SYSTEM,
                title = "📊 Bilan Hebdomadaire MBoté",
                body = "Vous avez passé au total $usageStr sur l'application cette semaine, dont $scrollStr à scroller sur vos vidéos préférées. Prenez soin de vos yeux ! 👁️✨",
                actionText = "Voir mes stats"
            )

            MboteNotificationManager.dispatchSystemNotification(context, notification)

            // Reset timers for the next week after reporting the measured values.
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit()
                .putLong(KEY_TOTAL_USAGE_SECONDS, 0L)
                .putLong(KEY_TOTAL_SCROLL_SECONDS, 0L)
                .putLong(KEY_LAST_WEEKLY_REPORT, System.currentTimeMillis())
                .apply()
        }
    }
}
