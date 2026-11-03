# 06 - Unknowns and Risks: BrainPal Rebuild

## 1. Technical Risks & Unknowns Identified

| Risk Area | Risk Description | Severity | Mitigation Strategy |
|---|---|---|---|
| **Proprietary Cloud Backend** | The original app communicates with `regainapp.ai` for user duels, friend invites, and cloud sync. Server private code and database are inaccessible. | Low / Expected | Implement clean local offline persistence (Room + DataStore). All wellbeing monitoring, app blocking, meme roasts, and stats are 100% self-contained on the device. |
| **In-App Billing & Razorpay/RevenueCat** | The original APK integrates commercial SDKs (Google Play Billing, Razorpay, RevenueCat). | Boundary / Out of Scope | Per prompt instructions: Do not bypass payment verification or reuse private publisher keys. All wellbeing features are provided fully unlocked and free in the clean-room rebuild without dummy payment screens. |
| **OEM Background Killing (MIUI / HyperOS / ColorOS / Samsung)** | Aggressive battery managers terminate background usage watchers and floating overlay services. | Medium | Implement standard Android Foreground Service (`specialUse`), `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`, and OEM battery guidance instructions (videos/audio included in app assets). |
| **Android 14/15 Foreground Service Restrictions** | Android 14+ requires explicit `foregroundServiceType` in manifest for overlays and usage tracking. | Medium | Correctly declare `foregroundServiceType="specialUse"` and provide user-facing notification with stop action. |
| **Short-Form Video Feed API Absence** | Instagram and YouTube do not provide public APIs to measure how many Reels or Shorts a user has viewed. | Low / Mitigated | Emulate BrainPal's dual-engine approach: `UsageStatsManager` measures foreground app screen time and launch frequency; optional `AccessibilityService` tracks swipe events. |
| **Meme CDN Availability** | Memes in `roast_gifs.json` are hosted on `https://puraa.regainapp.ai`. If CDN is unreachable, online memes might fail to load. | Medium | Bundle fallback local memes, and handle network image loading errors gracefully with placeholder meme cards so the UI never crashes or displays a blank box. |

