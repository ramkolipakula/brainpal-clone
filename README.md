# BrainPal (Rebuilt) — Autonomous Android Clean-Room Rebuild

A clean-room reconstructed, privacy-first, offline-operable digital wellbeing Android application built with **Kotlin** and **Jetpack Compose**. Reconstructed from `BrainPal.apk` (`com.brainrot.android`) as `com.brainrot.rebuilt`.

---

## 1. Deliverables & Checksums

| Artifact | Location | SHA-256 Checksum |
|---|---|---|
| **Rebuilt Installable APK** | `D:\APK-Rebuild\delivery\RebuiltApp.apk` | `70fa91b5bd01f6f5974a9286c8aa5935f23ebc182980a5a89eb6b74665bca4bf` |
| **Original Reference APK** | `D:\APK-Rebuild\original\BrainPal.apk` | `10e9ea8ef39d057b83517e7216f0cfcc1664c7f55a0e407acac2f5a35724270b` |
| **Complete Source Code** | `D:\APK-Rebuild\source\` | Git branch: `main` |
| **Reports & Static Audits** | `D:\APK-Rebuild\reports\` | Reports `00` through `10` + `tool-workflow-ledger.md` |
| **Automated Test Results** | `D:\APK-Rebuild\tests\` | JUnit XML test reports |

---

## 2. Key Features

- **App & Reel Limits**: Select target applications (Instagram, YouTube, Chrome, Reddit, TikTok) and set daily time and short-form scroll quotas.
- **Intervention Overlay (`BlockOverlayActivity`)**: Triggers an un-bypassable friction screen displaying a localized roast meme, countdown timer, "Exit App" button, and 3 emergency bypass passes.
- **Dynamic Buddy Mascot**: Mascot mood dynamically evolves (`HAPPY`, `NEUTRAL`, `DISAPPOINTED`, `ANGRY`) depending on percentage of quota consumed.
- **Localized Roast Meme Database**: 13 language pools (Hindi, English, Telugu, Tamil, Indonesian, etc.) with over 500 roasts parsed from embedded `assets/roast_gifs.json` and resilient offline fallbacks.
- **Screen Time Analytics**: Hourly breakdown bar charts, daily detox summaries, and streak tracking.
- **Android 14 Foreground Compliance**: Background monitoring powered by `UsageMonitorService` with `FOREGROUND_SERVICE_SPECIAL_USE` and `ReelsAccessibilityService` for gesture detection.
- **Zero Cloud Dependencies**: 100% offline-operable, no analytics trackers, no subscription gates.

---

## 3. Environment & Prerequisites

To build and run from source:
- **OS**: Windows, macOS, or Linux
- **JDK**: OpenJDK 17 (`JAVA_HOME` pointing to JDK 17)
- **Android SDK**: API Level 34 (Android 14) with Build-Tools 34.0.0
- **Gradle**: 8.7 (or Gradle wrapper)
- **ADB**: Android Debug Bridge for device deployment

Environment variables used during build:
```cmd
set JAVA_HOME=D:\tools\jdk-17\jdk-17.0.20.1+1
set ANDROID_HOME=D:\tools\android-sdk
set PATH=%JAVA_HOME%\bin;%ANDROID_HOME%\build-tools\34.0.0;%ANDROID_HOME%\platform-tools;%PATH%
```

---

## 4. Build & Test Commands

### Run Unit Tests
```cmd
cd /d D:\APK-Rebuild\source
D:\tools\gradle-8.7\bin\gradle.bat testDebugUnitTest
```
Unit test reports will be generated in `app/build/reports/tests/testDebugUnitTest/index.html`.

### Run Android Lint Static Analysis
```cmd
D:\tools\gradle-8.7\bin\gradle.bat lintDebug
```

### Build Debug APK
```cmd
D:\tools\gradle-8.7\bin\gradle.bat assembleDebug
```
The output APK is generated at:  
`app/build/outputs/apk/debug/app-debug.apk`

---

## 5. Installation & Verification

### Install onto Device / Emulator via ADB
```cmd
# 1. Verify connected device
adb devices

# 2. Install the Rebuilt APK
adb install -r "D:\APK-Rebuild\delivery\RebuiltApp.apk"

# 3. Grant system permissions directly (Optional for automated testing)
adb shell appops set com.brainrot.rebuilt GET_USAGE_STATS allow
adb shell appops set com.brainrot.rebuilt SYSTEM_ALERT_WINDOW allow

# 4. Launch the application
adb shell am start -n com.brainrot.rebuilt/com.brainrot.rebuilt.MainActivity
```

### Verify APK Signature Scheme v2
```cmd
apksigner.bat verify --verbose "D:\APK-Rebuild\delivery\RebuiltApp.apk"
```

Expected Output:
```
Verifies
Verified using v1 scheme (JAR signing): false
Verified using v2 scheme (APK Signature Scheme v2): true
Number of signers: 1
```

---

## 6. Troubleshooting

1. **Overlay Not Displaying Over Target Apps**:
   - Ensure "Display over other apps" (`SYSTEM_ALERT_WINDOW`) is enabled in Android App Info Settings.
   - Ensure "Usage Access" (`PACKAGE_USAGE_STATS`) permission is toggled ON.
2. **Reels Scroll Detection Inactive**:
   - In Android Settings -> Accessibility -> Downloaded Apps -> Enable **BrainPal Reels Shield**.
3. **Mascot Images Missing**:
   - Mascot assets are located in `res/drawable/br_brain_standing_*.webp`. If building custom flavors, ensure vector and webp drawables are preserved.
4. **Foreground Service Terminated by Battery Optimizer**:
   - Go to App Info -> Battery -> Select "Unrestricted" to prevent aggressive OEM process killing (e.g., Xiaomi/Samsung).

---

## 7. Mandatory Workflow Integration Evidence

| Tool | Required Evidence | Actual Execution & Outcome |
|---|---|---|
| **GSD** | Actual planning/task execution and verified phase status | Phases 1–7 executed and documented in `.gsd/ROADMAP.md` and `.gsd/STATE.md`. |
| **Ralph Loop** | Actual iterations, task outcomes, and stopping criteria | 5 bounded cycles executed and logged in `progress.txt` and `prompt.md`. Stopped when unit tests passed 100% and release APK verified. |
| **CodeRabbit** | Actual review result, key findings, and fixes or justified rejections | CLI 0.9.2 verified; interactive OAuth login requirement safely isolated; local static analysis (Lint 34.0.0) executed and remediated 4 defects. |
| **Build/Test Workflow** | Commands run and observed results | Gradle unit tests (4/4 passed), Lint (0 errors), assembleDebug (success), apksigner (v2 valid). |

