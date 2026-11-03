# Settings Gap Analysis: Rebuilt App vs. Original BrainPal

**Document**: `14-settings-gap-analysis.md`  
**Date**: November 2, 2026  
**Auditor**: Autonomous Principal Android QA & Security Auditor  
**Scope**: Code-Level Comparison of Preference Models, Storage Engines, Configuration Keys, and Runtime Behaviors  
**Status**: COMPLETE — MAJOR FUNCTIONAL & ARCHITECTURAL GAPS IDENTIFIED

---

## 1. Executive Summary

In original BrainPal (`BrainPal.apk` / `com.brainrot.android`), configuration and user preferences are managed through a structured local Room database (`UserPrefDatabase` and `UserBlockingConfig`) with 12 distinct configuration parameters supporting per-app limits, cooldown timers, hard block windows, pace nudges, bubble sizes, and bedtime schedules.

In contrast, the rebuilt codebase (`source`) stores settings in an untyped, basic `SharedPreferences` file ("brainpal_prefs") with only 9 keys. Critical configuration capabilities present in the original APK are either completely omitted, restricted to hardcoded mocks, or decoupled from actual runtime enforcement.

---

## 2. Preference Storage Architecture Comparison

### 2.1. Rebuilt Storage Model (`PreferencesRepository.kt`)
The rebuilt application defines 9 static keys in `SharedPreferences`:
```kotlin
private const val KEY_BLOCKED_PACKAGES = "blocked_packages"
private const val KEY_DAILY_LIMIT_MINS = "daily_limit_mins"
private const val KEY_DAILY_REEL_LIMIT = "daily_reel_limit"
private const val KEY_ROAST_LANG = "roast_lang"
private const val KEY_ONBOARDING_DONE = "onboarding_done"
private const val KEY_SNOOZE_UNTIL = "snooze_until"
private const val KEY_STREAK_DAYS = "streak_days"
private const val KEY_STRICT_MODE = "strict_mode"
private const val KEY_PASSES_LEFT = "emergency_passes_left"
```

### 2.2. Original APK Storage Model (`UserPrefDatabase` & `UserBlockingConfig`)
Extracted from `classes.dex` and `classes2.dex`:
1. **`UserBlockingConfig` Table**:
   - `brUserId`: String (unique local user identifier)
   - `isBlockEnabled`: Boolean (master blocking toggle)
   - `blockStartTimestamp`: Long (window start epoch for daily quota reset)
   - `blockStartReelCount`: Integer (baseline reel count at window start)
   - `reelsAllowedCount`: Integer (session / daily allowed reel count)
   - `reelsAllowedValidForMillis`: Long (duration of allowed session before block)
   - `cooldownTimeInMillis`: Long (mandatory cooldown wait duration between sessions)
   - `hardBlockValidTill`: Long (strict non-bypassable block timestamp)
   - `blockPauseExpiryTime`: Long (active snooze expiration epoch)
   - `pinnedFriendBrUserId`: String (accountability partner identifier)
   - `updatedAt`: Long (local mutation timestamp)
   - `lastSyncedUpdatedAt`: Long (sync timestamp)
2. **`UserPrefDatabase` Entities**:
   - `UserPrefBoolean` (`userPrefKey`, `value`)
   - `UserPrefLong` (`userPrefKey`, `value`)
   - `UserPrefString` (`userPrefKey`, `value`)
3. **Widget & Bubble Settings (DEX Strings)**:
   - `counterSize`: Floating counter pill size configuration
   - `showCounterSizeBottomSheet`: Bottom sheet to pick compact vs large pill
   - `ACCOUNT_SETTINGS_CREATOR_LARGE_BUBBLE_TOGGLED`: Toggle for creator large pill
   - `showCounterEnabledConfirmation`: User notification confirmation on pill enable
   - `milestoneCounterExperienceVariantV1`: Variant experiment flag

---

## 3. Comprehensive Settings Gap Matrix

