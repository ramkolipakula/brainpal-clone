package com.brainrot.rebuilt.data

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.PackageManager
import com.brainrot.rebuilt.domain.model.AppUsageInfo
import java.util.Calendar

class UsageStatsRepository(private val context: Context) {

    private val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
    private val packageManager: PackageManager = context.packageManager

    fun hasUsageStatsPermission(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? android.app.AppOpsManager ?: return false
        val mode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                context.packageName
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                context.packageName
            )
        }
        return mode == android.app.AppOpsManager.MODE_ALLOWED
    }

    fun getTodayUsageStats(
        blockedPackages: Set<String>,
        prefsRepository: PreferencesRepository? = null
    ): List<AppUsageInfo> {
        val manager = usageStatsManager
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startTime = calendar.timeInMillis
        val endTime = System.currentTimeMillis()

        val statsMap = manager?.queryAndAggregateUsageStats(startTime, endTime) ?: emptyMap()
        val result = mutableListOf<AppUsageInfo>()

        // Ensure all blocked target apps are included in the results
        val allRelevantPackages = (statsMap.keys.filter { blockedPackages.contains(it) } + blockedPackages).toSet()

        for (pkg in allRelevantPackages) {
            val stats = statsMap[pkg]
            val totalTime = stats?.totalTimeInForeground ?: 0L
            val appName = getAppName(pkg)
            val limitMins = prefsRepository?.getAppDailyLimitMinutes(pkg) ?: 30
            result.add(
                AppUsageInfo(
                    packageName = pkg,
                    appName = appName,
                    usageTimeMillis = totalTime,
                    dailyLimitMinutes = limitMins,
                    isBlocked = blockedPackages.contains(pkg)
                )
            )
        }

        // If user has not added blocked apps yet, return standard social apps with 0 usage
        if (result.isEmpty()) {
            return getDefaultEmptyUsageStats(blockedPackages, prefsRepository)
        }

        return result.sortedByDescending { it.usageTimeMillis }
    }

    fun getForegroundApp(): String? {
        val manager = usageStatsManager ?: return null
        val endTime = System.currentTimeMillis()
        val startTime = endTime - 10_000L // last 10 seconds

        val events = manager.queryEvents(startTime, endTime)
        var lastPackage: String? = null
        val event = UsageEvents.Event()

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED) {
                lastPackage = event.packageName
            }
        }
        return lastPackage
    }

    // DEF-01 FIX: Only aggregate usage for tracked/distraction packages, NOT the entire OS
    fun getTotalTodayForegroundTimeMillis(blockedPackages: Set<String>): Long {
        if (blockedPackages.isEmpty()) return 0L
        val manager = usageStatsManager ?: return 0L

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startTime = calendar.timeInMillis
        val endTime = System.currentTimeMillis()

        val statsMap = manager.queryAndAggregateUsageStats(startTime, endTime)
        return statsMap.filterKeys { blockedPackages.contains(it) }
            .values
            .sumOf { it.totalTimeInForeground }
    }

    // DEF-04 FIX: Real hourly usage distribution calculated from UsageStatsManager
    fun getHourlyUsageToday(blockedPackages: Set<String>): List<Pair<String, Int>> {
        val hoursLabels = listOf(
            Pair("9 AM", 9),
            Pair("11 AM", 11),
            Pair("1 PM", 13),
            Pair("3 PM", 15),
            Pair("5 PM", 17),
            Pair("7 PM", 19),
            Pair("9 PM", 21),
            Pair("11 PM", 23)
        )

        val manager = usageStatsManager
        if (manager == null || blockedPackages.isEmpty() || !hasUsageStatsPermission()) {
            return hoursLabels.map { Pair(it.first, 0) }
        }

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startOfDay = calendar.timeInMillis
        val now = System.currentTimeMillis()

        val hourlyMinutesMap = mutableMapOf<Int, Long>()
        try {
            val events = manager.queryEvents(startOfDay, now)
            val event = UsageEvents.Event()
            var lastResumeTime = 0L
            var lastPkg: String? = null

            while (events.hasNextEvent()) {
                events.getNextEvent(event)
                val pkg = event.packageName
                val time = event.timeStamp
                val cal = Calendar.getInstance().apply { timeInMillis = time }
                val hour = cal.get(Calendar.HOUR_OF_DAY)

                if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED) {
                    if (blockedPackages.contains(pkg)) {
                        lastResumeTime = time
                        lastPkg = pkg
                    }
                } else if (event.eventType == UsageEvents.Event.ACTIVITY_PAUSED || event.eventType == UsageEvents.Event.ACTIVITY_STOPPED) {
                    if (lastPkg == pkg && lastResumeTime > 0) {
                        val duration = (time - lastResumeTime).coerceAtLeast(0L)
                        hourlyMinutesMap[hour] = (hourlyMinutesMap[hour] ?: 0L) + (duration / 60000L)
                        lastResumeTime = 0L
                        lastPkg = null
                    }
                }
            }

            // If an app is still in foreground right now, account for ongoing session
            if (lastPkg != null && lastResumeTime > 0) {
                val ongoingDuration = (now - lastResumeTime).coerceAtLeast(0L)
                val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
                hourlyMinutesMap[currentHour] = (hourlyMinutesMap[currentHour] ?: 0L) + (ongoingDuration / 60000L)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return hoursLabels.map { (label, targetHour) ->
            val minutes = ((hourlyMinutesMap[targetHour] ?: 0L) + (hourlyMinutesMap[targetHour - 1] ?: 0L)).toInt()
            Pair(label, minutes)
        }
    }

    fun getAppName(packageName: String): String {
        return try {
            val info = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(info).toString()
        } catch (e: Exception) {
            when (packageName) {
                "com.instagram.android" -> "Instagram"
                "com.google.android.youtube" -> "YouTube"
                "com.snapchat.android" -> "Snapchat"
                "com.zhiliaoapp.musically" -> "TikTok"
                "com.facebook.katana" -> "Facebook"
                "com.reddit.frontpage" -> "Reddit"
                "com.twitter.android" -> "X / Twitter"
                "com.pinterest" -> "Pinterest"
                else -> packageName.substringAfterLast('.').replaceFirstChar { it.uppercase() }
            }
        }
    }

    private fun getDefaultEmptyUsageStats(
        blockedPackages: Set<String>,
        prefsRepository: PreferencesRepository?
    ): List<AppUsageInfo> {
        val defaults = listOf(
            "com.instagram.android",
            "com.google.android.youtube",
            "com.snapchat.android",
            "com.zhiliaoapp.musically"
        )
        return defaults.map { pkg ->
            AppUsageInfo(
                packageName = pkg,
                appName = getAppName(pkg),
                usageTimeMillis = 0L,
                dailyLimitMinutes = prefsRepository?.getAppDailyLimitMinutes(pkg) ?: 30,
                isBlocked = blockedPackages.contains(pkg)
            )
        }
    }
}
