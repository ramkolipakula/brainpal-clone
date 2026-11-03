# Technical Constraints

**Ingested:** 2026-11-03
**Source:** `PRD.md`

## Platform Constraints
- Minimum Android SDK: Android 8.0 (API level 26)
- Target Android SDK: Android 14 (API level 34)
- Special Android Permissions: `PACKAGE_USAGE_STATS`, `SYSTEM_ALERT_WINDOW`, `BIND_ACCESSIBILITY_SERVICE`, `POST_NOTIFICATIONS`, `FOREGROUND_SERVICE_SPECIAL_USE`

## Architecture Constraints
- Zero reliance on external network APIs or cloud backends (air-gapped privacy)
- Offline-bundled assets for meme and roast catalogs (`assets/roast_gifs.json`)
- Clean-room implementation in modern Kotlin and Jetpack Compose (no decompiled code imports)

