# 04 - Data and API Dependencies: BrainPal

## 1. Local Data Models and Schemas
From Room database inspections and recovered data contracts:

### A. `BrainRotRoomDatabase` & `UserPrefDatabase`
- **ReelsScrollEvent**:
  - `id`: Long (Primary Key)
  - `packageName`: String (`com.instagram.android`, `com.google.android.youtube`, etc.)
  - `timestamp`: Long (Epoch millis)
  - `sessionId`: String
  - `scrollVelocity`: Float
- **DailyReelsStats**:
  - `date`: String (`YYYY-MM-DD`)
  - `totalReelsWatched`: Int
  - `totalTimeSpentMillis`: Long
  - `limitExceeded`: Boolean
  - `passesUsed`: Int
- **HourlyReelsSplit**:
  - `hour`: Int (0-23)
  - `reelsCount`: Int
- **UserBlockingConfig**:
  - `packageName`: String
  - `dailyReelLimit`: Int (default: 20)
  - `dailyTimeLimitMinutes`: Int (default: 30)
  - `isBlocked`: Boolean
  - `enablePaceNudge`: Boolean
  - `strictModeEnabled`: Boolean
  - `snoozeUntil`: Long

### B. Roast Meme Pool (`assets/roast_gifs.json`)
- Version: 2
- Total pools: 13 language pools (`hindi`, `english`, `telugu`, `kannada`, `tamil`, `indonesia`, `pakistan`, `bangla`, `turkish`, `malayalam`, `arabic`, `nepal`, `portuguese_br`).
- Pool Schema:
  - `id`: String
  - `source`: String ("tenor")
  - `url`: String (CDN hosted webp)
  - `caption`: Localized roast text
  - `w`, `h`: Int (dimensions)

## 2. Remote API Architecture (Original vs Rebuilt Clean-Room)

### Original App Endpoints (Observable References)
- `https://puraa.regainapp.ai/brainpal-roast-gifs/...` (Meme CDN)
- `https://api.regainapp.ai/v1/user/...` (Proprietary server for cloud sync, duels, account deletion)
- Deep links: `invite.brainpalapp.ai`, `join.brainpalapp.ai`

### Rebuilt Clean-Room Strategy
- **Master Prompt Mandate**: Do not fabricate server responses, reuse private publisher credentials, or bypass access controls.
- **Local-First Architecture**: All core features (app usage tracking, threshold triggers, meme roast interventions, daily stats, streaks, settings, preferences) operate **100% offline** using Room database and Jetpack DataStore.
- **Roast Catalog**: The complete `roast_gifs.json` is bundled locally within assets, with local fallback memes and direct CDN image loading when network is available.
- **Network Interface**: A clean `BrainPalSyncRepository` interface is provided with a local mock/fallback implementation and documented remote contract.

