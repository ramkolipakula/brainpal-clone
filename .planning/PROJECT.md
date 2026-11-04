# BrainPal Android Application Reconstruction

## What This Is

A clean-room, privacy-preserving native Android reconstruction of the BrainPal application built using Kotlin and Jetpack Compose. It serves as an anti-procrastination and digital wellbeing companion that provides smart screen time interventions, app usage monitoring, localized meme/roast interventions, short-video reel tracking, healthy session limits, and a modern Material 3 user experience.

## Core Value

Real-time, resilient on-device intervention against mindless social media scrolling without compromising user privacy or consuming unnecessary battery.

## Requirements

### Validated

- [x] Clean-room APK reconstruction with Kotlin 1.9, Jetpack Compose BOM 2024.04.00, and Material 3
- [x] Usage monitoring via `UsageStatsManager` with background foreground service
- [x] Real-time short video reel swipe detection for Instagram Reels and YouTube Shorts via Accessibility Service
- [x] Draggable floating reel-counter capsule with dynamic 5-stage degenerating brain mascot
- [x] Fullscreen intervention overlay (`BlockOverlayActivity`) with localized meme roast catalog (`roast_gifs.json`)
- [x] App-specific and daily screen time quotas, emergency passes, and snooze controls
- [x] Midnight automatic reset receiver for streak and daily usage counters
- [x] 21/21 automated unit tests passing across business logic and swipe detection engines
- [x] Verified installable deliverable at `delivery/RebuiltApp.apk`

### Active

- [ ] Ongoing maintenance, ruleset updates for future third-party app layout revisions, and optional DataStore migration

### Out of Scope

- Remote cloud backend / telemetry / analytics servers — App operates strictly offline for total user privacy.
- Account sign-in / OAuth / cloud backup — State is preserved locally on-device.
- Decompiled binary code reuse — App is a 100% clean-room native rewrite.

## Context

- Target Android SDK 34 (Android 14) with backward compatibility to Android 8.0 (API 26).
- Original APK was reverse-engineered using JADX and custom Python inspection scripts.
- Project completed 10 intensive development and remediation phases covering discovery, design, implementation, testing, real-device audits, defect remediation, and floating UI synchronization.

## Constraints

- **Platform**: Android SDK 26–34 (`compileSdk = 34`, `minSdk = 26`)
- **Permissions**: Requires user-granted `PACKAGE_USAGE_STATS`, `SYSTEM_ALERT_WINDOW`, and `BIND_ACCESSIBILITY_SERVICE`
- **Network**: Air-gapped offline operation; no network calls allowed
- **Lifecycle**: Zero memory leaks and graceful recovery from OEM background process termination

## Key Decisions

| Decision | Rationale | Outcome |
|----------|-----------|---------|
| Clean-Room Jetpack Compose Rebuild | Modern declarative UI ensures maintainability and eliminates legacy decompiled technical debt | ✓ Good |
| Pure Domain `ReelDetectionEngine` | Decouples complex swipe filtering from Android UI lifecycle; allows hermetic JVM unit testing | ✓ Good |
| Reactive `StateFlow` Synchronization | Ensures instantaneous updates across background Accessibility Service, floating overlay, and foreground screens | ✓ Good |
| Offline-Bundled Roast Meme Asset Pool | Eliminates server dependencies and protects user privacy while retaining rich meme content | ✓ Good |

---
*Last updated: 2026-11-03 after GSD Onboarding & Doc Ingest*

