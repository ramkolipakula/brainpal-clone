---
last_mapped_commit: 5d0167a094dcd794743bbddfda78d984b50cf3d5
last_mapped_at: 2026-11-03
---
<!-- refreshed: 2026-11-03 -->
# Architecture

**Analysis Date:** 2026-11-03

## System Overview

```text
┌─────────────────────────────────────────────────────────────────────────────┐
│                           UI Layer (Jetpack Compose)                        │
├──────────────────┬──────────────────┬───────────────────────┬───────────────┤
│   HomeScreen     │ AppLimitsScreen  │      StatsScreen      │ SettingsScreen│
│  `ui/screens/`   │  `ui/screens/`   │     `ui/screens/`     │ `ui/screens/` │
├──────────────────┴──────────────────┴───────────────────────┴───────────────┤
│               MainActivity & BlockOverlayActivity (`com.brainrot.rebuilt`)   │
└────────┬────────────────────────────┬───────────────────────────────┬───────┘
         │                            │                               │
         ▼                            ▼                               ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                    Service & Accessibility Layer                            │
│  - UsageMonitorService (`service/UsageMonitorService.kt`)                   │
│  - ReelsAccessibilityService & Floating Capsule Window (`service/`)        │
│  - BootReceiver & DateChangedReceiver (`service/`)                          │
└────────┬────────────────────────────┬───────────────────────────────────────┘
         │                            │
         ▼                            ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                       Domain Logic & Evaluation Engines                     │
│  - ReelDetectionEngine (`domain/ReelDetectionEngine.kt`)                    │
│  - MascotStageCalculator (`domain/MascotStageCalculator.kt`)                │
│  - Data Models (`domain/model/Models.kt`)                                   │
└────────┬────────────────────────────┬───────────────────────────────────────┘
         │                            │
         ▼                            ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                        Data Layer & Repositories                            │
│  - PreferencesRepository (`data/PreferencesRepository.kt`)                  │
│    (Authoritative StateFlows: todayReelCountFlow, dailyLimits)              │
│  - UsageStatsRepository (`data/UsageStatsRepository.kt`)                    │
│  - RoastMemeRepository (`data/RoastMemeRepository.kt`)                      │
└─────────────────────────────────────┬───────────────────────────────────────┘
                                      │
                                      ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                        Android OS Platform APIs                             │
│  - SharedPreferences (`brainpal_prefs`)                                      │
│  - UsageStatsManager & WindowManager                                        │
│  - Assets (`assets/roast_gifs.json`)                                        │
└─────────────────────────────────────────────────────────────────────────────┘
```

## Component Responsibilities

| Component | Responsibility | File |
|-----------|----------------|------|
| `BrainPalApp` | Application singleton, initializes repositories and creates notification channels | `source/app/src/main/java/com/brainrot/rebuilt/BrainPalApp.kt` |
| `MainActivity` | Hosts Compose NavHost, permission gates, and bottom navigation bar | `source/app/src/main/java/com/brainrot/rebuilt/MainActivity.kt` |
| `BlockOverlayActivity` | Fullscreen intervention dialogue displaying roast memes when limit is reached | `source/app/src/main/java/com/brainrot/rebuilt/BlockOverlayActivity.kt` |
| `ReelsAccessibilityService` | Listens to view scroll events, hosts draggable floating reel capsule window | `source/app/src/main/java/com/brainrot/rebuilt/service/ReelsAccessibilityService.kt` |
| `UsageMonitorService` | Foreground service polling `UsageStatsManager` to enforce app minute limits | `source/app/src/main/java/com/brainrot/rebuilt/service/UsageMonitorService.kt` |
| `ReelDetectionEngine` | Pure domain classifier distinguishing Reel/Short swipes from clicks and feed | `source/app/src/main/java/com/brainrot/rebuilt/domain/ReelDetectionEngine.kt` |
| `MascotStageCalculator` | Pure domain calculator computing 5 brain mascot degeneration stages | `source/app/src/main/java/com/brainrot/rebuilt/domain/MascotStageCalculator.kt` |
| `MascotView` | Compose component rendering animated brain mascot with pulse transitions | `source/app/src/main/java/com/brainrot/rebuilt/ui/components/MascotView.kt` |
| `PreferencesRepository` | Central reactive store driving `StateFlow` updates across app and services | `source/app/src/main/java/com/brainrot/rebuilt/data/PreferencesRepository.kt` |
| `UsageStatsRepository` | Queries Android OS `UsageStatsManager` for foreground app packages and times | `source/app/src/main/java/com/brainrot/rebuilt/data/UsageStatsRepository.kt` |
| `RoastMemeRepository` | Parses and selects localized roast memes and humor copy from asset JSON | `source/app/src/main/java/com/brainrot/rebuilt/data/RoastMemeRepository.kt` |
| `BootReceiver` | Restarts monitoring services on device boot or package replacement | `source/app/src/main/java/com/brainrot/rebuilt/service/BootReceiver.kt` |
| `DateChangedReceiver` | Listens for midnight date transitions to reset daily counters and streaks | `source/app/src/main/java/com/brainrot/rebuilt/service/DateChangedReceiver.kt` |

