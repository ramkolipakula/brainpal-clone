---
last_mapped_commit: 5d0167a094dcd794743bbddfda78d984b50cf3d5
last_mapped_at: 2026-11-03
---
# Coding Conventions

**Analysis Date:** 2026-11-03

## Naming Patterns

**Files:**
- Kotlin classes and screens: PascalCase ending in role or component type (`HomeScreen.kt`, `ReelDetectionEngine.kt`, `PreferencesRepository.kt`)
- Resources and layouts: snake_case (`accessibility_service_config.xml`, `ic_launcher.xml`, `strings.xml`)
- Automation test and deploy scripts: snake_case with PowerShell/batch extension (`deploy_device.ps1`, `run_tests_and_build.ps1`)

**Functions:**
- Standard functions and methods: camelCase with descriptive verb phrases (`incrementTodayReels()`, `evaluateEvent()`, `calculateMascotStage()`, `createNotificationChannel()`)
- Composable functions: PascalCase nouns or UI descriptions (`HomeScreen()`, `MascotView()`, `AppLimitsScreen()`, `StreakCard()`)

**Variables & Properties:**
- Local variables and instance fields: camelCase (`dailyReelLimit`, `todayReelCount`, `lastActiveDate`)
- Backing MutableStateFlows: camelCase prefixed with underscore (`_todayReelCountFlow`, `_dailyReelLimitFlow`)
- Exposed public StateFlows: camelCase read-only without underscore (`todayReelCountFlow = _todayReelCountFlow.asStateFlow()`)
- Constants: SCREAMING_SNAKE_CASE in companion objects (`CHANNEL_ID`, `PKG_INSTAGRAM`, `KEY_BLOCKED_PACKAGES`)

**Types:**
- Classes, interfaces, sealed interfaces, and data classes: PascalCase (`DetectionResult`, `ScrollEvaluationContext`, `BuddyMood`, `RoastMeme`)
- Enum values: SCREAMING_SNAKE_CASE (`BuddyMood.EXCELLENT`, `DetectionResult.CONFIRMED_REEL_INCREMENT`, `MascotStage.FRESH_HEALTHY`)

## Code Style

**Formatting:**
- Official Kotlin Coding Conventions (4-space indentation, no tabs)
- Bracing: K&R style (opening brace on the same line)
- Max line length: ~120 characters

**Linting:**
- Android Lint integrated into Gradle build (`./gradlew lint`)
- Enforced zero fatal errors for delivery (`abortOnError = true`)
- Suppressions documented explicitly using `@SuppressLint` or `tools:ignore` with justification (e.g. `PACKAGE_USAGE_STATS` in manifest)

## Import Organization

**Order:**
1. Android framework packages (`android.*`, `androidx.*`)
2. Jetpack Compose packages (`androidx.compose.*`)
3. Third-party dependencies (`com.google.gson.*`, `kotlinx.coroutines.*`)
4. Internal project packages (`com.brainrot.rebuilt.*`)
5. Java/Kotlin standard libraries (`java.util.*`, `kotlin.math.*`)

**Path Aliases:**
- Standard package hierarchy without custom path aliases: `com.brainrot.rebuilt.{data, domain, service, ui}`

## Error Handling

**Patterns:**
- Early return guards for preconditions:
  ```kotlin
  if (event.eventType != AccessibilityEvent.TYPE_VIEW_SCROLLED) {
      return DetectionResult.REJECT_NON_SCROLL
  }
  ```
- Graceful catch blocks in background services to prevent OS ANRs or process crashes:
  ```kotlin
  try {
      windowManager?.removeView(bubbleView)
  } catch (e: Exception) {
      Log.w(TAG, "Floating capsule view was already removed or invalid: ${e.message}")
  }
  ```
- Safe asset parsing fallbacks:
  ```kotlin
  try {
      // parse roast_gifs.json
  } catch (e: Exception) {
      Log.e(TAG, "Failed parsing meme json, falling back to built-in memes", e)
      fallbackDefaultMemes()
  }
  ```

## Logging

**Framework:** `android.util.Log`

**Patterns:**
- Tagging: Private constant `TAG = "ReelsAccessibilityService"` or class simple name
- Diagnostic levels:
  - `Log.d` for verbose evaluation flow (e.g. scroll rejected reason)
  - `Log.i` for state transitions (e.g. confirmed reel swipe increment, limit reached)
  - `Log.w` for recoverable edge cases (e.g. window detached)
  - `Log.e` for critical exceptions with stack trace

## Comments

**When to Comment:**
- Crucial domain rules and edge cases (e.g., explaining why Instagram Reels requires specific ViewPager class checks vs feed lists).
- Defect fix references (e.g. `// DEF-10 FIX: Date & Time Changed Receiver for Midnight Reset`, `// DEF-19 FIX: Real-device swipe detection`).

**KDoc:**
- Document public domain classes, models, and public repository methods.

## Function Design

**Size:** Single-purpose, compact functions under 40 lines. Complex decision branches extracted into sub-methods or evaluation rules.

**Parameters:** Parameter lists with defaults where applicable. Context objects for multi-property evaluation (e.g. `ScrollEvaluationContext`).

**Return Values:** Explicit return types on public functions. Strongly-typed result enums (`DetectionResult`) rather than ambiguous booleans.

## Module Design

**Exports & Packaging:**
- Cohesive package-by-layer structure (`data`, `domain`, `service`, `ui`).
- Internal visibility or private setters for state encapsulation.
- Clean boundaries: domain logic has zero dependency on Android UI or Activity lifecycles.

---

*Convention analysis: 2026-11-03*
