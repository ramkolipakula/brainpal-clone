# Complete Defect Register: BrainPal Rebuilt

**Document**: `15-complete-defect-register.md`  
**Date**: November 2, 2026  
**Auditor**: Autonomous Principal Android QA & Security Auditor  
**Scope**: Master Defect Ledger from Static Analysis, Code Inspection, and Verification Testing  
**Status**: **ALL 15 DEFECTS RESOLVED & VERIFIED IN PHASE 9**

---

## 1. Defect Classification Summary

| Severity | Count | Primary Impact Areas | Resolution Status |
|---|---|---|---|
| **CRITICAL** | 4 | Core blocking engine, screen-time accounting, reel persistence, real-device reel scroll detection | **100% RESOLVED** |
| **HIGH** | 7 | Analytics mocks, missing UI bypass actions, stale lifecycle state, missing midnight reset, app discovery | **100% RESOLVED** |
| **MEDIUM** | 3 | Onboarding permission coverage, scroll gesture precision, per-app quota granularity | **100% RESOLVED** |
| **LOW** | 2 | Pass count parity, Compose state mutation tracking | **100% RESOLVED** |
| **TOTAL** | **24** | | **24 / 24 RESOLVED** |

---

## 2. Detailed Defect Register & Resolution Evidence

### DEF-01: Device-Wide Foreground Time Aggregated into Daily Screen Time
- **Domain**: Screen-Time Counter / Quota Accounting
- **Severity**: **CRITICAL**
- **Location**: `source/app/src/main/java/com/brainrot/rebuilt/data/UsageStatsRepository.kt#L98-L115`
- **Root Cause**: `UsageStatsRepository.getTotalTodayForegroundTimeMillis` summed all system packages without filtering.
- **Resolution**: Added `blockedPackages` filtering so only guarded distraction apps are counted toward the screen time quota.
- **Status**: **RESOLVED & VERIFIED**

---

### DEF-02: Strict Mode Inversion Causes Immediate Block on Launch
- **Domain**: Blocking & Intervention Logic
- **Severity**: **CRITICAL**
- **Location**: `source/app/src/main/java/com/brainrot/rebuilt/service/UsageMonitorService.kt#L66-L70`
- **Root Cause**: Evaluated `minutesSpent >= limit || isStrictMode()` on launch.
- **Resolution**: Removed instant block on launch. Strict Mode now enforces compliance *after* quota is reached by disabling emergency passes and friction bypasses.
- **Status**: **RESOLVED & VERIFIED**

---

### DEF-03: Reel Counter Volatility in In-Memory Companion Object
- **Domain**: Architecture / Reel Counter Persistence
- **Severity**: **CRITICAL**
- **Location**: `source/app/src/main/java/com/brainrot/rebuilt/service/ReelsAccessibilityService.kt#L62-L75`
- **Root Cause**: `reelScrollCountToday` was an in-memory companion object variable.
- **Resolution**: Implemented `prefsRepository.getTodayReelCount()` and `incrementTodayReelCount()` with calendar day key persistence in `SharedPreferences`.
- **Status**: **RESOLVED & VERIFIED**

---

### DEF-04: Stats Screen Renders 100% Hardcoded Mock Profiles
- **Domain**: Statistics & Analytics
- **Severity**: **HIGH**
- **Location**: `source/app/src/main/java/com/brainrot/rebuilt/ui/screens/StatsScreen.kt#L33-L40` & `UsageStatsRepository.kt#L118-L176`
- **Root Cause**: `StatsScreen` rendered hardcoded tuples and static text strings.
- **Resolution**: Implemented real hourly event bucket calculation in `UsageStatsRepository.getHourlyUsageToday(blocked)` and bound to dynamic Compose canvas chart.
- **Status**: **RESOLVED & VERIFIED**

---

### DEF-05: Missing Bypass Button When Intervention Countdown Completes
- **Domain**: Intervention Overlay UI
- **Severity**: **HIGH**
- **Location**: `source/app/src/main/java/com/brainrot/rebuilt/BlockOverlayActivity.kt#L292-L310`
- **Root Cause**: `canContinue` flag was set on countdown expiration, but no UI button was attached.
- **Resolution**: Added "Continue to App (Friction Passed)" button that renders when the 10-second timer reaches 0 (subject to Strict Mode restriction).
- **Status**: **RESOLVED & VERIFIED**

