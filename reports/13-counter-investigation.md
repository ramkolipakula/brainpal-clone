# Counter Investigation Report: Screen Time & Reel Metrics

**Document**: `13-counter-investigation.md`  
**Date**: November 2, 2026  
**Auditor**: Autonomous Principal Android QA & Security Auditor  
**Scope**: In-Depth Code-Level Audit of UsageStats, Reel Counters, Time Aggregations, and UI Propagation  
**Status**: DEFECTS IDENTIFIED — ROOT CAUSES CONFIRMED

---

## 1. Executive Summary

A comprehensive, read-only code analysis of `UsageStatsRepository.kt`, `UsageMonitorService.kt`, `ReelsAccessibilityService.kt`, `HomeScreen.kt`, and `StatsScreen.kt` was conducted and compared against the original decompiled `BrainPal.apk` evidence.

The reconstructed application suffers from **7 major counter defects** that produce inaccurate screen-time totals, stale UI metrics, premature blocking, unpersisted reel counts, and fake analytics data.

---

## 2. Root Cause Analysis by Component

### 2.1. Total Screen Time Aggregates Entire OS Instead of Tracked Apps
- **Location**: `source/app/src/main/java/com/brainrot/rebuilt/data/UsageStatsRepository.kt#L91-L104`
- **Code**:
  ```kotlin
  fun getTotalTodayForegroundTimeMillis(): Long {
      val calendar = Calendar.getInstance().apply {
          set(Calendar.HOUR_OF_DAY, 0)
          set(Calendar.MINUTE, 0)
          set(Calendar.SECOND, 0)
          set(Calendar.MILLISECOND, 0)
      }
      val startTime = calendar.timeInMillis
      val endTime = System.currentTimeMillis()

      val manager = usageStatsManager ?: return 1_800_000L // 30 min fallback
      val statsMap = manager.queryAndAggregateUsageStats(startTime, endTime)
      return statsMap.values.sumOf { it.totalTimeInForeground }
  }
  ```
- **Defect Description**:
  The function computes `statsMap.values.sumOf { it.totalTimeInForeground }`. This iterates over **every single package on Android**, including `com.android.systemui`, `com.google.android.apps.nexuslauncher`, `com.google.android.inputmethod.latin` (Gboard), `com.google.android.gms`, phone dialer, WhatsApp, and utilities.
- **Impact**:
  A user who spent 45 minutes answering WhatsApp messages and checking email will see BrainPal report 45 minutes of "Distracted Screen Time" against their 30-minute quota. The Buddy Mascot on the home screen immediately transitions into `BuddyMood.ROASTED` ("Brain is Fried!"), even if Instagram and YouTube were opened for 0 seconds.
- **Original APK Parity**:
  Original BrainPal queries usage only for explicitly targeted entertainment/social packages tracked in `UserBlockingConfig` and `DailyReelsAppSplit`.

---

### 2.2. Hardcoded Daily Limit & Mock Fallback in Per-App Usage
- **Location**: `source/app/src/main/java/com/brainrot/rebuilt/data/UsageStatsRepository.kt#L34-L71` & `L123-L131`
- **Code**:
  ```kotlin
  fun getTodayUsageStats(blockedPackages: Set<String>): List<AppUsageInfo> {
      ...
      for ((pkg, stats) in statsMap) {
          val totalTime = stats.totalTimeInForeground
          if (totalTime > 0 || blockedPackages.contains(pkg)) {
              val appName = getAppName(pkg)
              result.add(
                  AppUsageInfo(
                      packageName = pkg,
                      appName = appName,
                      usageTimeMillis = totalTime,
                      dailyLimitMinutes = 30, // HARDCODED
                      isBlocked = blockedPackages.contains(pkg)
                  )
              )
          }
      }

      if (result.isEmpty()) {
          return getMockUsageStats(blockedPackages) // FAKE FALLBACK
      }
      return result.sortedByDescending { it.usageTimeMillis }
  }
  ```
