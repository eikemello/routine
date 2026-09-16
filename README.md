# Routine — one app to track your water, meals, workouts and spending

**Rotta** is an Android app for tracking daily habits and health: water intake, meal quality, workout/medication/supplement routines, and expenses. It listens to bank notifications (Nubank, Itaú, Bradesco, XP, Inter) to automatically record expenses, lets you log each meal with a quality status, and summarizes your performance with a weighted daily score on a history calendar.

## Features

### 🧩 Trackers
- Six tracking modules that can be individually enabled/disabled: **Water**, **Meals**, **Expenses**, **Workout**, **Medication**, **Supplement**
- The home screen is built dynamically: only cards of enabled trackers are rendered
- A segmented progress bar in the header shows how many enabled trackers are complete today (water goal reached, all 4 regular meals logged, or a routine marked as done)
- Configure via the **Trackers** screen in the bottom navigation

### 💧 Water Tracking
- Configurable daily water goal (default: 2500 ml)
- Quick-add buttons with customizable default amounts (defaults: 50/100/250 ml)
- Linear progress indicator with live percentage
- Daily total shown on the home card, turning **green** when the goal is reached
- "Last added" record shows the time and amount of the most recent entry

### 🍽️ Meal Tracking
- Log meals by time slot: **Breakfast** (5h–10h), **Lunch** (11h–15h), **Tea** (16h–19h), **Dinner** (otherwise — the matching slot is pre-selected in the dialog)
- Rate each meal as one of:
  - ✅ **Correct meal**
  - ⚠️ **Warning meal**
  - ❌ **Wrong meal**
- Optional observation/notes field for each meal record
- **Different meal** option: log an irregular meal with a custom name (name + observation required); irregular meals don't count toward the 4 regular slots
- If a regular meal is already logged for today, the app asks whether to **update** the existing record instead of inserting a duplicate

### 💳 Automatic Expense Tracking
- **Notification Listener Service** detects bank notifications and automatically records expenses
- Supported banks: **Nubank**, **Itaú**, **Bradesco**, **XP**, **Inter**
- **Multiple credit cards**: configure bank name, last four digits and the statement closing day for each card
- Spending is measured **per card inside its own statement cycle** (the cycle that started on the card's closing day)
- When no card is configured, a single global cycle is used (fallback closing day configurable)
- Configurable monthly limit; each card gets its own progress bar plus a total

### 📅 History & Calendar
- **Weekly summary**: water days achieved / days elapsed, total spent, correct/warning/wrong meal counts
- **Monthly summary**: the same metrics for any browsed month
- **Calendar views**: toggle between **Week** and **Month** views, navigate between weeks/months
- **Day color coding** based on a weighted daily score (see [Day Scoring](#day-scoring) below)
- **Score breakdown**: tapping a day shows a human-readable explanation of the score (e.g. `Water 33%, Meals 25% (Breakfast 8%, Lunch 8%), Total 58%`)
- Day details grid with an icon/value/color cell per enabled tracker
- Selected day is highlighted with an outline while preserving its status color

### ⚙️ Configurable Settings
Access via the **Config** screen in the bottom navigation:
- Daily water goal
- Default quick-add button values (button 1, button 2, button 3)
- Monthly expense limit
- Fallback card statement closing day
- Credit cards list (bank name, last four digits, closing day 1–31)
- Notification access shortcut (opens the system Notification Listener settings)

## Day Scoring

Each day in the calendar is colored based on a **weighted average percentage** computed by `DayScore` in `service/calendar/`.

### Scoring Units

The total 100% is divided equally among the **enabled** trackers, excluding Expenses. The **Meals tracker counts as 4 units** — one per regular meal slot (Breakfast, Lunch, Tea, Dinner).

| Unit | Score (0–1) |
|------|-------------|
| Water | `min(consumed / daily goal, 1.0)` — proportional to the goal |
| Each regular meal logged | Correct = **1.0** · Warning = **0.5** · Wrong = **0.0** · not logged = 0.0 |
| Workout / Medication / Supplement | Completed = **1.0** · not completed = **0.0** |
| Expenses | **Excluded** from the score |

### Day Color Thresholds

| Status | Color | Condition |
|--------|-------|-----------|
| 🟢 **GREEN** | Green | Score ≥ **85%** |
| 🟡 **YELLOW** | Yellow | Score between **55% and 84%** |
| 🔴 **RED** | Red | Score **< 55%** |
| ⚪ **NONE** | Default / Has-Data | No enabled trackers, or no data at all that day |

> **Example:** with Water + Meals + Workout enabled there are 6 units (1 water + 4 meals + 1 workout), each worth ~16.7%. Drinking 50% of the goal, logging a correct breakfast and lunch, and completing the workout gives 0.5 + 1.0 + 1.0 + 1.0 = 3.5 of 6 units → **~58% → YELLOW**.

### Calendar Edge Cases

| Case | Display |
|------|---------|
| **Future days** | Default background (no color) |
| **Day has data but status is NONE** | "Has data" highlight color |
| **Day has no data** | Default background |
| **Selected day** | Status color + **outline** highlight |

## Tech Stack

- **Language:** Java 21 (uses records and switch expressions)
- **Min SDK:** 33 (Android 13) · **Target SDK:** 36 (Android 16)
- **UI:** AndroidX AppCompat, Material Components, ConstraintLayout, Edge-to-Edge layouts
- **Database:** SQLite (`Routine` database, schema v6)
- **Build:** Gradle 9.4.1 (Groovy DSL) with Android Gradle Plugin 9.2.1 via a version catalog (`gradle/libs.versions.toml`)
- **Architecture:** Activity → Service → Repository → SQLite, with renderer classes for the calendar/detail UI

## Build

```bash
./gradlew assembleDebug
```

The APK will be generated at `app/build/outputs/apk/debug/app-debug.apk`.

### Firebase configuration

The app uses Firebase Crashlytics, which requires an `app/google-services.json` file. This file is **not** committed to the repository (it contains a project API key) and must be downloaded for each development machine:

1. Open the [Firebase console](https://console.firebase.google.com/) project.
2. Go to **Project settings → Your apps → Android app (`com.android.nls.routine`)**.
3. Download `google-services.json` and place it at `app/google-services.json`.
4. Rebuild (`./gradlew assembleDebug`).

Without this file the build fails with *"File google-services.json is missing"*. See `app/google-services.json.example` for the expected structure (values redacted).

## Permissions

The app requires **Notification Access** to detect bank expense notifications. It is registered in the manifest as a `NotificationListenerService`. Access can be granted from **Config → Notification Access**, which opens the system settings screen.

## Supported Banks

| Bank | Package Identifier |
|------|-------------------|
| Nubank | `nubank` |
| Itaú | `itau` |
| Bradesco | `bradesco` |
| XP | `xp` |
| Inter | `inter` |