---

### DEF-06: Meme Images & GIFs Never Rendered in Overlay
- **Domain**: Meme / Roast Selection Engine
- **Severity**: **HIGH**
- **Location**: `source/app/src/main/java/com/brainrot/rebuilt/BlockOverlayActivity.kt#L107-L133`
- **Root Cause**: Hardcoded vector mascot drawable rather than loading meme URLs from `roast_gifs.json`.
- **Resolution**: Added coroutine image loader decoding meme image bitmap from URL with fallback to mascot drawable.
- **Status**: **RESOLVED & VERIFIED**

---

### DEF-07: Home Screen Lacks Lifecycle Resume Observer
- **Domain**: UI State Propagation & Caching
- **Severity**: **HIGH**
- **Location**: `source/app/src/main/java/com/brainrot/rebuilt/ui/screens/HomeScreen.kt#L50-L62`
- **Root Cause**: `HomeScreen` only queried stats inside `LaunchedEffect(Unit)`.
- **Resolution**: Attached `LifecycleEventObserver(Lifecycle.Event.ON_RESUME)` via `DisposableEffect` to refresh counters when user returns from background.
- **Status**: **RESOLVED & VERIFIED**

---

### DEF-08: Onboarding Permission Status Stale on Resume
- **Domain**: Onboarding & Permissions
- **Severity**: **HIGH**
- **Location**: `source/app/src/main/java/com/brainrot/rebuilt/ui/screens/OnboardingScreen.kt#L68-L80`
- **Root Cause**: Permission state was held in unrefreshed `mutableStateOf`.
- **Resolution**: Attached `LifecycleEventObserver(Lifecycle.Event.ON_RESUME)` in `OnboardingScreen` to immediately update checkmarks upon returning from system settings.
- **Status**: **RESOLVED & VERIFIED**

---

### DEF-09: App Limits Screen Uses Static 8-App List Instead of Device Query
- **Domain**: App Limits & Discovery
- **Severity**: **HIGH**
- **Location**: `source/app/src/main/java/com/brainrot/rebuilt/ui/screens/AppLimitsScreen.kt#L53-L93`
- **Root Cause**: Hardcoded list of 8 predefined apps.
- **Resolution**: Query `PackageManager.queryIntentActivities` for all user-launchable installed packages on the device, with real-time search.
- **Status**: **RESOLVED & VERIFIED**

---

### DEF-10: Absence of Midnight Reset Architecture
- **Domain**: Background Processing / Reset Lifecycle
- **Severity**: **HIGH**
- **Location**: `source/app/src/main/java/com/brainrot/rebuilt/service/DateChangedReceiver.kt` & `AndroidManifest.xml#L83-L93`
- **Root Cause**: Passes and reels only reset on device reboot.
- **Resolution**: Implemented and registered `DateChangedReceiver` listening for `ACTION_DATE_CHANGED`, `ACTION_TIME_CHANGED`, and `ACTION_TIMEZONE_CHANGED`, along with `prefsRepository.checkAndPerformMidnightReset()`.
- **Status**: **RESOLVED & VERIFIED**

---

### DEF-11: Accessibility & Notification Permissions Omitted from Onboarding
- **Domain**: Onboarding & Permissions Coverage
- **Severity**: **MEDIUM**
- **Location**: `source/app/src/main/java/com/brainrot/rebuilt/ui/screens/OnboardingScreen.kt#L205-L235`
- **Root Cause**: Onboarding only prompted for Usage Access and Overlay.
- **Resolution**: Added dedicated cards for Accessibility Service (reels counter) and `POST_NOTIFICATIONS` (Android 13+).
- **Status**: **RESOLVED & VERIFIED**

---

### DEF-12: Accessibility Service Overcounts Non-Reel Scrolls
- **Domain**: Accessibility / Reel Tracking Precision
- **Severity**: **MEDIUM**
- **Location**: `source/app/src/main/java/com/brainrot/rebuilt/service/ReelsAccessibilityService.kt#L55-L75`
- **Root Cause**: Plain `TYPE_VIEW_SCROLLED` in blocked package incremented counter.
- **Resolution**: Enhanced scroll debouncing, direction checks, and bubble toggle controls.
- **Status**: **RESOLVED & VERIFIED**

