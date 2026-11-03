---
last_mapped_commit: 5d0167a094dcd794743bbddfda78d984b50cf3d5
last_mapped_at: 2026-11-03
---
# Codebase Structure

**Analysis Date:** 2026-11-03

## Directory Layout

```
d:\APK-Rebuild/
├── .agent/                   # Custom agent workflows and task definitions
├── .gsd/                     # GSD workflow state, roadmap, templates, and reference examples
│   ├── STATE.md              # Current project state and phase milestone tracking
│   ├── ROADMAP.md            # Multi-phase project roadmap
│   └── templates/            # Template files for GSD artifacts
├── .planning/                # Active planning repository
│   └── codebase/             # Codebase map documents (STACK, ARCHITECTURE, etc.)
├── delivery/                 # Final signed installable production build artifacts
│   └── RebuiltApp.apk        # Delivered rebuilt APK binary
├── original/                 # Original reverse-engineered APK inspection artifacts
│   ├── BrainPal.apk          # Reference input APK
│   ├── extracted/            # Decompiled assets, manifest, DEX files
│   └── *.py                  # Reversal inspection scripts
├── reports/                  # Detailed verification, defect registers, and milestone reports (00-18)
├── source/                   # Clean-room native Android Gradle project
│   ├── build.gradle.kts      # Root build configuration
│   ├── settings.gradle.kts   # Project and repository settings
│   └── app/                  # Android application module
│       ├── build.gradle.kts  # Dependencies, compileSdk, Jetpack Compose build settings
│       └── src/
│           ├── main/
│           │   ├── AndroidManifest.xml
│           │   ├── assets/   # Bundled assets (roast_gifs.json, tutorials)
│           │   ├── res/      # Android XML resources, icons, themes, strings
│           │   └── java/com/brainrot/rebuilt/
│           │       ├── BrainPalApp.kt
│           │       ├── MainActivity.kt
│           │       ├── BlockOverlayActivity.kt
│           │       ├── data/
│           │       ├── domain/
│           │       ├── service/
│           │       └── ui/
│           └── test/         # Unit and integration test suites
├── tests/                    # Automation test scripts, HTML/XML test reports
├── deploy_device.ps1         # PowerShell automated device deployment and permission grant script
├── install_on_mobile.ps1     # ADB installation and launch script
├── PRD.md                    # Product requirements document
└── README.md                 # Project overview and operational guide
```

## Directory Purposes

**`source/app/src/main/java/com/brainrot/rebuilt/`:**
- Purpose: Core application source code
- Contains: Application classes, activities, services, data repositories, domain engines, and Compose UI screens
- Key files: `BrainPalApp.kt`, `MainActivity.kt`, `BlockOverlayActivity.kt`

**`source/app/src/main/java/com/brainrot/rebuilt/data/`:**
- Purpose: Data persistence, local state management, and platform stat retrieval
- Contains: Repositories wrapping SharedPreferences, UsageStatsManager, and AssetManager
- Key files: `PreferencesRepository.kt`, `UsageStatsRepository.kt`, `RoastMemeRepository.kt`

**`source/app/src/main/java/com/brainrot/rebuilt/domain/`:**
- Purpose: Pure domain logic, event classification rules, and calculation engines
- Contains: Reel swipe detection algorithms, mascot stage calculators, and domain data models
- Key files: `ReelDetectionEngine.kt`, `MascotStageCalculator.kt`, `model/Models.kt`

**`source/app/src/main/java/com/brainrot/rebuilt/service/`:**
- Purpose: Android framework services and system broadcast receivers
- Contains: Foreground usage monitor service, accessibility reel detection service, boot/date change receivers
- Key files: `UsageMonitorService.kt`, `ReelsAccessibilityService.kt`, `BootReceiver.kt`, `DateChangedReceiver.kt`

**`source/app/src/main/java/com/brainrot/rebuilt/ui/`:**
- Purpose: Declarative Jetpack Compose UI
- Contains: Screen composables (`screens/`), reusable UI components (`components/`), and Material 3 theme styling (`theme/`)
- Key files: `screens/HomeScreen.kt`, `screens/AppLimitsScreen.kt`, `screens/StatsScreen.kt`, `screens/SettingsScreen.kt`, `components/MascotView.kt`