- **Defect Description**:
  1. `dailyLimitMinutes = 30` is hardcoded on line 58. Even if the user adjusts the global slider to 60 minutes or sets custom quotas, every individual app model reports 30 minutes.
  2. If a clean device with 0 usage runs this query, or before usage stats permissions finish indexing, `result.isEmpty()` triggers `getMockUsageStats()`, which injects fabricated usage times (Instagram: 40m, YouTube: 30m, Snapchat: 15m, Reddit: 10m).
- **Impact**:
  Misleads users by displaying phantom usage when permissions are freshly granted or no usage has occurred today.

---

### 2.3. In-Memory Volatility of Reel Scroll Counter
- **Location**: `source/app/src/main/java/com/brainrot/rebuilt/service/ReelsAccessibilityService.kt#L35-L37`
- **Code**:
  ```kotlin
  companion object {
      private const val SCROLL_DEBOUNCE_MILLIS = 850L
      var isServiceConnected = false
          private set
      var reelScrollCountToday = 0
          private set
  }
  ```
- **Defect Description**:
  `reelScrollCountToday` is an in-memory JVM static variable inside the companion object. It is never written to `SharedPreferences`, `DataStore`, or a database.
- **Impact**:
  If the application process is trimmed by the OS (Android Low Memory Killer), if the accessibility service rebinds, or if the user switches apps and the service restarts, `reelScrollCountToday` is immediately reset to 0. A user could watch 24 reels (1 below limit), get backgrounded, and return to an active counter of 0, completely resetting their quota.
- **Original APK Parity**:
  Original BrainPal persists every reel swipe event to Room Database (`WeekEventDb` and `ReelsScrollEvent` table) with fields `appId`, `eventTimestamp`, and `viewDurationMillis`.

---

### 2.4. Lack of Lifecycle-Aware UI Counter Refresh
- **Location**: `source/app/src/main/java/com/brainrot/rebuilt/ui/screens/HomeScreen.kt#L42-L50`
- **Code**:
  ```kotlin
  LaunchedEffect(Unit) {
      val blocked = app.prefsRepository.getBlockedPackages()
      usageStats = app.usageRepository.getTodayUsageStats(blocked)
      val totalMillis = app.usageRepository.getTotalTodayForegroundTimeMillis()
      totalMinutesSpent = (totalMillis / 60000L).toInt()
      limitMinutes = app.prefsRepository.getDailyLimitMinutes()
      isSnoozed = app.prefsRepository.isSnoozed()
  }
  ```
- **Defect Description**:
  `LaunchedEffect(Unit)` only fires once when `HomeScreen` first attaches to the composition. When the user navigates out to Instagram, scrolls for 20 minutes, and presses the Home button or Task Switcher back into BrainPal:
  `HomeScreen` does NOT recompose or re-query `UsageStatsManager`.
- **Impact**:
  The screen time counter, progress bar, and mascot mood remain permanently stale until the app is killed or the user navigates between tabs.
- **Remediation Requirement**:
  Must bind to `LifecycleResumeEffect` or `LifecycleEventObserver(Lifecycle.Event.ON_RESUME)`.

---

### 2.5. Total Absence of Midnight Reset Architecture
- **Location**: `source/app/src/main/java/com/brainrot/rebuilt/service/BootReceiver.kt#L13-L23`
- **Defect Description**:
  In the rebuilt source, `app.prefsRepository.resetDailyPasses()` is invoked **only** inside `BootReceiver` (`ACTION_BOOT_COMPLETED` and `ACTION_MY_PACKAGE_REPLACED`).
  There is NO `ACTION_DATE_CHANGED`, `ACTION_TIME_CHANGED`, `ACTION_TIMEZONE_CHANGED` broadcast receiver, nor any WorkManager periodic worker (`PeriodicWorkRequestBuilder<MidnightResetWorker>(24, TimeUnit.HOURS)`).
- **Impact**:
  If a phone remains powered on across midnight:
  - Used emergency passes remain 0 and never replenish.
  - Reel counts do not reset at 12:00 AM.
  - The detox streak (`KEY_STREAK_DAYS`) is never incremented. In fact, `PreferencesRepository.incrementStreak()` is orphaned and never called anywhere in the codebase.