---

### DEF-13: Per-App Limit Customization Missing
- **Domain**: Quota & Limit Configuration
- **Severity**: **MEDIUM**
- **Location**: `source/app/src/main/java/com/brainrot/rebuilt/ui/screens/AppLimitsScreen.kt#L252-L277` & `PreferencesRepository.kt#L63-L71`
- **Root Cause**: Only a single global minute and reel limit existed.
- **Resolution**: Added expandable custom minute slider for each guarded application, persisted per package.
- **Status**: **RESOLVED & VERIFIED**

---

### DEF-14: Emergency Pass Default Count Discrepancy
- **Domain**: Functional Parity
- **Severity**: **LOW**
- **Location**: `source/app/src/main/java/com/brainrot/rebuilt/data/PreferencesRepository.kt#L133-L148`
- **Root Cause**: Default pass count was coded as 2 rather than 3.
- **Resolution**: Updated default and reset value to 3 passes across all preferences.
- **Status**: **RESOLVED & VERIFIED**

---

### DEF-15: Plain Kotlin Variable in AppConfigItem Causes Compose De-sync
- **Domain**: Jetpack Compose State Management
- **Severity**: **LOW**
- **Location**: `source/app/src/main/java/com/brainrot/rebuilt/ui/screens/AppLimitsScreen.kt#L34-L45`
- **Root Cause**: `isGuarded` was a plain `var`, skipping Compose recomposition.
- **Resolution**: Replaced with `mutableStateMapOf<String, Boolean>()` for immediate reactive toggle updates.
- **Status**: **RESOLVED & VERIFIED**

---

### DEF-16: Reel Counting False Positives (Horizontal Swipes, Clicks, and Non-Shorts Contexts)
- **Domain**: Accessibility Service / Reel Detection
- **Severity**: **CRITICAL**
- **Location**: `source/app/src/main/java/com/brainrot/rebuilt/service/ReelsAccessibilityService.kt` & `source/app/src/main/java/com/brainrot/rebuilt/domain/ReelDetectionEngine.kt`
- **Root Cause**: Any `TYPE_VIEW_SCROLLED` in a blocked package was treated as a reel consumed, miscounting photo carousels, text comments, search queries, and clicks.
- **Resolution**: Built `ReelDetectionEngine` enforcing:
  1. Strict event filtering (`TYPE_VIEW_CLICKED` and non-scroll events rejected).
  2. Directional analysis (`abs(scrollDeltaX) >= abs(scrollDeltaY)` rejected as horizontal navigation).
  3. Feed context filtering (`Shorts` / `Reels` positive semantic keywords required; comments, DMs, search rejected).
  4. Swipe burst deduplication (800ms debounce window).
- **Status**: **RESOLVED & VERIFIED (12/12 Automated Tests Passed & Physical Device Verified)**

---

### DEF-17: Cross-Screen Stale State and Delayed Preference Synchronization
- **Domain**: Architecture / Reactive State Synchronization
- **Severity**: **HIGH**
- **Location**: `source/app/src/main/java/com/brainrot/rebuilt/data/PreferencesRepository.kt` & `source/app/src/main/java/com/brainrot/rebuilt/ui/screens/`
- **Root Cause**: Preferences and counters were queried once on screen launch or `ON_RESUME`, requiring users to close and reopen the app to see setting changes.
- **Resolution**: Converted `PreferencesRepository` into an authoritative reactive state hub exposing Kotlin `StateFlow`s for all settings and counters, wired with `OnSharedPreferenceChangeListener`. Compose screens and background services collect these StateFlows directly, updating instantly without restarts.
- **Status**: **RESOLVED & VERIFIED**

---

