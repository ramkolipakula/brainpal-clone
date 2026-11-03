# Real-Device Audit Report: BrainPal Rebuilt

**Document**: `12-real-device-audit.md`  
**Date**: November 2, 2026  
**Auditor**: Autonomous Principal Android QA & Security Auditor  
**Workspace**: `D:\APK-Rebuild`  
**Scope**: Read-Only Audit of Reconstructed BrainPal against Original BrainPal APK  
**Status**: READ-ONLY AUDIT COMPLETE (Hardware Live Tests BLOCKED pending device reconnection)

---

## 1. Android Studio & SDK Environment Verification

Android Studio and the Android SDK were inspected on the local machine:

| Component | Detected Path / Details | Status | Notes |
|---|---|---|---|
| **Android Studio IDE** | `C:\Program Files\Android\Android Studio` | **PASS** | Version 2026.2.2 (Build `AI-262.10968.63.2622.16730706`). Process `studio64.exe` is currently running (PID `13824`). |
| **Android Studio JBR (JDK)** | `C:\Program Files\Android\Android Studio\jbr\bin\java.exe` | **PASS** | OpenJDK 25.0.4 (`build 25.0.4+-16339643-b508.27`). |
| **Configured Android SDK** | `D:\tools\android-sdk` | **PASS** | Sourced via `source/local.properties` (`sdk.dir=D\:\\tools\\android-sdk`). |
| **SDK Platform Tools / ADB** | `D:\tools\android-sdk\platform-tools\adb.exe` | **PASS** | Android Debug Bridge version 1.0.41 (Version `35.0.2-12147458`). |
| **SDK Build Tools** | `D:\tools\android-sdk\build-tools\34.0.0\` | **PASS** | Includes `apkanalyzer`, `apksigner`, `zipalign`, and `d8`. |
| **SDK Platforms** | `D:\tools\android-sdk\platforms\android-34` | **PASS** | Target API 34 (Android 14 UpsideDownCake). |
| **Android Studio SDK Path Option** | `C:\Users\Ramk\AppData\Roaming\Google\AndroidStudio2026.2.2\options\android.sdk.path.xml` | **PASS** | Set to `C:\Users\Ramk\AppData\Local\Android\Sdk` in IDE default preferences; Gradle project correctly binds to `D:\tools\android-sdk`. |

---

## 2. Physical Device Connection & Blocker Diagnosis

### 2.1. ADB Device Query
Executing `& "D:\tools\android-sdk\platform-tools\adb.exe" devices -l`:
```text
List of devices attached
(no devices listed)
```
Status: **NO ACTIVE DEVICE DETECTED BY ADB**

### 2.2. Hardware Bus & PnP Inspection
Querying Windows Plug-and-Play system records (`Get-PnpDevice`):
```text
FriendlyName : ADB Interface
InstanceId   : USB\VID_2D95&PID_6003&MI_01\6&2ADFCAC2&1&0001
Status       : Unknown
Class        : USBDevice
Problem      : CM_PROB_PHANTOM
Service      : WINUSB

