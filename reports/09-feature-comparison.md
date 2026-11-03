# Feature Comparison Report: Original BrainPal vs. RebuiltApp

**Document**: `09-feature-comparison.md`  
**Date**: November 2, 2026  
**Original Application**: `BrainPal.apk` (`com.brainrot.android`, Version: 22.0.636, Target SDK: 36)  
**Rebuilt Application**: `RebuiltApp.apk` (`com.brainrot.rebuilt`, Version: 1.0.0, Target SDK: 34)  

---

## 1. Executive Summary

The reverse engineering and clean-room reconstruction of BrainPal resulted in a fully independent, offline-first digital wellbeing application. All core user-facing functionality—including foreground app tracking, reel scroll detection, tiered meme roasts, dynamic buddy mascot moods, emergency passes, and daily screen time analytics—was faithfully reconstructed without any proprietary backend dependencies or commercial payment gates.

---

## 2. Feature-by-Feature Comparison Matrix

| Feature Domain | Original BrainPal (`com.brainrot.android`) | Rebuilt App (`com.brainrot.rebuilt`) | Status | Notes & Rationale |
|---|---|---|---|---|
| **App Quotas & Limits** | Supported via package selection & daily minutes slider | Supported via package toggles & customizable time/reel sliders | ✅ Reproduced | Fully configurable in `AppLimitsScreen.kt` |
| **Foreground App Monitoring** | `UsageStatsManager` polling loop | `UsageStatsService` foreground service running every 1.5s | ✅ Reproduced | Background-safe on API 26-34 with `specialUse` FGS |
| **Reels / Scroll Counter** | Accessibility service inspecting scroll gestures | `ReelsAccessibilityService` intercepting `TYPE_VIEW_SCROLLED` | ✅ Reproduced | Increments scroll quota and triggers intervention |
| **Intervention Screen** | Fullscreen activity overlay with meme roast & friction timer | `BlockOverlayActivity` with Jetpack Compose UI & countdown | ✅ Reproduced | Displays localized meme, friction countdown, Exit & Emergency pass |
| **Buddy Mascot & Moods** | Dynamic mascot (Angry, Disappointed, Neutral, Happy) | Dynamic mascot state machine driven by quota consumption | ✅ Reproduced | Vector drawables preserved; mood switches automatically |
| **Meme Roast Engine** | 13 languages sourced from `roast_gifs.json` | 13 languages sourced from local `assets/roast_gifs.json` | ✅ Reproduced | Full Hindi, English, Tamil, Telugu, Indonesian meme pool with offline fallbacks |
| **Screen Time Analytics** | Hourly breakdown bar chart, weekly recovery curve | Compose-driven Canvas bar chart and streak counter | ✅ Reproduced | `StatsScreen.kt` computes daily and hourly usage from `UsageStatsManager` |
| **Language Selection** | In-app language picker dialog | Language selection modal with instant preference persistence | ✅ Reproduced | Available in `SettingsScreen.kt` |
| **Strict Mode & Snooze** | Prevents app modification after limit is reached | Strict mode lock toggle + 5-minute snooze button | ✅ Reproduced | Configurable in Settings & Overlay |
| **Emergency Passes** | 3 daily emergency bypass passes with 10-minute pass-through | 3 daily passes with atomic decrements and timer resets | ✅ Reproduced | Fully operational offline without paywall |
| **Onboarding Flow** | 4-step permission grant wizard | 4-step Compose onboarding carousel with direct Intent launchers | ✅ Reproduced | Guides user to grant Usage Stats, Overlay, and Accessibility |
| **In-App Billing (Pro)** | Razorpay, RevenueCat, Google Play Billing paywall | Removed; all features unlocked for free | ⚡ Intentional Exclusion | Commercial monetization excluded per clean-room specification |
| **Cloud Analytics / Sentry** | Sentry, Mixpanel, Firebase, AppsFlyer telemetry | Removed entirely; strict local privacy | ⚡ Intentional Exclusion | No user telemetry, device IDs, or tracking sent to cloud |
| **Proprietary Backend** | REST API calls to `puraa.regainapp.ai` | Replaced by local assets and local JSON parser | ⚡ Intentional Exclusion | App is 100% resilient and functional without external servers |

---

## 3. Metrics & Technical Characteristics

| Metric | Original APK (`BrainPal.apk`) | Rebuilt APK (`RebuiltApp.apk`) | Delta / Evaluation |
|---|---|---|---|
| **File Size** | 41,942,918 bytes (~40.0 MB) | 21,431,679 bytes (~20.4 MB) | **49% size reduction** via removal of commercial SDK bloat |
| **Min SDK** | 26 (Android 8.0 Oreo) | 26 (Android 8.0 Oreo) | Identical broad compatibility |
| **Target SDK** | 36 (Android 15+) | 34 (Android 14) | Target SDK 34 is the stable Google Play release target |
| **Signature Scheme** | APK Signature Scheme v2 | APK Signature Scheme v2 | Verified cryptographically with `apksigner` |
| **Third-Party Trackers** | 6 (Sentry, Mixpanel, Firebase, AppsFlyer, Razorpay, RevenueCat) | 0 (Zero trackers, 100% privacy-preserving) | Clean-room compliance |
| **Offline Operability** | Partially degraded without internet (memes fail, paywall blocks) | 100% operational offline with embedded fallback memes | Enhanced resilience |

---

## 4. Key Architectural Enhancements in Rebuilt Codebase

1. **Jetpack Compose 1.5.4 UI**:
   - Modern, declarative UI replacing legacy mixed views and XML layouts.
   - Fluid dark-mode theme (`#0E1117` background, vibrant `#6C5CE7` purple accent, `#FF7675` intervention warning).

2. **Single Source of Truth (`PreferencesRepository`)**:
   - Centralized `SharedPreferences` management with reactive state updates.
   - Clean separation of concern across domain models (`BuddyMood`, `AppUsageInfo`, `RoastMeme`).

3. **Android 14 Foreground Service Compliance**:
   - Explicit declaration of `android:foregroundServiceType="specialUse"` with the mandatory `PROPERTY_SPECIAL_USE_FGS_SUBTYPE` metadata in `AndroidManifest.xml`.
   - Prevents OS background execution crashes on Android 14 (API 34).

4. **Security & Sandbox Isolation**:
   - All internal activities and services marked `android:exported="false"` unless explicitly required (e.g., `MainActivity` launcher and `ReelsAccessibilityService` with system permission checks).