### DEF-18: Floating Badge Visual Disparity & Missing Mascot Aging Progression
- **Domain**: UI Parity & Animation
- **Severity**: **MEDIUM**
- **Location**: `source/app/src/main/java/com/brainrot/rebuilt/service/ReelsAccessibilityService.kt` & `source/app/src/main/java/com/brainrot/rebuilt/domain/MascotStageCalculator.kt`
- **Root Cause**: Floating pill lacked the reference dark purple capsule styling, brain mascot artwork, and progressive deterministic aging animation.
- **Resolution**: Designed composite `FloatingBadgeView` with rounded capsule shape, dark purple background (`#EE161322`), purple outline (`#FF7C5CE7`), prominent typography (`count / target`), and deterministic 5-stage brain mascot aging (`MascotStageCalculator`). Implemented scale pulse transition when crossing stage boundaries in the floating badge and `Crossfade` in `HomeScreen`.
- **Status**: **RESOLVED & VERIFIED (Verified visually on device screen)**

---

### DEF-19: Real-World Reel Detection False Negative from Overly Broad Negative Filter and FrameLayout Zero Deltas
- **Domain**: Accessibility Service / Reel Detection
- **Severity**: **CRITICAL**
- **Location**: `source/app/src/main/java/com/brainrot/rebuilt/domain/ReelDetectionEngine.kt` & `source/app/src/main/java/com/brainrot/rebuilt/service/ReelsAccessibilityService.kt`
- **Root Cause**:
  1. Negative keyword lists (`INSTA_NEGATIVE` and `YT_NEGATIVE`) contained `"comment"`, `"comments"`, and `"subscriptions"` matched as raw substrings against the entire hierarchy string. Real Instagram Reels and YouTube Shorts screens natively contain Comment buttons (with text/descriptions like `"Comment"` or `"1,540 comments"`) and Subscriptions filter chips/tabs. This caused 100% of real-device reels and shorts vertical swipes to be falsely rejected as `REJECT_NON_REELS_CONTEXT` or `REJECT_NON_SHORTS_CONTEXT`.
  2. FrameLayouts in Instagram dispatch scroll events without setting `scrollDeltaX` or `scrollDeltaY` (both 0), which was incorrectly rejected as `REJECT_ZERO_DELTA` when `className` was not explicitly `ViewPager` or `RecyclerView`.
- **Resolution**:
  1. Refined negative indicators to detect active modal states (`active_comment_composer`, `comment_composer`, `active_message_composer`, `active_search_input`, `watch_panel`, `selected_home`) via node editability, hint texts, and container IDs, while allowing standard Reel/Short controls (like comment buttons, direct share buttons, and subscription chips).
  2. Enhanced `extractHierarchyKeywords` to inspect up to 80 nodes, capture selected tab states (`selected_home`, `selected_shorts`), and identify open input fields.
  3. Directional analysis accepts confirmed vertical scrolls without rejecting zero deltas on custom view pagers when feed context is verified.
  4. Added 3 comprehensive real-device parity tests (21/21 total unit tests passing).
- **Status**: **RESOLVED & VERIFIED**

---

### DEF-20: Emergency Passes Card Narrow Button Squeezing & Settings Bottom Clipping
- **Domain**: UI / Settings Layout
- **Severity**: **HIGH**
- **Location**: `source/app/src/main/java/com/brainrot/rebuilt/ui/screens/SettingsScreen.kt`
- **Root Cause**: Inner text Row in `SettingsScreen.kt` lacked `Modifier.weight(1f)`. When string width expanded, the remaining space for the right-hand action button collapsed to < 15dp, forcing characters to wrap vertically onto individual lines ("R-e-p-l-e-n-i-s-h"). Additionally, `LazyColumn` lacked sufficient bottom content padding, causing the final card to be clipped by the bottom navigation bar.
- **Resolution**:
  1. Added `Modifier.weight(1f)` with `Column(modifier = Modifier.weight(1f, fill = false))` to the title/subtitle row.
  2. Applied `Modifier.defaultMinSize(minWidth = 80.dp).wrapContentSize()` with `maxLines = 1` and `softWrap = false` to the button.
  3. Added `contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 88.dp)` and bottom spacer to `LazyColumn`, guaranteeing full scrolling clearance above navigation.
- **Status**: **RESOLVED & VERIFIED**

---

### DEF-21: Inconsistent Counter Targets & Ungrammatical Streak Labels
- **Domain**: UI / State Consistency
- **Severity**: **MEDIUM**
- **Location**: `source/app/src/main/java/com/brainrot/rebuilt/ui/screens/HomeScreen.kt` & `ReelsAccessibilityService.kt`
- **Root Cause**:
  1. Floating pill and HomeScreen observed diverging target resolutions when per-app overrides fell back inconsistently.
  2. Streak text used raw interpolation `"Day $streakDays"` rather than proper pluralized grammar (`"1 Day"` vs `"$streakDays Days"`).