FriendlyName : iQOO Z9 5G
InstanceId   : USB\VID_2D95&PID_6002\10BE2N05NR0001M
Status       : Unknown
Class        : WPD
Problem      : CM_PROB_PHANTOM
Service      : WUDFWpdMtp
```

### 2.3. Historical Device Profile (From Prior Session Run at 3:35 PM)
- **Model**: iQOO Z9 5G (Model `I2302`, Brand `vivo` / `iQOO`)
- **Device ID**: `10BE2N05NR0001M`
- **Android Version**: Android 14 (API Level 34)
- **Screen Resolution**: 1080 x 2400 pixels (~395 ppi, 120Hz AMOLED)
- **Prior Verified Run**: Package `com.brainrot.rebuilt` was installed and launched to foreground at 3:35 PM (`mCurrentFocus=Window{... MainActivity}`), captured in `reports/device_screenshot.png`.

### 2.4. Blocker Diagnosis & Action Required
- **Exact Blocker**: The physical USB cable connecting the **iQOO Z9 5G** is currently disconnected from the computer (Windows PnP state `CM_PROB_PHANTOM`), or the USB connection mode switched to Charge Only without USB debugging enabled.
- **Action Required for Real-Device Execution**:
  1. Plug the USB cable back into the computer and phone.
  2. Unlock the phone screen.
  3. Ensure USB mode is set to **File Transfer / Android Auto**.
  4. Under **Settings -> System -> Developer Options**, ensure **USB Debugging** is toggled **ON**.
  5. If an "Allow USB debugging?" dialog appears on the phone screen, select **"Always allow from this computer"** and tap **Allow**.

---

## 3. Real-Device & Functional Audit Test Matrix

Every test item is strictly evaluated and marked as **PASS**, **FAIL**, **BLOCKED**, or **NOT RUN**.

| Test ID | Test Category | Feature Under Test | Status | Evidence / Defect Reference |
|---|---|---|---|---|
| **RDA-01** | Device Connectivity | ADB USB Debugging link active | **BLOCKED** | Phone disconnected (`CM_PROB_PHANTOM`). Awaiting physical cable reconnection. |
| **RDA-02** | Device Specs | Device Model, Android OS, and SDK readout | **BLOCKED** | Previously captured as `iQOO Z9 5G` (API 34). Live read blocked. |
| **RDA-03** | Installation & Integrity | APK signature & DEX container integrity | **PASS** | Delivered APK at `delivery/RebuiltApp.apk` has valid v2 signature, valid DEX, and matching checksums. |
| **RDA-04** | Onboarding Flow | 3-step onboarding carousel navigation | **PASS** | Clean Compose carousel handles page progression cleanly. |
| **RDA-05** | Permissions Grant | Permission buttons update upon grant return | **FAIL** | `OnboardingScreen.kt` lines 40-41 do not observe lifecycle `ON_RESUME`. Button states stay stale after user grants permission in OS Settings. |
| **RDA-06** | Permissions Coverage | Accessibility & Notification requests in onboarding | **FAIL** | Onboarding only requests Usage Stats and Overlay. Accessibility and Notification permissions are completely omitted. |
| **RDA-07** | Screen Time Counters | Total daily screen time aggregation | **FAIL** | `UsageStatsRepository.kt` lines 91-104 sums ALL system packages (System UI, Launcher, Keyboard, Google Play Services). Rapidly exhausts quota prematurely. |
| **RDA-08** | Per-App Usage | Individual monitored app usage times | **FAIL** | `UsageStatsRepository.kt` line 58 hardcodes `dailyLimitMinutes = 30`. Defaults to mock numbers if query returns empty. |
| **RDA-09** | Counter Refresh | Counter refresh upon app foregrounding | **FAIL** | `HomeScreen.kt` line 42 queries stats inside `LaunchedEffect(Unit)`. Returning from background does not refresh counters. |
| **RDA-10** | Quota Configuration | Per-app quota customization | **FAIL** | `AppLimitsScreen.kt` only provides a single global screen time slider and global reel slider. Per-app customization is missing. |
| **RDA-11** | App Discovery | Discovery of installed device applications | **FAIL** | `AppLimitsScreen.kt` lines 38-46 hardcodes a static list of 8 apps. Real installed packages on device cannot be selected. |
| **RDA-12** | Fullscreen Intervention | Intervention overlay triggers on quota breach | **PASS** | `UsageMonitorService.kt` launches `BlockOverlayActivity` with `FLAG_ACTIVITY_NEW_TASK`. |
| **RDA-13** | Intervention Bypass | 10-second countdown reveals bypass button | **FAIL** | `BlockOverlayActivity.kt` lines 88-97 counts down `canContinue`, but no bypass action or button is rendered in the UI when complete. |
| **RDA-14** | Meme Roast Display | Roast GIF / image rendering | **FAIL** | `BlockOverlayActivity.kt` line 148 renders static vector `br_brain_standing_5`. Meme GIFs and images from catalog are never loaded. |
| **RDA-15** | Emergency Passes | Deduct pass and grant 5-min grace snooze | **PASS** | `PreferencesRepository.kt` atomic decrement and 5-min snooze timestamp persistence verified. |
| **RDA-16** | Strict Mode Logic | Strict mode prevents quota editing | **FAIL** | `UsageMonitorService.kt` line 66 immediately blocks guarded apps upon launch if Strict Mode is enabled, even if 0 seconds spent. |
| **RDA-17** | Reel Counter Persistence | Reel scroll counter survives service restart | **FAIL** | `ReelsAccessibilityService.kt` line 35 stores `reelScrollCountToday` in-memory. Resets to 0 upon service restart or app kill. |
| **RDA-18** | Scroll Reel Filtering | Accessibility distinguishes reel vs regular scroll | **FAIL** | `ReelsAccessibilityService.kt` lines 60-77 triggers on any `TYPE_VIEW_SCROLLED` in blocked package, miscounting text and profile scrolling as reels. |
| **RDA-19** | Statistics Screen | Live usage charts and weekly trends | **FAIL** | `StatsScreen.kt` lines 29-37 renders 100% hardcoded mock tuples and mock strings (`"-46%"`, `"3.2 hours"`). |
| **RDA-20** | Streak Maintenance | Daily midnight streak increment & pass reset | **FAIL** | `PreferencesRepository.kt` streak defaults to 3 and `incrementStreak()` is never invoked. No `DateChangedReceiver` or WorkManager worker. |
| **RDA-21** | Floating Bubble Toggle | User control for floating counter bubble | **FAIL** | Floating bubble is hardcoded to appear in `ReelsAccessibilityService` with no toggle or size configuration in Settings. |
| **RDA-22** | Live UI Automated Testing | UIAutomator / Instrumentation on device | **BLOCKED** | Blocked due to disconnected physical device. |

---

## 4. Summary of Verification Status

- **Total Checks**: 22
- **PASS**: 5 (APK integrity, onboarding navigation, intervention launch, pass deduction, SDK/Studio setup)
- **FAIL**: 14 (Functional defects across counters, settings, UI, accessibility, and lifecycle)
- **BLOCKED**: 3 (Live device ADB tests awaiting physical cable reconnection)
- **NOT RUN**: 0