## Pattern Overview

**Overall:** Clean Architecture with MVVM and Reactive Unidirectional Data Flow (UDF) via Kotlin Coroutine `StateFlow`.

**Key Characteristics:**
- **Decoupled Detection Engine:** UI and Accessibility services delegate event analysis to testable pure domain classes (`ReelDetectionEngine`).
- **Single Source of Truth:** `PreferencesRepository` maintains the authoritative counter and limit states via reactive `StateFlow`s, preventing desynchronization between background services and foreground Compose screens.
- **Defensive Android Lifecycle Integration:** Overlay windows and services gracefully handle permission revocations, process termination, and configuration changes.

## Layers

**UI Layer (`ui/`):**
- Purpose: Present UI states, handle user actions, render screens using Jetpack Compose
- Location: `source/app/src/main/java/com/brainrot/rebuilt/ui/`
- Contains: Screens (`HomeScreen.kt`, `AppLimitsScreen.kt`, `StatsScreen.kt`, `SettingsScreen.kt`, `OnboardingScreen.kt`), components (`MascotView.kt`), themes (`Theme.kt`, `Color.kt`)
- Depends on: Domain models and Data repositories
- Used by: Android Activity components (`MainActivity.kt`, `BlockOverlayActivity.kt`)

**Service Layer (`service/`):**
- Purpose: Background monitoring, accessibility event intake, floating overlay management
- Location: `source/app/src/main/java/com/brainrot/rebuilt/service/`
- Contains: `UsageMonitorService.kt`, `ReelsAccessibilityService.kt`, `BootReceiver.kt`, `DateChangedReceiver.kt`
- Depends on: Domain detection engine and Data repositories
- Used by: Android OS framework

**Domain Layer (`domain/`):**
- Purpose: Core business rules, calculation algorithms, event classification
- Location: `source/app/src/main/java/com/brainrot/rebuilt/domain/`
- Contains: `ReelDetectionEngine.kt`, `MascotStageCalculator.kt`, `domain/model/Models.kt`
- Depends on: Pure Kotlin / Android Accessibility event data abstractions only
- Used by: Service layer and UI layer

**Data Layer (`data/`):**
- Purpose: Persistence, system usage stats retrieval, asset catalog queries
- Location: `source/app/src/main/java/com/brainrot/rebuilt/data/`
- Contains: `PreferencesRepository.kt`, `UsageStatsRepository.kt`, `RoastMemeRepository.kt`
- Depends on: Android framework `SharedPreferences`, `UsageStatsManager`, `AssetManager`
- Used by: Domain services and UI layer

## Data Flow

### Primary Reel Swipe Counter Flow

1. User swipes vertically inside Instagram Reels or YouTube Shorts.
2. `ReelsAccessibilityService.onAccessibilityEvent()` captures event (`service/ReelsAccessibilityService.kt:66`).
3. Event context is passed to `ReelDetectionEngine.evaluateEvent()` (`domain/ReelDetectionEngine.kt:65`).
4. On `CONFIRMED_REEL_INCREMENT`:
   - `PreferencesRepository.incrementTodayReels()` is called (`data/PreferencesRepository.kt:130`).
   - `_todayReelCountFlow.value` updates immediately.
5. Observers reactively update:
   - Floating counter capsule updates badge count and mascot icon on screen.
   - `HomeScreen` re-renders updated progress and mascot stage via `todayReelCountFlow.collectAsState()`.
   - If count exceeds `dailyReelLimit`, `BlockOverlayActivity` is launched or full-screen warning triggered.

### App Usage Monitoring & Intervention Flow

1. `UsageMonitorService` coroutine loop runs every 1,000ms (`service/UsageMonitorService.kt:70`).
2. `UsageStatsRepository.getForegroundPackage()` queries recent events.
3. If package belongs to `blockedPackages` and accumulated time exceeds limit:
   - If user is not currently snoozed (`PreferencesRepository.isSnoozed()`), launches `BlockOverlayActivity`.
