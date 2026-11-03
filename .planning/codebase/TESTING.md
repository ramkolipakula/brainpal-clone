---
last_mapped_commit: 5d0167a094dcd794743bbddfda78d984b50cf3d5
last_mapped_at: 2026-11-03
---
# Testing Patterns

**Analysis Date:** 2026-11-03

## Test Framework

**Runner:**
- JUnit 4 (`junit:junit:4.13.2`)
- Kotlinx Coroutines Test (`org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.0`)
- Config: Configured in `source/app/build.gradle.kts` under `testOptions` and `dependencies`

**Assertion Library:**
- Standard JUnit 4 assertions (`org.junit.Assert.*`: `assertEquals`, `assertTrue`, `assertFalse`, `assertNotNull`)

**Run Commands:**
```bash
# Run all JVM unit tests (from source/ directory)
./gradlew testDebugUnitTest

# Run tests and generate HTML reports
./gradlew test

# Run tests via root PowerShell automation script
powershell -ExecutionPolicy Bypass -File tests/run_tests_and_build.ps1
```

## Test File Organization

**Location:**
- Unit tests are located in `source/app/src/test/java/com/brainrot/rebuilt/`
- Device verification scripts live in `tests/`
- Generated reports and screenshots live in `tests/html-report/`, `tests/*.xml`, and `reports/`

**Naming:**
- Unit test classes end with `Test` or `UnitTest`:
  - `source/app/src/test/java/com/brainrot/rebuilt/LogicUnitTest.kt`
  - `source/app/src/test/java/com/brainrot/rebuilt/Phase10ReelDetectionAndStateTest.kt`

**Structure:**
```
source/app/src/test/java/com/brainrot/rebuilt/
├── LogicUnitTest.kt                      # Tests moods, snooze timeouts, pass replenishment
└── Phase10ReelDetectionAndStateTest.kt   # Tests ReelDetectionEngine & MascotStageCalculator
```

## Test Structure

**Suite Organization:**
```kotlin
package com.brainrot.rebuilt

import com.brainrot.rebuilt.domain.DetectionResult
import com.brainrot.rebuilt.domain.MascotStageCalculator
import com.brainrot.rebuilt.domain.ReelDetectionEngine
import com.brainrot.rebuilt.domain.ScrollEvaluationContext
import com.brainrot.rebuilt.domain.model.MascotStage
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class Phase10ReelDetectionAndStateTest {

    private lateinit var engine: ReelDetectionEngine

    @Before
    fun setUp() {
        engine = ReelDetectionEngine(debounceWindowMs = 800L)
    }

    @Test
    fun testYouTubeShortsVerticalSwipe_incrementsOnce() {
        val context = ScrollEvaluationContext(
            eventType = AccessibilityEvent.TYPE_VIEW_SCROLLED,
            packageName = ReelDetectionEngine.PKG_YOUTUBE,
            scrollDeltaX = 0,
            scrollDeltaY = 600,
            className = "androidx.viewpager2.widget.ViewPager2",
            windowHierarchyKeywords = setOf("Shorts", "Remix", "Sound"),
            eventTimestamp = 1000L
        )

        val result = engine.evaluateEvent(context)
        assertEquals(DetectionResult.CONFIRMED_REEL_INCREMENT, result)
    }
}
```

**Patterns:**
- `@Before` method initializes a clean instance of the subject under test with deterministic timing/debouncing.
- Tests use explicit context objects (`ScrollEvaluationContext`) modeling exact real-world scenarios.
- Strict equality assertions on strongly-typed result enums (`DetectionResult.CONFIRMED_REEL_INCREMENT`, `DetectionResult.REJECT_DEDUPLICATE_BURST`).

## Mocking

**Framework:** None. Pure domain design avoids heavyweight mock frameworks (Mockito, MockK) in favor of mock-free state evaluation.

**Patterns:**
- Pure domain models and decoupled context evaluation objects pass parameters directly to pure engines (`ReelDetectionEngine`, `MascotStageCalculator`).
- Time-based tests provide explicit `eventTimestamp` or delta values rather than relying on system clock.

**What to Mock / Avoid:**
- Do NOT mock Android OS services in unit tests — decouple logic into pure Kotlin domain classes that take simple data holders (`ScrollEvaluationContext`), leaving Android platform binding to thin service wrappers.

## Fixtures and Factories

**Test Data Patterns:**
```kotlin
val baseContext = ScrollEvaluationContext(
    eventType = AccessibilityEvent.TYPE_VIEW_SCROLLED,
    packageName = ReelDetectionEngine.PKG_YOUTUBE,
    scrollDeltaX = 0,
    scrollDeltaY = 450,
    className = "androidx.viewpager2.widget.ViewPager2",
    windowHierarchyKeywords = setOf("Shorts"),
    eventTimestamp = 1000L
)
```

**Location:**
- Test contexts and mock payloads are constructed inline inside test methods or private helper functions within test classes.

## Coverage

**Requirements:**
- All critical business logic, calculations, event filtering rules, and state transitions must have unit test coverage.
- Current status: 21/21 passing tests (100% pass rate).

**View Coverage & Test Results:**
```bash
# View test summary XML
cat tests/TEST-com.brainrot.rebuilt.LogicUnitTest.xml

# View HTML report in browser
start tests/html-report/index.html
```

## Test Types

**Unit Tests (JVM):**
- Fast, hermetic tests running without Android emulator or physical device.
- Covers:
  - `BuddyMood` thresholds (0.35, 0.75, 1.0 limit ratios)
  - Emergency pass deduction & replenish logic
  - Snooze logic and expiration math
  - 5-stage mascot degeneration boundaries (`FRESH_HEALTHY` to `COMPLETELY_FRIED`)
  - Swipe directionality (horizontal rejection, vertical confirmation)
  - Debounce burst deduplication (rapid multi-scroll events counted once)
  - Negative context rejection (Instagram comments, DM threads, profile feeds)

**Device Integration & Automation Tests (ADB):**
- PowerShell automated tests verifying deployment on real Android devices:
  - `tests/check_bubble.ps1` - Verifies floating window presence via `dumpsys window`
  - `tests/check_running.ps1` - Verifies foreground service persistence via `dumpsys activity services`
  - `tests/verify_apk.ps1` - Verifies package installation and signature integrity

---

*Testing analysis: 2026-11-03*
