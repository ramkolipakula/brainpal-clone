# PRD: BrainPal Android Application Reconstruction

## Overview
Reconstruct the BrainPal application from the original APK (`BrainPal.apk`) into a maintainable, high-performance, clean-room native Android application. The application is an anti-procrastination and digital wellbeing companion that provides smart screen time interventions, app usage monitoring, localized humorous meme/roast interventions, healthy session limits, and a modern Material 3 user experience.

## Task 1: Environment and Tooling Setup
Verify all execution prerequisites and reversal toolkits.
- Extract APK contents and verify SHA-256 integrity.
- Set up portable JDK 17, Android build tools, Gradle wrapper, and JADX.
- Verify CodeRabbit CLI readiness and GSD state configuration.

## Task 2: APK Static Analysis and Feature Extraction
Perform deep inspection of decompiled APK structure, manifest, and resources.
- Decode binary `AndroidManifest.xml` (package name, permissions, activities, services, receivers).
- Catalog all UI strings, styles, drawables, layouts, and meme assets (`roast_gifs.json`).
- Reverse engineer core business logic: usage tracking, app blocking/intervention triggers, and settings.
- Produce discovery reports 00 through 06 in `D:\APK-Rebuild\reports\`.

## Task 3: Architecture and Implementation Plan
Design the reconstructed application architecture.
- Select target Android SDK, Kotlin, Jetpack Compose, Material 3, and Coroutines architecture.
- Design data persistence (DataStore / Room) for blocked apps, daily limits, statistics, and user preferences.
- Design platform background service / accessibility / usage stats integration for real-time app intervention.
- Generate report `07-implementation-plan.md` and initialize the Android Gradle project skeleton.

## Task 4: UI Design and Screen Reconstruction
Implement observable screens using Jetpack Compose and Material 3 matching BrainPal's visual design.
- Onboarding & Permissions Screen (Usage Access, Overlay, Notification).
- Dashboard / Home Screen (Screen time summary, blocked apps status, streak counter, quick toggle).
- App Selection & Limit Configuration Screen (installed apps list, category grouping, time limit picker).
- Roast / Intervention Screen (full-screen meme intervention overlay with multi-language roast pools from `roast_gifs.json`).
- Statistics / Analytics Screen (daily/weekly usage charts, intervention frequency).
- Settings & Customization Screen (roast language selection, strict mode, schedules).

## Task 5: Core Wellbeing Engine and Background Logic
Implement the functional logic and platform integrations.
- `UsageStatsManager` queries to calculate foreground app time and detect blocked app launch.
- Overlay service / PiP intervention launcher with grace periods and emergency snooze.
- Local repository and DataStore preferences for blocked app packages, limits, and roast meme pool selection.
- Robust error handling, non-crashing fallbacks, and zero leak lifecycle management.

## Task 6: Unit and Integration Testing
Build automated tests to verify stability, validation, and domain logic.
- Unit tests for usage calculation algorithms, time limit thresholds, and preference state transitions.
- Unit tests for Meme/Roast selector logic across localized language pools.
- UI tests for screen rendering, state flows, and input validation.
- Compile and execute tests with Gradle; document results in `08-test-report.md`.

## Task 7: Code Review and Defect Remediation with CodeRabbit
Perform automated and manual code quality audit.
- Run CodeRabbit code review checks on reconstructed source code.
- Analyze logic vulnerabilities, memory leaks, concurrency races, permission handling, and UI jank.
- Remediate all valid critical, high, and medium severity findings.
- Re-run regression tests to verify fixes.

## Task 8: Release Packaging, Integrity Verification, and Delivery
Generate final installable Android APK and documentation.
- Compile signed debug APK using Android Gradle Plugin.
- Verify APK archive integrity, package metadata, and alignment using build tools.
- Copy deliverable to `D:\APK-Rebuild\delivery\RebuiltApp.apk`.
- Calculate SHA-256 checksum and verify file size.
- Produce `09-feature-comparison.md`, `10-final-delivery-report.md`, and complete `README.md`.

