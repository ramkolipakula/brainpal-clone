---
last_mapped_commit: 5d0167a094dcd794743bbddfda78d984b50cf3d5
last_mapped_at: 2026-11-03
---
# Codebase Concerns

**Analysis Date:** 2026-11-03

## Tech Debt

**Persistence Architecture:**
- Issue: State persistence relies on Android `SharedPreferences` rather than modern Jetpack `DataStore` or `Room`.
- Files: `source/app/src/main/java/com/brainrot/rebuilt/data/PreferencesRepository.kt`
- Impact: While `PreferencesRepository` mitigates jank by wrapping keys in memory `StateFlow`s, `SharedPreferences` lacks transactional ACID safety and schema migration tooling.
- Fix approach: Migrate to Jetpack Proto DataStore or Preferences DataStore for asynchronous, thread-safe persistence.

**Hardcoded Third-Party App Signatures:**
- Issue: Package names (`com.instagram.android`, `com.google.android.youtube`) and UI view hierarchy keywords are hardcoded.
- Files: `source/app/src/main/java/com/brainrot/rebuilt/domain/ReelDetectionEngine.kt`
- Impact: If Instagram or YouTube modifies their internal layout resource IDs, ViewPager class structures, or content descriptions in a future app update, detection accuracy could degrade.
- Fix approach: Externalize detection patterns to a configurable JSON ruleset that can be updated without recompiling the entire APK.

## Known Bugs & Remediated Defect History

**Remediated Defects (Reports 15, 17, 18):**
- Resolved DEF-10: Daily counters previously did not automatically reset at midnight. Fixed by implementing `DateChangedReceiver` (`source/app/src/main/java/com/brainrot/rebuilt/service/DateChangedReceiver.kt`).
- Resolved DEF-14: Default emergency passes misaligned. Synchronized to 3 default passes.
- Resolved DEF-19: Instagram Reels real-device swipe detection missed events due to FrameLayout zero-delta events. Fixed in `ReelDetectionEngine.kt` by evaluating window keywords and class hierarchies.

**Potential Live Edge Case:**
- Symptoms: If the device clock is manually turned backward by the user, daily limit calculations could behave unexpectedly until the next calendar day.
- Files: `source/app/src/main/java/com/brainrot/rebuilt/data/PreferencesRepository.kt`
- Workaround: System date change receiver catches `TIME_SET` broadcasts and resynchronizes active date keys.

## Security Considerations

**Accessibility Service Privileges:**
- Risk: `BIND_ACCESSIBILITY_SERVICE` is a highly privileged Android permission capable of reading on-screen elements.
- Files: `source/app/src/main/java/com/brainrot/rebuilt/service/ReelsAccessibilityService.kt`, `source/app/src/main/res/xml/accessibility_service_config.xml`
- Current mitigation: The accessibility service configuration explicitly restricts event monitoring to `TYPE_VIEW_SCROLLED` and `TYPE_WINDOW_STATE_CHANGED`, and limits package inspection to tracked social apps only. No text input, password fields, or user keystrokes are accessed or stored.
- Recommendations: Ensure documentation and onboarding screens clearly explain the privacy guarantees to users.

**System Alert Window Permission:**
- Risk: `SYSTEM_ALERT_WINDOW` allows drawing over other applications.
- Files: `source/app/src/main/AndroidManifest.xml`, `source/app/src/main/java/com/brainrot/rebuilt/BlockOverlayActivity.kt`
- Current mitigation: Used strictly for the floating reel counter capsule and intervention dialogs.

## Performance Bottlenecks

**Accessibility Event Frequency:**
- Problem: Accessibility scroll events are fired at high frequency during continuous user scrolling in short-video feeds.
- Files: `source/app/src/main/java/com/brainrot/rebuilt/service/ReelsAccessibilityService.kt`, `source/app/src/main/java/com/brainrot/rebuilt/domain/ReelDetectionEngine.kt`
- Cause: Fast scroll flings trigger multiple accessibility notifications per second.
- Improvement path: Pre-filtering on package name and event type before inspecting view hierarchy keywords; 800ms debounce window prevents expensive repetitive operations.

**Usage Monitor Polling Interval:**
- Problem: `UsageMonitorService` coroutine polls every 1,000ms.
- Files: `source/app/src/main/java/com/brainrot/rebuilt/service/UsageMonitorService.kt`
- Cause: Checking foreground app time to trigger timely limit interventions.
- Improvement path: If device screen turns off (`Intent.ACTION_SCREEN_OFF`), pause polling coroutines to optimize battery life.

## Fragile Areas

**Aggressive OEM Battery Optimization (Xiaomi / Samsung / OnePlus):**
- Files: `source/app/src/main/AndroidManifest.xml`, `install_on_mobile.ps1`
- Why fragile: Custom OEM Android skins aggressively terminate background foreground services and disable accessibility services if battery optimization is active.
- Safe modification: Retain `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` in manifest and provide user guidance in `OnboardingScreen` for OEM-specific autostart settings.
- Test coverage: Tested on real Xiaomi and stock Android test devices.

## Scaling Limits

**Blocked Package Set:**
- Current capacity: Tested with 5–20 blocked apps.
- Limit: Set of strings in SharedPreferences. Practical limit is hundreds of packages before serialization overhead becomes noticeable.
- Scaling path: Ample capacity for typical personal digital wellbeing usage (3–10 target apps).

## Dependencies at Risk

**Android Platform Foreground Service Restrictions:**
- Package: Android OS Target SDK 34 (`FOREGROUND_SERVICE_SPECIAL_USE`)
- Risk: Android 14+ introduces strict enforcement on foreground service types. Play Store review requires explicit declaration for `specialUse`.
- Impact: If published to Google Play, Google may request demonstration of necessity.
- Migration plan: Sideloaded / direct APK delivery remains uninhibited; keep declaration property `PROPERTY_SPECIAL_USE_FGS_SUBTYPE` properly populated in `AndroidManifest.xml`.

## Test Coverage Gaps

**Compose UI Instrumented Rendering Tests:**
- What's not tested: Visual snapshot / Jetpack Compose UI instrumented tests (`composeTestRule`) are not currently running in headless CI.
- Files: `source/app/src/main/java/com/brainrot/rebuilt/ui/screens/*.kt`
- Risk: Visual layout bugs or theme color regressions could occur unnoticed during UI refactoring.
- Priority: Medium (Currently covered by manual device screenshots in `reports/` and PowerShell ADB deployment tests).

---

*Concerns audit: 2026-11-03*
