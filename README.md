# Routine — one app to track your water, meals, and spending

**Routine** is an Android app for tracking daily water intake, meal quality, and expenses. It listens to bank notifications (Nubank, Itaú, Bradesco, XP, Inter) to automatically record expenses, lets you log each meal with a quality status, and helps you hit your daily hydration goal with quick-add buttons and a circular progress indicator.

## Features

### 💧 Water Tracking
- Configurable daily water goal
- Quick-add buttons with customizable default amounts
- Circular progress indicator with live percentage
- Daily total shown on the home card
- Text turns **green** when the daily goal is exceeded

### 🍽️ Meal Tracking
- Log meals by time slot: **Breakfast**, **Lunch**, **Tea**, **Dinner** (auto-selected based on current hour)
- Rate each meal as one of:
  - ✅ **Correct meal**
  - ⚠️ **Warning meal**
  - ❌ **Wrong meal**
- Optional observation/notes field for each meal record

### 💳 Automatic Expense Tracking
- **Notification Listener Service** detects bank notifications and automatically records expenses
- Supported banks: **Nubank**, **Itaú**, **Bradesco**, **XP**, **Inter**
- Configurable monthly spending limit
- Home card shows monthly total spent vs. limit

### 📅 History & Calendar
- **Weekly summary**: water days achieved, total spent, correct/warning/wrong meal counts
- **Calendar views**: toggle between **Week** and **Month** views
- **Day color coding** (see [Day Color Rules](#day-color-rules) below)
- Click any day to see detailed records for water, meals, and expenses
- Selected day is highlighted with an outline while preserving its status color

### ⚙️ Configurable Settings
Access via the user menu (⋮) → **Config**:
- Daily water goal
- Default quick-add button values (button 1, button 2, button 3)
- Monthly expense limit

## Day Color Rules

Each day in the calendar is colored based on the day's water intake and meal records, calculated by `HistoryService.getDayStatus()`.

### Status Priority

| Status | Color | Condition |
|--------|-------|-----------|
| 🔴 **RED** | Red | **2 or more** wrong meals |
| 🟡 **YELLOW** | Yellow | **2 or more** warning meals **OR** **1 or more** wrong meals |
| 🟢 **GREEN** | Green | Score ≥ **3** (see scoring below) |
| 🟡 **YELLOW** | Yellow | Score between **0 and 2** (inclusive) |
| ⚪ **NONE** | Default / Has-Data | No data, or data present but status criteria not met |

> **Note:** RED and YELLOW checks run **first** and take priority over the scoring model. A day with 2+ wrong meals is always RED, even if the score would otherwise be GREEN.

### Scoring Model

Used only after the RED/YELLOW priority checks have passed:

| Action | Points    |
|--------|-----------|
| Achieved daily water goal | **+2**    |
| Each correct meal | **+1**    |
| Each warning meal | **-0.5**  |
| Each wrong meal | **−1**    |

**Day classification:**

- **GREEN** → `score >= 3`
- **YELLOW** → `0 <= score <= 2`
- **NONE** → data present but doesn't qualify for any status above

### Calendar Edge Cases

| Case | Display |
|------|---------|
| **Future days** | Default background (no color) |
| **Day has data but status is NONE** | "Has data" highlight color |
| **Day has no data** | Default background |
| **Selected day** | Status color + **outline** highlight |

## Tech Stack

- **Language:** Java 21
- **Min SDK:** 33 (Android 13) · **Target SDK:** 36 (Android 16)
- **UI:** AndroidX AppCompat, Material Components, ConstraintLayout
- **Database:** SQLite (`Routine` database)
- **Architecture:** Simple Service-Activity pattern with manual SQLite queries

## Build

```bash
./gradlew assembleDebug
```

The APK will be generated at `app/build/outputs/apk/debug/app-debug.apk`.

## Permissions

The app requires **Notification Access** to detect bank expense notifications. When the expense card is enabled on the home screen and access is not yet granted, the app will prompt you to enable it in the system settings.

## Supported Banks

| Bank | Package Identifier |
|------|-------------------|
| Nubank | `nubank` |
| Itaú | `itau` |
| Bradesco | `bradesco` |
| XP | `xp` |
| Inter | `inter` |