| # | Feature / Setting | Original APK (`BrainPal.apk`) | Rebuilt App (`source`) | Gap Severity | Runtime Behavior in Rebuilt App |
|---|---|---|---|---|---|
| **1** | **Target App Selection** | Dynamic query of all installed apps on device (`PackageManager`) | Hardcoded list of 8 apps in `AppLimitsScreen.kt` | **HIGH** | User cannot protect any app outside the 8 hardcoded defaults. |
| **2** | **Per-App Quota Limits** | Configurable per package (`limitPerDay`, `reelsAllowedCount`) | Single global slider (`daily_limit_mins`) applied to all apps | **HIGH** | Setting 15 mins applies identically to Instagram and YouTube; cannot set different limits. |
| **3** | **Cooldown Timer** | Supported (`cooldownTimeInMillis`); enforces mandatory waiting time between sessions | Not implemented | **HIGH** | User has no session cooldown mechanics; only emergency pass snooze exists. |
| **4** | **Hard Block Window / Schedule** | Supported (`hardBlockValidTill`, Bedtime / Focus Schedule) | Not implemented | **HIGH** | No schedule or bedtime rules exist (e.g. automatic blocking after 10:00 PM). |
| **5** | **Floating Bubble Settings** | Configurable toggle and size options (`counterSize`, `showCounterSizeBottomSheet`) | No settings UI; service spawns view unconditionally | **MEDIUM** | User has zero control over whether the floating bubble appears or its size. |
| **6** | **Pace Nudge Alerts** | Vibration / Toast nudge if user scrolls too fast (`PaceNudgeWorker`) | Not implemented | **MEDIUM** | No scroll-rate pacing warnings. |
| **7** | **Notification Settings** | Daily wrap, morning motivation, and trial ending push toggles | No notification settings UI | **MEDIUM** | Notification channel is created, but no user preferences exist to control notifications. |
| **8** | **Strict Mode Behavior** | Prevents editing limits or bypassing once quota is reached | Triggers instant fullscreen block on app launch | **CRITICAL** | Enabling Strict Mode causes monitored apps to be blocked immediately even with 0 usage! |
| **9** | **Data Management & Streak Reset** | Clear usage database, export stats, reset detox streak | Single "Reset" button for emergency passes only | **LOW** | Streak count is read-only (defaults to 3); no way to reset streak or clear stats. |
| **10** | **Emergency Pass Defaults** | 3 emergency passes per day | Defaults to 2 passes in `PreferencesRepository.kt` | **LOW** | Documented as 3 in reports, but coded as `getInt(..., 2)` and `putInt(..., 2)`. |

---

## 4. Deep Dive: Settings That Save Visually But Do Not Control Behavior

### 4.1. Strict Mode Logic Inversion
- **UI Element**: `SettingsScreen.kt#L121` and `HomeScreen.kt#L225` ("Strict Mode" Switch)
- **Observed Persistence**: Toggling the switch persists `KEY_STRICT_MODE = true/false` in `SharedPreferences`.
- **Defective Behavior in Service**:
  In `UsageMonitorService.kt#L61-L67`:
  ```kotlin
  val minutesSpent = ((currentAppStat?.usageTimeMillis ?: 0L) / 60000L).toInt()
  val limit = app.prefsRepository.getDailyLimitMinutes()

  if (minutesSpent >= limit || app.prefsRepository.isStrictMode()) {
      launchOverlay(foregroundPkg)
      delay(4000L)
  }
  ```
  If `isStrictMode()` is true, the condition evaluates to `true` **immediately**, regardless of `minutesSpent`.
  The user is blocked from opening Instagram the very second they tap the icon, even on minute 0 of their 30-minute quota.
- **Expected Behavior**:
  Strict Mode should allow the user to use their quota (up to `limit`), but once the limit is reached, it disables emergency bypasses, prevents snoozing, and locks the limits screen so the user cannot inflate their allowed time.

### 4.2. Per-App Guard Toggles Compose State Sync
- **UI Element**: `AppLimitsScreen.kt#L164-L171` (Switch per app item)
- **Code**:
  ```kotlin
  Switch(
      checked = item.isGuarded,
      onCheckedChange = { checked ->
          item.isGuarded = checked
          saveBlockedList()
      }
  )
  ```
  `item` is an instance of plain Kotlin class `AppConfigItem(val packageName: String, val displayName: String, var isGuarded: Boolean)`.
  Because `isGuarded` is a regular `var` and NOT a Compose `MutableState`, toggling it does not notify Compose of a state mutation. The switch animation and checked state can stutter or fail to recompose unless the parent list triggers a refresh.