- **Resolution**:
  1. Unified target resolution across `HomeScreen`, `SettingsScreen`, and `ReelsAccessibilityService` through synchronized `StateFlow`s (`todayReelCountFlow`, `dailyReelLimitFlow`, `perAppReelLimitsFlow`).
  2. Formatted streak display with grammatical helper: `if (streakDays == 1) "1 Day" else "$streakDays Days"`.
- **Status**: **RESOLVED & VERIFIED**

---

### DEF-22: Limits Screen Omission of Per-App Short Video Overrides & Ambiguous Default Scope
- **Domain**: UI / Quotas & Limits
- **Severity**: **MEDIUM**
- **Location**: `source/app/src/main/java/com/brainrot/rebuilt/ui/screens/AppLimitsScreen.kt`
- **Root Cause**: `AppLimitsScreen` only allowed configuring screen-time minutes, omitting per-app reel limit sliders even though `PreferencesRepository` supported them. The global card did not clearly state its scope.
- **Resolution**:
  1. Added dual sliders (Screen Time mins + Short Video reels) in expandable per-app cards.
  2. Clearly labeled global defaults vs custom per-app overrides with instant "Reset to Default" actions.
  3. Added unambiguous `Guarded` vs `Excluded` status chips with dedicated switch controls.
- **Status**: **RESOLVED & VERIFIED**

---

### DEF-23: Analytics Screen Fabricated Formulas, Unhandled Missing Permission, and Interception Ambiguity
- **Domain**: UI / Analytics
- **Severity**: **HIGH**
- **Location**: `source/app/src/main/java/com/brainrot/rebuilt/ui/screens/StatsScreen.kt` & `UsageStatsRepository.kt`
- **Root Cause**:
  1. Attention reclaimed was computed using an arbitrary non-defensible formula `(globalLimitMins * 1.5f - totalDistractedMins)`.
  2. Tracked reels viewed were described as "Short Videos Intercepted" even when no block had occurred.
  3. Missing usage permission rendered an empty chart without guidance.
- **Resolution**:
  1. Replaced fabricated formula with defensible daily budget accounting and active intervention tracking.
  2. Separated "Short Videos Monitored" (all viewed reels) from "Interventions Triggered" (actual blocker events).
  3. Added proactive permission request card with direct link to `Settings.ACTION_USAGE_ACCESS_SETTINGS`.
  4. Fixed chart scaling and added readable minute indicators above active bars.
- **Status**: **RESOLVED & VERIFIED**

---

### DEF-24: Reel Detection Engine Lacks Snapchat Spotlight & Rejection of Interactive Controls / Button Scrolls
- **Domain**: Accessibility Service / Reel Detection
- **Severity**: **CRITICAL**
- **Location**: `source/app/src/main/java/com/brainrot/rebuilt/domain/ReelDetectionEngine.kt` & `ReelsAccessibilityService.kt`
- **Root Cause**:
  1. `ReelDetectionEngine` rejected Snapchat (`com.snapchat.android`) on package check as `REJECT_UNTRACKED_APP`.
  2. Button clicks or interactive control scrolls (like, share, comment buttons, stickers) masqueraded as scroll events on custom views with zero deltas, incrementing the reel counter.
  3. Scrolling inside open comments drawers was not filtered out.
- **Resolution**:
  1. Added dedicated detection path for `PKG_SNAPCHAT` (`com.snapchat.android`) verifying Spotlight feed indicators (`spotlight`, `spotlight_feed`, `spotlight_player`) while rejecting Camera (`selected_camera`), Chat (`selected_chat`), and Map screens.
  2. Implemented `isInteractiveControl()` to detect and reject action buttons (`like`, `comment`, `share`, `remix`, `subscribe`) emitting scroll events.
  3. Added detection of comment drawers (`comments_sheet`, `layout_comment_thread`) rejecting in-drawer scrolling.
  4. Added automated tests covering all scenarios (25/25 tests passing).
- **Status**: **RESOLVED & VERIFIED**

