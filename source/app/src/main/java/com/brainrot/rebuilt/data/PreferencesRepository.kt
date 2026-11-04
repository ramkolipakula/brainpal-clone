package com.brainrot.rebuilt.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class PreferencesRepository(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("brainpal_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_BLOCKED_PACKAGES = "blocked_packages"
        private const val KEY_DAILY_LIMIT_MINS = "daily_limit_mins"
        private const val KEY_DAILY_REEL_LIMIT = "daily_reel_limit"
        private const val KEY_ROAST_LANG = "roast_lang"
        private const val KEY_ONBOARDING_DONE = "onboarding_done"
        private const val KEY_SNOOZE_UNTIL = "snooze_until"
        private const val KEY_STREAK_DAYS = "streak_days"
        private const val KEY_STRICT_MODE = "strict_mode"
        private const val KEY_PASSES_LEFT = "emergency_passes_left"
        private const val KEY_LAST_ACTIVE_DATE = "last_active_date"
        private const val KEY_BUBBLE_ENABLED = "floating_bubble_enabled"
        private const val KEY_BUBBLE_SIZE = "floating_bubble_size" // "compact" or "large"
        private const val KEY_BEDTIME_ENABLED = "bedtime_enabled"
        private const val KEY_BEDTIME_START_HOUR = "bedtime_start_hour" // 22 (10 PM)
        private const val KEY_BEDTIME_END_HOUR = "bedtime_end_hour" // 6 (6 AM)
        private const val KEY_APP_LIMIT_PREFIX = "app_limit_mins_"
        private const val KEY_APP_REEL_LIMIT_PREFIX = "app_reel_limit_"
        private const val KEY_REEL_COUNT_PREFIX = "reels_today_"
        private const val KEY_INTERVENTIONS_PREFIX = "interventions_today_"

        private val DEFAULT_BLOCKED_PACKAGES = setOf(
            "com.instagram.android",
            "com.google.android.youtube",
            "com.snapchat.android",
            "com.zhiliaoapp.musically"
        )
    }

    private fun getTodayDateKey(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    }

    // ----------------------------------------------------
    // Reactive StateFlows (Phase 3: Live State Synchronization)
    // ----------------------------------------------------
    private val _todayReelCountFlow = MutableStateFlow(getTodayReelCount())
    val todayReelCountFlow: StateFlow<Int> = _todayReelCountFlow.asStateFlow()

    private val _dailyReelLimitFlow = MutableStateFlow(getDailyReelLimit())
    val dailyReelLimitFlow: StateFlow<Int> = _dailyReelLimitFlow.asStateFlow()

    private val _dailyLimitMinutesFlow = MutableStateFlow(getDailyLimitMinutes())
    val dailyLimitMinutesFlow: StateFlow<Int> = _dailyLimitMinutesFlow.asStateFlow()

    private val _floatingBubbleEnabledFlow = MutableStateFlow(isFloatingBubbleEnabled())
    val floatingBubbleEnabledFlow: StateFlow<Boolean> = _floatingBubbleEnabledFlow.asStateFlow()

    private val _bubbleSizeFlow = MutableStateFlow(getBubbleSize())
    val bubbleSizeFlow: StateFlow<String> = _bubbleSizeFlow.asStateFlow()

    private val _strictModeFlow = MutableStateFlow(isStrictMode())
    val strictModeFlow: StateFlow<Boolean> = _strictModeFlow.asStateFlow()

    private val _emergencyPassesFlow = MutableStateFlow(getEmergencyPasses())
    val emergencyPassesFlow: StateFlow<Int> = _emergencyPassesFlow.asStateFlow()

    private val _snoozeUntilFlow = MutableStateFlow(getSnoozeUntil())
    val snoozeUntilFlow: StateFlow<Long> = _snoozeUntilFlow.asStateFlow()

    private val _bedtimeEnabledFlow = MutableStateFlow(isBedtimeModeEnabled())
    val bedtimeEnabledFlow: StateFlow<Boolean> = _bedtimeEnabledFlow.asStateFlow()

    private val _bedtimeStartHourFlow = MutableStateFlow(getBedtimeStartHour())
    val bedtimeStartHourFlow: StateFlow<Int> = _bedtimeStartHourFlow.asStateFlow()

    private val _bedtimeEndHourFlow = MutableStateFlow(getBedtimeEndHour())
    val bedtimeEndHourFlow: StateFlow<Int> = _bedtimeEndHourFlow.asStateFlow()

    private val _blockedPackagesFlow = MutableStateFlow(getBlockedPackages())
    val blockedPackagesFlow: StateFlow<Set<String>> = _blockedPackagesFlow.asStateFlow()

    private val _streakDaysFlow = MutableStateFlow(getStreakDays())
    val streakDaysFlow: StateFlow<Int> = _streakDaysFlow.asStateFlow()

    private val _perAppMinuteLimitsFlow = MutableStateFlow<Map<String, Int>>(loadAllAppMinuteLimits())
    val perAppMinuteLimitsFlow: StateFlow<Map<String, Int>> = _perAppMinuteLimitsFlow.asStateFlow()

    private val _perAppReelLimitsFlow = MutableStateFlow<Map<String, Int>>(loadAllAppReelLimits())
    val perAppReelLimitsFlow: StateFlow<Map<String, Int>> = _perAppReelLimitsFlow.asStateFlow()

    private val _todayInterventionsFlow = MutableStateFlow(getTodayInterventionsCount())
    val todayInterventionsFlow: StateFlow<Int> = _todayInterventionsFlow.asStateFlow()

    // Automatic preference listener to keep StateFlows in lockstep even across processes/receivers
    private val prefChangeListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == null) return@OnSharedPreferenceChangeListener
        val todayKey = KEY_REEL_COUNT_PREFIX + getTodayDateKey()
        val todayInterventionsKey = KEY_INTERVENTIONS_PREFIX + getTodayDateKey()
        when {
            key == todayKey -> _todayReelCountFlow.value = getTodayReelCount()
            key == todayInterventionsKey -> _todayInterventionsFlow.value = getTodayInterventionsCount()
            key == KEY_DAILY_REEL_LIMIT -> _dailyReelLimitFlow.value = getDailyReelLimit()
            key == KEY_DAILY_LIMIT_MINS -> _dailyLimitMinutesFlow.value = getDailyLimitMinutes()
            key == KEY_BUBBLE_ENABLED -> _floatingBubbleEnabledFlow.value = isFloatingBubbleEnabled()
            key == KEY_BUBBLE_SIZE -> _bubbleSizeFlow.value = getBubbleSize()
            key == KEY_STRICT_MODE -> _strictModeFlow.value = isStrictMode()
            key == KEY_PASSES_LEFT -> _emergencyPassesFlow.value = getEmergencyPasses()
            key == KEY_SNOOZE_UNTIL -> _snoozeUntilFlow.value = getSnoozeUntil()
            key == KEY_BEDTIME_ENABLED -> _bedtimeEnabledFlow.value = isBedtimeModeEnabled()
            key == KEY_BEDTIME_START_HOUR -> _bedtimeStartHourFlow.value = getBedtimeStartHour()
            key == KEY_BEDTIME_END_HOUR -> _bedtimeEndHourFlow.value = getBedtimeEndHour()
            key == KEY_BLOCKED_PACKAGES -> _blockedPackagesFlow.value = getBlockedPackages()
            key == KEY_STREAK_DAYS -> _streakDaysFlow.value = getStreakDays()
            key.startsWith(KEY_APP_LIMIT_PREFIX) -> _perAppMinuteLimitsFlow.value = loadAllAppMinuteLimits()
            key.startsWith(KEY_APP_REEL_LIMIT_PREFIX) -> _perAppReelLimitsFlow.value = loadAllAppReelLimits()
        }
    }

    init {
        prefs.registerOnSharedPreferenceChangeListener(prefChangeListener)
    }

    private fun loadAllAppMinuteLimits(): Map<String, Int> {
        val map = mutableMapOf<String, Int>()
        for ((k, v) in prefs.all) {
            if (k.startsWith(KEY_APP_LIMIT_PREFIX) && v is Int) {
                map[k.removePrefix(KEY_APP_LIMIT_PREFIX)] = v
            }
        }
        return map
    }

    private fun loadAllAppReelLimits(): Map<String, Int> {
        val map = mutableMapOf<String, Int>()
        for ((k, v) in prefs.all) {
            if (k.startsWith(KEY_APP_REEL_LIMIT_PREFIX) && v is Int) {
                map[k.removePrefix(KEY_APP_REEL_LIMIT_PREFIX)] = v
            }
        }
        return map
    }

    fun getBlockedPackages(): Set<String> {
        return prefs.getStringSet(KEY_BLOCKED_PACKAGES, DEFAULT_BLOCKED_PACKAGES) ?: emptySet()
    }

    fun setBlockedPackages(packages: Set<String>) {
        prefs.edit().putStringSet(KEY_BLOCKED_PACKAGES, packages).apply()
        _blockedPackagesFlow.value = packages
    }

    fun isAppBlocked(packageName: String): Boolean {
        return getBlockedPackages().contains(packageName)
    }

    fun getDailyLimitMinutes(): Int {
        return prefs.getInt(KEY_DAILY_LIMIT_MINS, 30)
    }

    fun setDailyLimitMinutes(minutes: Int) {
        prefs.edit().putInt(KEY_DAILY_LIMIT_MINS, minutes).apply()
        _dailyLimitMinutesFlow.value = minutes
    }

    fun hasCustomAppLimitMinutes(packageName: String): Boolean {
        return prefs.contains(KEY_APP_LIMIT_PREFIX + packageName)
    }

    fun removeCustomAppLimitMinutes(packageName: String) {
        prefs.edit().remove(KEY_APP_LIMIT_PREFIX + packageName).apply()
        _perAppMinuteLimitsFlow.value = loadAllAppMinuteLimits()
    }

    fun getAppDailyLimitMinutes(packageName: String): Int {
        return prefs.getInt(KEY_APP_LIMIT_PREFIX + packageName, getDailyLimitMinutes())
    }

    fun setAppDailyLimitMinutes(packageName: String, minutes: Int) {
        prefs.edit().putInt(KEY_APP_LIMIT_PREFIX + packageName, minutes).apply()
        _perAppMinuteLimitsFlow.value = loadAllAppMinuteLimits()
    }

    fun getDailyReelLimit(): Int {
        return prefs.getInt(KEY_DAILY_REEL_LIMIT, 25)
    }

    fun setDailyReelLimit(limit: Int) {
        val safeLimit = kotlin.math.max(limit, 1)
        prefs.edit().putInt(KEY_DAILY_REEL_LIMIT, safeLimit).apply()
        _dailyReelLimitFlow.value = safeLimit
    }

    fun hasCustomAppReelLimit(packageName: String): Boolean {
        return prefs.contains(KEY_APP_REEL_LIMIT_PREFIX + packageName)
    }

    fun removeCustomAppReelLimit(packageName: String) {
        prefs.edit().remove(KEY_APP_REEL_LIMIT_PREFIX + packageName).apply()
        _perAppReelLimitsFlow.value = loadAllAppReelLimits()
    }

    fun getAppDailyReelLimit(packageName: String): Int {
        return prefs.getInt(KEY_APP_REEL_LIMIT_PREFIX + packageName, getDailyReelLimit())
    }

    fun setAppDailyReelLimit(packageName: String, limit: Int) {
        val safeLimit = kotlin.math.max(limit, 1)
        prefs.edit().putInt(KEY_APP_REEL_LIMIT_PREFIX + packageName, safeLimit).apply()
        _perAppReelLimitsFlow.value = loadAllAppReelLimits()
    }

    // Reel Counter Persistence (DEF-03)
    fun getTodayReelCount(): Int {
        val key = KEY_REEL_COUNT_PREFIX + getTodayDateKey()
        return prefs.getInt(key, 0)
    }

    fun incrementTodayReelCount(): Int {
        val key = KEY_REEL_COUNT_PREFIX + getTodayDateKey()
        val current = prefs.getInt(key, 0) + 1
        prefs.edit().putInt(key, current).apply()
        _todayReelCountFlow.value = current
        return current
    }

    fun setTodayReelCount(count: Int) {
        val key = KEY_REEL_COUNT_PREFIX + getTodayDateKey()
        val safeCount = kotlin.math.max(count, 0)
        prefs.edit().putInt(key, safeCount).apply()
        _todayReelCountFlow.value = safeCount
    }

    // Interventions Persistence (Tracking actual quota/bedtime interventions)
    fun getTodayInterventionsCount(): Int {
        val key = KEY_INTERVENTIONS_PREFIX + getTodayDateKey()
        return prefs.getInt(key, 0)
    }

    fun incrementTodayInterventionsCount(): Int {
        val key = KEY_INTERVENTIONS_PREFIX + getTodayDateKey()
        val current = prefs.getInt(key, 0) + 1
        prefs.edit().putInt(key, current).apply()
        _todayInterventionsFlow.value = current
        return current
    }

    fun resetTodayInterventions() {
        val key = KEY_INTERVENTIONS_PREFIX + getTodayDateKey()
        prefs.edit().putInt(key, 0).apply()
        _todayInterventionsFlow.value = 0
    }

    fun getRoastLanguage(): String {
        return prefs.getString(KEY_ROAST_LANG, "english") ?: "english"
    }

    fun setRoastLanguage(lang: String) {
        prefs.edit().putString(KEY_ROAST_LANG, lang).apply()
    }

    fun isOnboardingCompleted(): Boolean {
        return prefs.getBoolean(KEY_ONBOARDING_DONE, false)
    }

    fun setOnboardingCompleted(completed: Boolean) {
        prefs.edit().putBoolean(KEY_ONBOARDING_DONE, completed).apply()
    }

    fun getSnoozeUntil(): Long {
        return prefs.getLong(KEY_SNOOZE_UNTIL, 0L)
    }

    fun setSnoozeMinutes(minutes: Int) {
        val until = if (minutes <= 0) 0L else System.currentTimeMillis() + (minutes * 60 * 1000L)
        prefs.edit().putLong(KEY_SNOOZE_UNTIL, until).apply()
        _snoozeUntilFlow.value = until
    }

    fun isSnoozed(): Boolean {
        return System.currentTimeMillis() < getSnoozeUntil()
    }

    fun getStreakDays(): Int {
        return prefs.getInt(KEY_STREAK_DAYS, 1)
    }

    fun getStreakDisplayString(): String {
        val days = getStreakDays()
        return if (days == 1) "1 Day" else "$days Days"
    }

    fun incrementStreak() {
        val newStreak = getStreakDays() + 1
        prefs.edit().putInt(KEY_STREAK_DAYS, newStreak).apply()
        _streakDaysFlow.value = newStreak
    }

    fun resetStreak() {
        prefs.edit().putInt(KEY_STREAK_DAYS, 1).apply()
        _streakDaysFlow.value = 1
    }

    fun isStrictMode(): Boolean {
        return prefs.getBoolean(KEY_STRICT_MODE, false)
    }

    fun setStrictMode(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_STRICT_MODE, enabled).apply()
        _strictModeFlow.value = enabled
    }

    fun getEmergencyPasses(): Int {
        return prefs.getInt(KEY_PASSES_LEFT, 3)
    }

    fun useEmergencyPass(): Boolean {
        if (isStrictMode()) return false // Strict mode disallows emergency passes
        val current = getEmergencyPasses()
        if (current > 0) {
            val newPasses = current - 1
            prefs.edit().putInt(KEY_PASSES_LEFT, newPasses).apply()
            _emergencyPassesFlow.value = newPasses
            setSnoozeMinutes(5)
            return true
        }
        return false
    }

    fun resetDailyPasses() {
        prefs.edit().putInt(KEY_PASSES_LEFT, 3).apply()
        _emergencyPassesFlow.value = 3
    }

    // Floating Bubble Settings
    fun isFloatingBubbleEnabled(): Boolean {
        return prefs.getBoolean(KEY_BUBBLE_ENABLED, true)
    }

    fun setFloatingBubbleEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_BUBBLE_ENABLED, enabled).apply()
        _floatingBubbleEnabledFlow.value = enabled
    }

    fun getBubbleSize(): String {
        return prefs.getString(KEY_BUBBLE_SIZE, "compact") ?: "compact"
    }

    fun setBubbleSize(size: String) {
        val safeSize = if (size == "large") "large" else "compact"
        prefs.edit().putString(KEY_BUBBLE_SIZE, safeSize).apply()
        _bubbleSizeFlow.value = safeSize
    }

    // Bedtime & Schedule
    fun isBedtimeModeEnabled(): Boolean {
        return prefs.getBoolean(KEY_BEDTIME_ENABLED, false)
    }

    fun setBedtimeModeEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_BEDTIME_ENABLED, enabled).apply()
        _bedtimeEnabledFlow.value = enabled
    }

    fun getBedtimeStartHour(): Int {
        return prefs.getInt(KEY_BEDTIME_START_HOUR, 22)
    }

    fun setBedtimeStartHour(hour: Int) {
        prefs.edit().putInt(KEY_BEDTIME_START_HOUR, hour).apply()
        _bedtimeStartHourFlow.value = hour
    }

    fun getBedtimeEndHour(): Int {
        return prefs.getInt(KEY_BEDTIME_END_HOUR, 6)
    }

    fun setBedtimeEndHour(hour: Int) {
        prefs.edit().putInt(KEY_BEDTIME_END_HOUR, hour).apply()
        _bedtimeEndHourFlow.value = hour
    }

    fun isWithinBedtime(): Boolean {
        if (!isBedtimeModeEnabled()) return false
        val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val start = getBedtimeStartHour()
        val end = getBedtimeEndHour()
        return if (start > end) {
            // e.g. 22:00 to 06:00
            currentHour >= start || currentHour < end
        } else {
            currentHour in start until end
        }
    }

    // Midnight Reset Handler (DEF-10)
    fun checkAndPerformMidnightReset() {
        val today = getTodayDateKey()
        val lastDate = prefs.getString(KEY_LAST_ACTIVE_DATE, null)
        if (lastDate != null && lastDate != today) {
            // New day detected
            resetDailyPasses()
            incrementStreak()
            // Reset today's flow to today's count (0)
            _todayReelCountFlow.value = getTodayReelCount()
            _todayInterventionsFlow.value = getTodayInterventionsCount()
        }
        prefs.edit().putString(KEY_LAST_ACTIVE_DATE, today).apply()
    }
}