4. `BlockOverlayActivity` loads localized roast meme from `RoastMemeRepository` and presents dismissal/snooze options.

**State Management:**
- Application state is synchronized across processes via `PreferencesRepository` backed by `SharedPreferences` and reactive in-memory `StateFlow` primitives.

## Key Abstractions

**`ReelDetectionEngine`:**
- Purpose: Evaluates Accessibility events against positive/negative keyword lists, scroll deltas, and debounce windows to classify whether an interaction was an actual reel switch.
- Examples: `source/app/src/main/java/com/brainrot/rebuilt/domain/ReelDetectionEngine.kt`
- Pattern: Strategy / Rule-Engine pattern

**`MascotStageCalculator`:**
- Purpose: Maps reel counts and usage duration against limits into 5 deterministic visual brain degeneration stages: `FRESH_HEALTHY`, `EARLY_FATIGUE`, `SLIGHTLY_COOKED`, `BRAINROT_WARNING`, `COMPLETELY_FRIED`.
- Examples: `source/app/src/main/java/com/brainrot/rebuilt/domain/MascotStageCalculator.kt`
- Pattern: State Machine / Value Object mapping

## Entry Points

**Application Bootstrap:**
- Location: `source/app/src/main/java/com/brainrot/rebuilt/BrainPalApp.kt`
- Triggers: Android OS process startup
- Responsibilities: Prepares repositories, registers notification channels.

**Main Dashboard UI:**
- Location: `source/app/src/main/java/com/brainrot/rebuilt/MainActivity.kt`
- Triggers: User launches application from launcher
- Responsibilities: Checks permissions, displays onboard flow if first run, routes between Home, Limits, Stats, and Settings.

**Reels Accessibility Hook:**
- Location: `source/app/src/main/java/com/brainrot/rebuilt/service/ReelsAccessibilityService.kt`
- Triggers: OS delivers window and view scroll events from target packages
- Responsibilities: Real-time swipe detection and overlay capsule management.

## Architectural Constraints

- **Threading:** Heavy queries (`UsageStatsManager`, `AssetManager` parsing) run on `Dispatchers.IO`. UI operations strictly bound to `Dispatchers.Main`.
- **Global state:** `PreferencesRepository` is the sole mutable state manager; UI components are stateless composables consuming state flows.
- **Circular dependencies:** None. Clean separation between UI, Service, Domain, and Data layers.

## Anti-Patterns

### Direct SharedPreferences Polling in UI Composables

**What happens:** UI reading directly from disk SharedPreferences on every recomposition.
**Why it's wrong:** Triggers disk I/O on the main thread and causes UI frame drops (jank).
**Do this instead:** Collect from `PreferencesRepository` reactive `StateFlow` using `collectAsState()` as in `source/app/src/main/java/com/brainrot/rebuilt/ui/screens/HomeScreen.kt`.

### Blind Accessibility Event Counting

**What happens:** Incrementing reel counts on every scroll event received from YouTube or Instagram.
**Why it's wrong:** Causes massive double-counting (bursts of 5–10 scroll events per finger swipe, horizontal carousel swipes, comment scrolling).
**Do this instead:** Pass events through `ReelDetectionEngine.evaluateEvent()` with delta checks, negative context checks, and 800ms debounce as in `source/app/src/main/java/com/brainrot/rebuilt/domain/ReelDetectionEngine.kt`.

## Error Handling

**Strategy:** Fail-safe non-crashing operations. In background services and accessibility listeners, exceptions are caught, logged, and safe fallback states are returned to guarantee the phone never experiences system freeze or UI crashes.

**Patterns:**
- Try-catch around WindowManager overlay attachment and detachment (`service/ReelsAccessibilityService.kt`).
- Fallback default meme objects if asset parsing encounters corrupted JSON (`data/RoastMemeRepository.kt`).
- Guard clauses against missing or revoked system permissions (`MainActivity.kt`).

## Cross-Cutting Concerns

**Logging:** Android `Logcat` via `android.util.Log` with class-scoped tags.
**Validation:** Domain boundary checks on limits, integer percentages, and valid package names.
**Permissions:** Explicit runtime verification and redirection intents for `PACKAGE_USAGE_STATS`, `SYSTEM_ALERT_WINDOW`, and `BIND_ACCESSIBILITY_SERVICE`.

---

*Architecture analysis: 2026-11-03*
