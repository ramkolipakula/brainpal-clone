package com.brainrot.rebuilt.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.brainrot.rebuilt.BlockOverlayActivity
import com.brainrot.rebuilt.BrainPalApp
import com.brainrot.rebuilt.MainActivity
import com.brainrot.rebuilt.R
import kotlinx.coroutines.*

class UsageMonitorService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var isMonitoring = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForegroundServiceNotification()
        if (!isMonitoring) {
            startMonitoringLoop()
        }
        return START_STICKY
    }

    private fun startForegroundServiceNotification() {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification: Notification = NotificationCompat.Builder(this, BrainPalApp.CHANNEL_ID)
            .setContentTitle("BrainPal Active")
            .setContentText("Focus protection is guarding your screen time")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        startForeground(101, notification)
    }

    private fun startMonitoringLoop() {
        isMonitoring = true
        serviceScope.launch {
            val app = BrainPalApp.instance
            while (isActive) {
                try {
                    // Check and perform midnight reset if a new calendar day has started
                    app.prefsRepository.checkAndPerformMidnightReset()

                    val foregroundPkg = app.usageRepository.getForegroundApp()
                    if (foregroundPkg != null && app.prefsRepository.isAppBlocked(foregroundPkg)) {
                        // Check if in active snooze
                        if (!app.prefsRepository.isSnoozed()) {
                            val blocked = app.prefsRepository.getBlockedPackages()
                            val stats = app.usageRepository.getTodayUsageStats(blocked, app.prefsRepository)
                            val currentAppStat = stats.find { it.packageName == foregroundPkg }
                            val minutesSpent = ((currentAppStat?.usageTimeMillis ?: 0L) / 60000L).toInt()
                            val limit = app.prefsRepository.getAppDailyLimitMinutes(foregroundPkg)
                            val isBedtime = app.prefsRepository.isWithinBedtime()

                            // DEF-02 FIX: Only trigger block when quota is actually reached or bedtime schedule is active.
                            // Strict Mode disallows bypasses inside BlockOverlayActivity once blocked.
                            if (minutesSpent >= limit || isBedtime) {
                                launchOverlay(foregroundPkg)
                                delay(4000L) // prevent spamming while overlay opens
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                delay(1500L)
            }
        }
    }

    private fun launchOverlay(packageName: String) {
        val intent = Intent(this, BlockOverlayActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra("TARGET_PACKAGE", packageName)
            putExtra("IS_STRICT_MODE", BrainPalApp.instance.prefsRepository.isStrictMode())
        }
        startActivity(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        isMonitoring = false
        serviceScope.cancel()
    }
}