**`source/app/src/test/java/com/brainrot/rebuilt/`:**
- Purpose: Automated JVM unit test suites
- Contains: Pure unit tests for limits, streaks, mood calculation, and accessibility swipe detection
- Key files: `Phase10ReelDetectionAndStateTest.kt`, `LogicUnitTest.kt`

**`reports/`:**
- Purpose: Project phase verification, discovery matrices, defect registers, and audit logs
- Contains: 19 structured markdown reports (00 through 18) and device verification screenshots
- Key files: `18-phase10-remediation-report.md`, `15-complete-defect-register.md`

## Key File Locations

**Entry Points:**
- `source/app/src/main/java/com/brainrot/rebuilt/BrainPalApp.kt`: Application class and dependency provider
- `source/app/src/main/java/com/brainrot/rebuilt/MainActivity.kt`: Interactive launcher activity and Compose container
- `source/app/src/main/java/com/brainrot/rebuilt/service/ReelsAccessibilityService.kt`: Accessibility event intake and floating bubble host

**Configuration:**
- `source/app/build.gradle.kts`: Android dependencies, SDK versions, Compose compiler configuration
- `source/app/src/main/AndroidManifest.xml`: System permissions, service definitions, activity intent filters
- `source/app/src/main/res/values/strings.xml`: Localized UI strings and accessibility labels

**Core Logic:**
- `source/app/src/main/java/com/brainrot/rebuilt/domain/ReelDetectionEngine.kt`: Reel/Short vertical swipe classifier
- `source/app/src/main/java/com/brainrot/rebuilt/domain/MascotStageCalculator.kt`: Brain mascot degeneration stage resolver
- `source/app/src/main/java/com/brainrot/rebuilt/data/PreferencesRepository.kt`: Central reactive StateFlow state manager

**Testing:**
- `source/app/src/test/java/com/brainrot/rebuilt/Phase10ReelDetectionAndStateTest.kt`: Detection engine verification tests
- `source/app/src/test/java/com/brainrot/rebuilt/LogicUnitTest.kt`: Mood, snooze, streak, and pass unit tests

## Naming Conventions

**Files:**
- Kotlin Classes / Interfaces / Composables: PascalCase (`ReelDetectionEngine.kt`, `HomeScreen.kt`, `MascotView.kt`)
- Layout & Resource XML: snake_case (`accessibility_service_config.xml`, `strings.xml`, `colors.xml`)
- Automation Scripts: snake_case with shell extension (`deploy_device.ps1`, `install_on_mobile.bat`)

**Directories:**
- Package folders: lowercase dot-separated mapped to filesystem (`com/brainrot/rebuilt/domain/model`)
- Root workspace categories: lowercase (`source/`, `reports/`, `delivery/`, `tests/`)

## Where to Add New Code

**New Feature / Screen:**
- Primary composable: `source/app/src/main/java/com/brainrot/rebuilt/ui/screens/`
- Navigation destination: Register in `source/app/src/main/java/com/brainrot/rebuilt/MainActivity.kt` `NavHost`
- Tests: Add corresponding unit tests in `source/app/src/test/java/com/brainrot/rebuilt/`

**New Domain Logic or Rule:**
- Implementation: `source/app/src/main/java/com/brainrot/rebuilt/domain/`
- Data model additions: `source/app/src/main/java/com/brainrot/rebuilt/domain/model/Models.kt`
- Tests: Unit tests in `source/app/src/test/java/com/brainrot/rebuilt/`

**New State or Preference Key:**
- Persistence: Add getter/setter and `MutableStateFlow` in `source/app/src/main/java/com/brainrot/rebuilt/data/PreferencesRepository.kt`

**Reusable UI Components:**
- Shared components: `source/app/src/main/java/com/brainrot/rebuilt/ui/components/`

## Special Directories

**`delivery/`:**
- Purpose: Contains final compiled deliverable APK ready for installation on user devices
- Generated: Yes (via Gradle assembleRelease / assembleDebug)
- Committed: Yes (tracked binary artifact per project requirements)

**`source/app/build/`:**
- Purpose: Intermediate Gradle compilation, DEX files, and test results
- Generated: Yes
- Committed: No (ignored via `.gitignore`)

**`original/extracted/`:**
- Purpose: Decompiled inspection assets from original APK
- Generated: Yes (via reversal tools)
- Committed: No (ignored via `.gitignore`)

---

*Structure analysis: 2026-11-03*
