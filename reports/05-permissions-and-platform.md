# 05 - Permissions and Platform Integration: BrainPal

## 1. Declared Permissions Analysis

| Permission | Protection Level | Purpose & Necessity in BrainPal | Handled in Rebuilt App |
|---|---|---|---|
| `android.permission.PACKAGE_USAGE_STATS` | AppOps / Special Access | Query foreground package activity & detect when user opens Instagram/YouTube/Snapchat. | **YES**: Checked via `AppOpsManager`, launches `Settings.ACTION_USAGE_ACCESS_SETTINGS` when missing. |
| `android.permission.SYSTEM_ALERT_WINDOW` | Special / System Alert | Display the floating reel counter pill and full-screen roast overlay across other apps. | **YES**: Checked via `Settings.canDrawOverlays(context)`, deep-links to overlay permission screen. |
| `android.permission.BIND_ACCESSIBILITY_SERVICE` | Signature / System | Detects reel swiping gestures within short video feeds in real time. | **YES**: `ReelsAccessibilityService` declared with accessibility XML config. |
| `android.permission.POST_NOTIFICATIONS` | Runtime (API 33+) | Sends daily recap notification and pace nudges. | **YES**: Requested via standard runtime permission dialog on Android 13+. |
| `android.permission.RECEIVE_BOOT_COMPLETED` | Normal | Restores daily tracking alarm and resets daily quotas at midnight. | **YES**: Registered `DateChangedReceiver` and `TierBootReceiver`. |
| `android.permission.VIBRATE` | Normal | Haptic feedback for pace nudges when scrolling excessively fast. | **YES**: Used in `PaceNudgeWorker` with `Vibrator` service. |
| `android.permission.FOREGROUND_SERVICE` | Normal (API 28+) | Keeps the floating counter bubble active while in foreground use. | **YES**: Foreground service type `specialUse` declared. |
| `android.permission.INTERNET` | Normal | Loads meme roasts and remote GIF assets from CDN. | **YES**: Standard network access. |
| `android.permission.ACCESS_NETWORK_STATE` | Normal | Detects network availability before fetching assets. | **YES**: Used with `ConnectivityManager`. |
| `android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` | Special | Prevents aggressive OEM task killers from killing the reel counter. | **YES**: Audio/video guides provided for Samsung, Xiaomi, Motorola. |
| `com.android.vending.BILLING` | Normal | In-app billing (original commercial app). | **OMITTED / OUT OF SCOPE**: Clean-room rebuild is free & self-contained. |

## 2. Platform Integrations

### A. Usage Stats Polling vs Accessibility Service
- **Primary Mechanism**: `UsageStatsManager.queryUsageStats()` or `UsageEvents` provides robust, non-intrusive monitoring of app launch and foreground duration.
- **Secondary Mechanism**: `ReelsAccessibilityService` inspects UI hierarchy node transitions (`AccessibilityNodeInfo`) to count short-video scroll actions.
- **Graceful Fallback**: If Accessibility Service is disabled by the user, the app automatically falls back to `UsageStatsManager` foreground time tracking without crashing (`FallbackReelsCounterService`).

### B. Floating Overlay (`SYSTEM_ALERT_WINDOW`)
- Custom view attached via `WindowManager.addView(view, params)` using `TYPE_APPLICATION_OVERLAY`.
- Touch listener manages drag and repositioning on the phone display.
- Graceful lifecycle handles device orientation changes and screen locks.

