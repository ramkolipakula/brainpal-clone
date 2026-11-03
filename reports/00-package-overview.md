# 00 - Package Overview: BrainPal (BrainRot)

## 1. Application Identity
- **Original Package Identifier**: `com.brainrot.android`
- **Application Name / Label**: BrainPal (internally referred to as BrainRot / BrainPal)
- **Target Replacement Package**: `com.brainrot.rebuilt` (or `com.brainpal.rebuilt` to avoid namespace collisions on installation)
- **Source APK Path**: `D:\brainpal clone\BrainPal.apk`
- **Preserved APK Location**: `D:\APK-Rebuild\original\BrainPal.apk`
- **Original APK Size**: 41,942,918 bytes (~40.0 MB)
- **SHA-256 Checksum**: `10E9EA8EF39D057B83517E7216F0CFCC1664C7F55A0E407ACAC2F5A35724270B`

## 2. Platform SDK Configuration
- **Minimum SDK (minSdkVersion)**: 26 (Android 8.0 Oreo)
- **Target SDK (targetSdkVersion)**: 34 / 35 (Android 14 / 15)
- **Compile SDK**: 34 / 35
- **Native Architectures**: Pure Java/Kotlin bytecode application (no C/C++ `.so` libraries in `lib/`), maximizing compatibility across all ARM64, ARMv7, x86, and x86_64 devices.

## 3. Technology Stack & Framework
- **Language**: Kotlin 1.9+ / 2.0
- **UI Framework**: Modern Jetpack Compose with Material 3 (XML layouts used almost exclusively for the floating bubble, Glance widgets, and system wrappers).
- **Architecture**: Clean Architecture + MVI/MVVM with Dagger Hilt dependency injection.
- **Local Persistence**: Room Database (`BrainRotRoomDatabase`, `UserPrefDatabase`, `InviteDatabase`) and Jetpack DataStore / SharedPreferences.
- **Background Processing**: Android `WorkManager` for scheduled background workers (`BlockConfigSyncWorker`, `RoastGifCatalogSyncWorker`, `DailyHeartbeatWorker`, `ReelsSyncWorker`, `PaceNudgeWorker`).
- **Foreground Interventions**:
  - `ReelsAccessibilityService` (`android.accessibilityservice.AccessibilityService`): detects real-time short-form video feed scrolling (Instagram Reels, YouTube Shorts, Snapchat Spotlight, etc.).
  - `ReelsCounterFloatingService`: floating overlay bubble with real-time reel counters.
  - `BlockReelsOverlayActivity`: full-screen intervention overlay that intercepts blocked apps and presents multi-language meme roasts.
- **Widget Integration**: Android Jetpack Glance widgets (`BuddyWidgetReceiver`, `ReelsCounterWidgetReceiver`) providing homescreen Buddy mascot and live stats.
- **Media & Assets**: ExoPlayer (`androidx.media3`), Lottie animations (`confetti.lottie`, `offer_paywall_bg.lottie`), and localized roast GIF/WebP catalog (`roast_gifs.json`).

