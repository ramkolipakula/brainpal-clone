# Architecture & Project Decisions

**Ingested:** 2026-11-03
**Source:** `PRD.md`

## Architecture Decisions

### DEC-001: Clean-Room Native Android Implementation
- **source:** `PRD.md` (Overview, Task 3)
- **status:** active
- **decision:** Implement a native Android application using Kotlin 1.9, Jetpack Compose BOM 2024.04.00, Material 3, and Kotlin Coroutines/Flows, independent of the decompiled code.
- **scope:** Core application structure and build system

### DEC-002: Offline-First & Privacy-Preserving Operation
- **source:** `PRD.md` (Tasks 2, 5)
- **status:** active
- **decision:** Reconstructed application must run completely offline without cloud backends, user accounts, or external analytics/tracking services.
- **scope:** Security, privacy, networking

### DEC-003: Platform Integration Strategy for Interventions
- **source:** `PRD.md` (Tasks 1, 2, 5)
- **status:** active
- **decision:** Use Android OS `UsageStatsManager` (`PACKAGE_USAGE_STATS`) for app screen time calculations, `AccessibilityService` (`BIND_ACCESSIBILITY_SERVICE`) for reel swipe detection, and `WindowManager` (`SYSTEM_ALERT_WINDOW`) for floating badge and full-screen roast interventions.
- **scope:** Background services, accessibility, overlay