- **Original APK Parity**:
  Original BrainPal registers `com.brainrot.android.core.receiver.DateChangedReceiver` in `AndroidManifest.xml` with intent filters for `android.intent.action.DATE_CHANGED` and `android.intent.action.TIME_SET`, alongside `DailyHeartbeatWorker` and `ReelsSyncWorker`.

---

### 2.6. Stats Screen Composed Entirely of Hardcoded Strings and Tuples
- **Location**: `source/app/src/main/java/com/brainrot/rebuilt/ui/screens/StatsScreen.kt#L29-L37` & `L74-L90` & `L159-L176`
- **Code**:
  ```kotlin
  val hourlyData = remember {
      listOf(
          Pair("9 AM", 4), Pair("11 AM", 12), Pair("1 PM", 22),
          Pair("3 PM", 18), Pair("5 PM", 35), Pair("7 PM", 42),
          Pair("9 PM", 28), Pair("11 PM", 15)
      )
  }
  ...
  Text(text = "-46%", ...)
  Text(text = "Reels vs Last Week", ...)
  Text(text = "• You saved approximately 3.2 hours of scrolling this week.", ...)
  Text(text = "• Instagram interruptions prevented: 18 times.", ...)
  ```
- **Defect Description**:
  The entire "Detox Analytics" screen is a static wireframe mock. None of the chart bars or stats reflect actual usage queries.
- **Impact**:
  Users see unchanging numbers ("-46%", "3.2 hours saved", "18 times") regardless of their actual device behavior.

---

### 2.7. Unfiltered Scroll Interception in Accessibility Service
- **Location**: `source/app/src/main/java/com/brainrot/rebuilt/service/ReelsAccessibilityService.kt#L60-L78`
- **Defect Description**:
  When an `AccessibilityEvent.TYPE_VIEW_SCROLLED` event arrives, the service only verifies `app.prefsRepository.isAppBlocked(pkg)` and an 850ms debounce. It performs **no UI hierarchy inspection** (e.g. checking for `ViewPager2`, `ReelsRecyclerView`, `ShortsContainer`, vertical swipe velocity, or view ID matching).
- **Impact**:
  Scrolling a direct message thread, reading comments, scrolling someone's profile, or viewing search results in Instagram increments the reel counter.

---

## 3. Comparison with Original APK Architecture

| Metric / Mechanism | Original BrainPal (`com.brainrot.android`) | Rebuilt App (`com.brainrot.rebuilt`) | Assessment |
|---|---|---|---|
| **Reel Event Storage** | Room DB: `WeekEventDb` (`appId`, `timestamp`, `duration`) | JVM In-Memory static `reelScrollCountToday` | **CRITICAL DEFECT** (Data loss on process kill) |
| **Hourly Usage Aggregation** | `HourlyReelsSplit` calculated from Room event timestamps | Hardcoded static 8-tuple list | **CRITICAL DEFECT** (Mock data in release) |
| **Screen Time Target Filter** | Filtered by target package set in `UserBlockingConfig` | Sum of all packages on entire device | **HIGH DEFECT** (Pre-mature block) |
| **Per-App Quota Modeling** | Per-package `limitPerDay`, `reelsAllowedCount` | Hardcoded `30` mins per app; global slider only | **HIGH DEFECT** (Lack of per-app limits) |
| **Midnight Reset Trigger** | `DateChangedReceiver` + `DailyHeartbeatWorker` | Boot receiver only (`ACTION_BOOT_COMPLETED`) | **HIGH DEFECT** (No overnight reset) |
| **UI Lifecycle Sync** | Reactive Flow / LiveData backed by Room | `LaunchedEffect(Unit)` (one-shot, non-reactive) | **MEDIUM DEFECT** (Stale UI metrics) |
| **Shorts/Reels Detection** | View hierarchy check + scroll gesture parser | Plain `TYPE_VIEW_SCROLLED` in package | **MEDIUM DEFECT** (Overcounting non-video scrolls) |

