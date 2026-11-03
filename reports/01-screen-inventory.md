# 01 - Screen Inventory: BrainPal

Based on APK static analysis, manifest activities, composable definitions, string resources, and extracted assets, here is the complete screen inventory for BrainPal.

## Screen 1: Onboarding & Interactive Permission Flow (`PermissionStepsOverlayActivity` & Composable Steps)
- **Purpose**: Welcomes the user, explains the mascot buddy ("Brain"), sets screen-time and dopamine-reduction goals, and guides user through system permissions required for wellbeing tracking.
- **Visual Assets**: `br_onboarding_brain_step_1.webp` through `step_4.webp`, `welcome_ting.mp3`, video guides (`bp-android-usage-access-tutorial.mp4`, `bp-xiaomi-usage-access-tutorial.mp4`).
- **Major Components**:
  - Carousel of wellbeing principles ("Reclaim your focus", "Break the scrolling loop").
  - Target app selector (Instagram, YouTube Shorts, Snapchat, Facebook, Reddit).
  - Daily reel / screen time threshold slider (e.g., limit to 20 reels/day or 30 minutes/day).
  - Permission Granting Cards:
    1. **Usage Access**: `PACKAGE_USAGE_STATS` (monitors app launch).
    2. **Overlay Permission**: `SYSTEM_ALERT_WINDOW` (displays intervention roast and floating counter).
    3. **Accessibility Service**: `BIND_ACCESSIBILITY_SERVICE` (optional/advanced reel swipe counter).
    4. **Notifications**: `POST_NOTIFICATIONS` (daily wrap-up, pace nudges).
  - Manufacturer-specific battery optimization guides (Motorola, Samsung, Xiaomi video/audio guides).

## Screen 2: Dashboard / Home Screen (`MainActivity` - Home Tab)
- **Purpose**: Central hub showing current day's progress, live buddy status, today's reel count, and active blocking rules.
- **Major Components**:
  - **Buddy Mascot Widget**: Animated Brain mascot showing emotional state (happy when within limits, panicked/roasted when bingeing). Assets: `br_brain_standing_1.webp` to `5.webp`.
  - **Daily Counter Metric**: Large radial / numeric counter of reels watched today vs. allowed limit.
  - **Quick Action Bar**: Emergency Pause (15-min snooze), Strict Mode toggle, Floating Bubble toggle.
  - **App Usage Breakdown**: List of monitored apps with individual counts (e.g., Instagram: 18 reels, YouTube: 4 shorts).
  - **Streak & Level**: Day streak counter (e.g. "Day 5 Dopamine Detox Streak").

## Screen 3: Block & Intervention Overlay (`BlockReelsOverlayActivity`)
- **Purpose**: Full-screen modal that triggers immediately when a user attempts to open a blocked app or scroll past their set reel limit.
- **Major Components**:
  - **Meme / Roast Card**: Displays a hilarious localized meme/gif from `roast_gifs.json` matching the user's selected language (Hindi, English, Telugu, Tamil, Indonesian, etc.) with roasting caption.
  - **Impact Summary**: "You've already watched 25 reels today (42 minutes wasted)".
  - **Action Buttons**:
    - "Exit App" (primary action, returns to home screen or closes app).
    - "Wait 10s to Continue" (friction timer).
    - "Emergency Snooze / 5 Min Break" (controlled bypass).

## Screen 4: Floating Bubble Overlay (`ReelsCounterFloatingService`)
- **Purpose**: Compact floating overlay widget that appears directly over Instagram/YouTube when short video feeds are active.
- **Major Components**:
  - Live count pill showing reels watched in the current session (e.g. `1/15`, `2/15`).
  - Color gradient shifting from green -> orange -> red as limit nears.
  - Tap to expand quick menu (dismiss, mute, pause).

## Screen 5: Statistics & Insights (`MainActivity` - Stats Tab)
- **Purpose**: Deep analytics into screen time habits, hourly usage patterns, and weekly detox improvements.
- **Major Components**:
  - Daily & weekly bar chart of reels watched (`HourlyReelsSplit`, `DailyReelsAppSplit`).
  - Top distracted apps split.
  - Weekly Story / Wrap-up summary (`WeeklyStoryShareTargetReceiver`).

## Screen 6: App Limits & Intervention Rules (`MainActivity` - Block Rules Tab)
- **Purpose**: Customization of which apps are blocked, schedule windows, and intervention strictness.
- **Major Components**:
  - Installed apps checklist with search filter.
  - Quota settings (session limits, daily reel limits, daily time limits).
  - Pace Nudge settings (vibrate or toast if scrolling more than 5 reels per minute).
  - Bedtime schedule / Focus hours (e.g. block between 10 PM and 6 AM).

## Screen 7: Settings & Profile (`MainActivity` - Settings Tab)
- **Purpose**: Application preferences, roast language selection, buddy customization, data management, and backup.
- **Major Components**:
  - Roast Language Selector (13 languages: Hindi, English, Telugu, Kannada, Tamil, Indonesian, etc.).
  - Notification Preferences (Pace nudges, daily wraps, morning motivation).
  - Local Data Management (clear stats, export data, reset streak).
  - About, Privacy Policy, and License info.

