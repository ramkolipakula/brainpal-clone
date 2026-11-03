---
last_mapped_commit: 5d0167a094dcd794743bbddfda78d984b50cf3d5
last_mapped_at: 2026-11-03
---
# External Integrations

**Analysis Date:** 2026-11-03

## APIs & External Services

**Digital Wellbeing & Platform Subsystems:**
- Android Usage Stats Subsystem - Query app foreground usage, sessions, and screen-time timestamps
  - SDK/Client: `android.app.usage.UsageStatsManager` accessed via `source/app/src/main/java/com/brainrot/rebuilt/data/UsageStatsRepository.kt`
  - Auth: System permission `android.permission.PACKAGE_USAGE_STATS` granted via Android Settings
- Android Accessibility Framework - Real-time view hierarchy observation for short-form reel swipe detection (Instagram & YouTube)
  - SDK/Client: `android.accessibilityservice.AccessibilityService` via `source/app/src/main/java/com/brainrot/rebuilt/service/ReelsAccessibilityService.kt`
  - Auth: User toggle in Android Accessibility Settings (`android.permission.BIND_ACCESSIBILITY_SERVICE`)
- Android Window Manager - System alert overlay window for floating reel counter capsule and intervention views
  - SDK/Client: `android.view.WindowManager` via `source/app/src/main/java/com/brainrot/rebuilt/service/ReelsAccessibilityService.kt`
  - Auth: Special permission `android.permission.SYSTEM_ALERT_WINDOW` ("Draw over other apps")

**Third-Party Web Services:**
- None detected. The reconstructed application is fully clean-room, privacy-preserving, and offline-first. No tracking, cloud telemetry, or third-party ad networks are integrated.

## Data Storage

**Databases:**
- Key-Value Store: Android `SharedPreferences` (`"brainpal_prefs"`)
  - Connection: Local application private sandbox (`Context.MODE_PRIVATE`)
  - Client: `PreferencesRepository` in `source/app/src/main/java/com/brainrot/rebuilt/data/PreferencesRepository.kt`
  - Schema: Stored keys for blocked package sets, daily reel limits, minute limits, bedtime schedule hours, streak count, emergency passes, and active date.

**File Storage:**
- Bundled Read-Only Assets: Bundled meme metadata and multi-language roast messages (`assets/roast_gifs.json`)
  - Client: `RoastMemeRepository` in `source/app/src/main/java/com/brainrot/rebuilt/data/RoastMemeRepository.kt`
- Local filesystem only: No external cloud storage or network bucket.

**Caching:**
- Reactive In-Memory Cache: Kotlin `StateFlow` primitives (`todayReelCountFlow`, `dailyReelLimitFlow`, `dailyLimitMinutesFlow`) in `source/app/src/main/java/com/brainrot/rebuilt/data/PreferencesRepository.kt` provide instant non-blocking reads across foreground screens and background services.

## Authentication & Identity

**Auth Provider:**
- None / Local Device Only: No user login or identity tokens required. All profile state (e.g. streaks, limits, passes) is keyed locally on-device.

## Monitoring & Observability

**Error Tracking:**
- Android Logcat logging via `android.util.Log` tagged with service domains (`BrainPalApp`, `UsageMonitor`, `ReelsAccessibility`, `ReelDetectionEngine`).
- Automated crash log extraction via PowerShell scripts (`tests/get_crash_log.ps1`).

**Logs:**
- Human-readable Markdown verification reports stored in `reports/` (reports 00 through 18).
- ADB instrumentation log outputs and unit test XML reports (`tests/TEST-com.brainrot.rebuilt.LogicUnitTest.xml`).

## CI/CD & Deployment

**Hosting:**
- Local artifact distribution in `delivery/RebuiltApp.apk`.

**CI Pipeline:**
- Local PowerShell build and verification automation scripts:
  - `tests/run_tests_and_build.ps1` - Runs Gradle test and assemble tasks
  - `install_on_mobile.ps1` / `install_on_mobile.bat` - Automates device discovery, uninstall, and ADB install
  - `deploy_device.ps1` - Grants all runtime permissions automatically post-install

## Environment Configuration

**Required permissions & system configuration:**
- `PACKAGE_USAGE_STATS` - Digital wellbeing access
- `SYSTEM_ALERT_WINDOW` - Floating counter overlay
- `BIND_ACCESSIBILITY_SERVICE` - Reels & Shorts swipe tracking
- `POST_NOTIFICATIONS` - Android 13+ foreground service notification
- `FOREGROUND_SERVICE` & `FOREGROUND_SERVICE_SPECIAL_USE` - Background usage monitoring
- `RECEIVE_BOOT_COMPLETED` - Automatic service start on device boot
- `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` - Prevents aggressive OEM background killing

**Secrets location:**
- No secrets or API keys required. No `.env` or external credentials in repository.

## Webhooks & Callbacks

**Incoming:**
- System Broadcasts:
  - `android.intent.action.BOOT_COMPLETED` and `android.intent.action.MY_PACKAGE_REPLACED` handled by `source/app/src/main/java/com/brainrot/rebuilt/service/BootReceiver.kt`
  - `android.intent.action.DATE_CHANGED`, `android.intent.action.TIME_SET`, `android.intent.action.TIMEZONE_CHANGED` handled by `source/app/src/main/java/com/brainrot/rebuilt/service/DateChangedReceiver.kt` for midnight streak and limit reset

**Outgoing:**
- None.

---

*Integration audit: 2026-11-03*
