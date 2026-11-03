# Requirements: BrainPal Android Application Reconstruction

**Defined:** 2026-11-03
**Core Value:** Real-time, resilient on-device intervention against mindless social media scrolling without compromising user privacy or consuming unnecessary battery.

## v1 Requirements

Requirements established by the reconstructed application specification and PRD.

### Platform & Architecture (ARCH)

- [x] **ARCH-01**: Clean-room native Android Gradle project using Kotlin 1.9 and Jetpack Compose (BOM 2024.04.00)
- [x] **ARCH-02**: Modern Material 3 UI design system with dynamic theming and custom vector graphics
- [x] **ARCH-03**: Complete offline-first, privacy-preserving runtime architecture with zero external analytics/telemetry

### Core Wellbeing & Usage Monitoring (WELL)

- [x] **WELL-01**: Foreground app monitoring using Android `UsageStatsManager` with specialUse foreground service
- [x] **WELL-02**: Per-app daily time limit quotas and global digital wellbeing screen time tracking
- [x] **WELL-03**: Emergency snooze functionality and configurable emergency pass replenishment
- [x] **WELL-04**: Automatic midnight calendar reset (`DateChangedReceiver`) for daily counters and usage streaks

### Real-Time Reel Detection & Floating Capsule (REEL)

- [x] **REEL-01**: Android `AccessibilityService` monitoring short-form video events on Instagram and YouTube Shorts
- [x] **REEL-02**: Pure domain `ReelDetectionEngine` filtering scroll flings, horizontal carousels, and deduplicating swipe bursts
- [x] **REEL-03**: Draggable floating pill overlay (`WindowManager`) displaying real-time reel count
- [x] **REEL-04**: 5-stage brain mascot aging animation engine (`MascotStageCalculator`) rendering brainrot progression

### Interventions & User Experience (INTV)

- [x] **INTV-01**: Fullscreen intervention overlay (`BlockOverlayActivity`) triggered when limits or reel quotas are exceeded
- [x] **INTV-02**: Multi-language roast meme delivery parsed from bundled `assets/roast_gifs.json`
- [x] **INTV-03**: Full onboarding permission flow guiding users through Usage Access, Overlay, and Accessibility settings

### Verification & Delivery (DELV)

- [x] **DELV-01**: Automated JVM unit tests for business logic, mood calculations, and swipe detection
- [x] **DELV-02**: Android Lint verification with zero fatal errors
- [x] **DELV-03**: Signed installable production APK deliverable packaged at `delivery/RebuiltApp.apk`

## Traceability

| Requirement | Phase | Status |
|-------------|-------|--------|
| ARCH-01 | Phase 3, 4 | Complete |
| ARCH-02 | Phase 4 | Complete |
| ARCH-03 | Phase 3, 4 | Complete |
| WELL-01 | Phase 4 | Complete |
| WELL-02 | Phase 4 | Complete |
| WELL-03 | Phase 4, 9 | Complete |
| WELL-04 | Phase 9 (DEF-10) | Complete |
| REEL-01 | Phase 4, 10 | Complete |
| REEL-02 | Phase 10 | Complete |
| REEL-03 | Phase 10 | Complete |
| REEL-04 | Phase 10 | Complete |
| INTV-01 | Phase 4 | Complete |
| INTV-02 | Phase 4 | Complete |
| INTV-03 | Phase 4, 8 | Complete |
| DELV-01 | Phase 5, 9, 10 | Complete |
| DELV-02 | Phase 6, 9, 10 | Complete |
| DELV-03 | Phase 7, 9, 10 | Complete |

**Coverage:**
- v1 requirements: 17 total
- Mapped to phases: 17
- Unmapped: 0

---
*Requirements verified: 2026-11-03*
