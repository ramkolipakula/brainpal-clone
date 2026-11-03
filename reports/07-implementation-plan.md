# 07 - Implementation Plan: BrainPal Rebuild

## 1. Architectural Strategy
- **Architecture**: Clean Architecture with MVVM pattern.
- **Layers**:
  - `domain`: Core models (`AppUsageStat`, `BlockedApp`, `RoastMeme`, `DetoxScore`), interfaces, and business logic.
  - `data`: `PreferencesRepository`, `UsageStatsRepository`, `RoastMemeRepository` (parsing `roast_gifs.json`), local storage.
  - `service`: `UsageMonitorService` (foreground service monitoring app launches) and `ReelsAccessibilityService`.
  - `ui`: Jetpack Compose Material 3 screens:
    - `navigation`: Screen routing (`AppNavGraph`, `Screen` sealed class).
    - `screens.onboarding`: Interactive setup and permission guides.
    - `screens.home`: Dashboard with Buddy mascot emotion, live usage gauges, quick snoozes.
    - `screens.apps`: Target app selector & threshold duration sliders.
    - `screens.overlay`: Full-screen intervention overlay with meme roast selector.
    - `screens.stats`: Daily and hourly analytics graphs.
    - `screens.settings`: Meme roast language pool selector (13 languages), strict mode, schedules.
    - `theme`: Material 3 typography, dynamic color palette, dark/light mode.

## 2. Technical Stack & Versions
- **Language**: Kotlin 1.9.22
- **Java Compatibility**: JDK 17
- **Android Gradle Plugin**: 8.3.2 / 8.2.2
- **Compile / Target SDK**: 34
- **Min SDK**: 26
- **Jetpack Compose Compiler**: Compatible with Kotlin 1.9.22 (Compose 1.5.8)
- **Material 3**: 1.2.1
- **Kotlinx Serialization / Gson**: For parsing `roast_gifs.json`
- **Coroutines & StateFlow**: 1.8.0

## 3. Directory Layout in `D:\APK-Rebuild\source`
```
D:\APK-Rebuild\source\
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
└── app/
    ├── build.gradle.kts
    └── src/
        ├── main/
        │   ├── AndroidManifest.xml
        │   ├── assets/
        │   │   └── roast_gifs.json
        │   ├── java/com/brainrot/rebuilt/
        │   │   ├── BrainPalApp.kt
        │   │   ├── MainActivity.kt
        │   │   ├── BlockOverlayActivity.kt
        │   │   ├── data/
        │   │   ├── domain/
        │   │   ├── service/
        │   │   └── ui/
        │   └── res/
        │       ├── drawable/
        │       ├── raw/
        │       └── values/
        └── test/
            └── java/com/brainrot/rebuilt/
```

## 4. Execution Phases
1. Create project skeleton and Gradle build scripts.
2. Transfer assets (`roast_gifs.json`, mascot images, sounds) from decompiled/extracted APK.
3. Implement Domain models and Data repositories.
4. Implement UI Composables and Navigation.
5. Implement Background Services and Receivers.
6. Run unit tests (`testDebugUnitTest`).
7. Execute CodeRabbit review.
8. Assemble signed debug APK (`assembleDebug`).
9. Verify final APK integrity, calculate SHA-256, and deliver to `D:\APK-Rebuild\delivery\RebuiltApp.apk`.

