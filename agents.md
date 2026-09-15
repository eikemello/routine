# Agent Guide: Routine App

This document provides a comprehensive overview of the Routine app's architecture, functionality, and codebase for AI agents. Use this as the primary context when implementing features, fixing bugs, or refactoring.

## 📱 App Overview

**Routine** (display name **Rotta**) is an Android application designed for daily habit and health tracking. It is built around **trackers** — six modules that the user can enable/disable individually:

1.  **Water**: Tracking daily hydration against a configurable goal.
2.  **Meals**: Logging the 4 regular meal slots (Breakfast, Lunch, Tea, Dinner) and rating their quality (Correct, Warning, Wrong), plus irregular "different" meals.
3.  **Expenses**: Using a `NotificationListenerService` to automatically record spending from specific bank notifications, organized by user-configured credit cards.
4.  **Workout / Medication / Supplement**: Simple daily completed/not-completed routines.

The app also features a history calendar that visually summarizes daily performance using a weighted-average scoring system.

---

## 🏗️ Technical Architecture

### Stack
- **Language**: Java 21 (model classes are Java records; switch expressions used throughout)
- **Android SDK**: Min 33 (Android 13), Target 36 (Android 16)
- **Build**: Gradle 9.4.1 (Groovy DSL `build.gradle` files) with AGP 9.2.1 and a version catalog (`gradle/libs.versions.toml`)
- **Database**: SQLite (via `DatabaseHelper` and manual SQL queries)
- **UI**: AndroidX AppCompat, Material Components, ConstraintLayout, Edge-to-Edge

### Package Structure
- `com.android.nls.routine`: `MainActivity` (launcher; immediately forwards to `HomeActivity`).
- `com.android.nls.routine.activity`: UI controllers — `HomeActivity`, `HistoryActivity`, `ConfigActivity`, `TrackerConfigActivity`.
- `com.android.nls.routine.database`: `DatabaseHelper` (schema v6, singleton with reference counting).
- `com.android.nls.routine.listener`: `NotificationListener` — the background `NotificationListenerService`.
- `com.android.nls.routine.model`: Java record POJOs (`WaterRecord`, `MealRecord`, `ExpenseRecord`, `TrackerRecord`, `Tracker`, `TrackerType` enum, `CreditCard`, `CardSpending`, `ExpenseCardSummary`, `WeeklySummary`, `DayDetails`, `DayStatus`, `DayStatusInfo`).
- `com.android.nls.routine.parser`: `BankDetector`, `NotificationTextExtractor`, and the `Parser` interface with one implementation per bank (Nubank, Itaú, Bradesco, XP, Inter).
- `com.android.nls.routine.repository`: DAO layer — `WaterRepository`, `MealRepository`, `ExpenseRepository`, `ConfigRepository`, `TrackerRepository`, `CardRepository`.
- `com.android.nls.routine.service`: Business logic — `HomeService`, `HomeCardWaterService`, `HomeCardMealService`, `HomeCardExpenseService`, `HistoryService`, `ConfigService`.
- `com.android.nls.routine.service.calendar`: History/calendar logic — `DayScore` (weighted scoring), `DayScoreData`, `HistoryContext`, `HistoryCalendarRenderer`, `DayDetailsRenderer`.
- `com.android.nls.routine.utils`: `Constants` (all table/column names, message strings, tracker/bank identifiers), `Common` (date/time helpers), `BottomNavHelper`.

### Data Flow
1.  **Expenses**: `NotificationListener` $\rightarrow$ `BankDetector` $\rightarrow$ `Parser` (per bank) $\rightarrow$ `HomeCardExpenseService.saveExpenseTest()` $\rightarrow$ `ExpenseRepository` $\rightarrow$ SQLite `EXPENSE_TEST`.
2.  **Home cards**: `HomeActivity` $\rightarrow$ `HomeCardWaterService` / `HomeCardMealService` / `HomeCardExpenseService` / `HomeService` $\rightarrow$ `Repository` $\rightarrow$ SQLite. The card list is built dynamically from `TrackerRepository.getEnabledTrackers()`.
3.  **History**: `HistoryActivity` $\rightarrow$ `HistoryService` (+ `HistoryContext`) $\rightarrow$ Repositories $\rightarrow$ SQLite $\rightarrow$ `DayScore.compute()` per day $\rightarrow$ `HistoryCalendarRenderer` (grid cells) and `DayDetailsRenderer` (detail grid).

---

## 🛠️ Key Components

### Core Logic
- **`DatabaseHelper`**: Manages the "Routine" SQLite database (schema **v6**). A singleton obtained via `getInstance()` with a **reference-counting scheme**: each repository calls `acquire()` in its constructor and must call `release()` in its `closeDb()`; the database is closed (and the instance nulled) only when the last holder releases. `onUpgrade` applies migrations in order, guarded by version checks.
- **`NotificationListener`**: The background service that intercepts bank notifications and delegates to `BankDetector`.
- **`BankDetector`**: Matches the notification package against the known bank identifiers and dispatches to the right `Parser` implementation.
- **`Parser` & implementations**: Handle the regex/text extraction for Nubank, Itaú, Bradesco, XP, and Inter (`NotificationTextExtractor` pulls the raw text out of the `StatusBarNotification`).
- **`DayScore`**: The central scoring logic (see [Calendar Scoring System](#calendar-scoring-system)). Provides `compute()` (returns a `DayStatus`), `computePercentage()`, and `getBreakdown()` (human-readable score explanation).
- **`HistoryService`**: Aggregates weekly/monthly summaries and per-day statuses for a date range (`getDayStatusesForRange()`), with a small **LRU cache** for day details.
- **`HistoryCalendarRenderer` / `DayDetailsRenderer` / `HistoryContext`**: Render the week/month calendar grid (cell colors, selection outline, navigation) and the per-tracker day-details grid; `HistoryContext` bundles the shared history state.
- **`HomeCardWaterService` / `HomeCardMealService` / `HomeCardExpenseService`**: Per-card business logic for the home dashboard. `HomeService` computes which trackers are completed today (water goal, all 4 meals logged, routines done).
- **`ConfigService`**: Backs `ConfigActivity` (goal/button/limit dialogs, cards dialog, notification access shortcut).

### Database Schema
All table and column names live in `Constants`. The database contains seven tables:

- **`WATER`**: `_id`, `WATER_DRANK` (INTEGER, ml), `TIMESTAMP` (INTEGER, epoch millis).
- **`USER_CONFIG`**: `_id`, `DAILY_WATER`, `BTN_1_ADD_WATER`, `BTN_2_ADD_WATER`, `BTN_3_ADD_WATER`, `MONTHLY_LIMIT`, `CARD_STATEMENT_CLOSING` (all TEXT).
- **`EXPENSE_TEST`**: `_id`, `EXPENSE_VALUE` (REAL), `EXPENSE_TEXT`, `BANK`, `TIMESTAMP`.
- **`MEAL`**: `_id`, `MEAL` (slot name — regular slot or custom name), `MEAL_STATUS` (`CORRECT_MEAL`, `WARNING_MEAL`, `WRONG_MEAL`, `OTHER_MEAL` for irregular meals), `MEAL_OBS`, `TIMESTAMP`.
- **`TRACKERS`**: `_id`, `TRACKER_TYPE`, `TRACKER_NAME`, `TRACKER_ICON`, `TRACKER_ENABLED`, `TRACKER_DESCRIPTION`. Seeded with the six default trackers on create.
- **`TRACKER_RECORDS`**: `_id`, `TRACKER_RECORD_TYPE`, `TRACKER_RECORD_COMPLETED` (INTEGER), `TRACKER_RECORD_NOTE`, `TRACKER_RECORD_TIMESTAMP`.
- **`CARDS`**: `_id`, `BANK_NAME`, `LAST_FOUR`, `CLOSING_DAY` (INTEGER 1–31). Added in v6; saves replace the whole table in a single transaction.

`onCreate` also creates indexes on the timestamp columns (`idx_water_timestamp`, `idx_expense_timestamp`, `idx_meal_timestamp`, `idx_tracker_records_type_timestamp`).

---

## 📊 Business Rules

### Calendar Scoring System
The day score is a **weighted average percentage** computed by `DayScore.compute()`:

1.  The 100% total is divided **equally among the enabled scoring units**:
    - **Meals** counts as **4 units** (one per regular slot: Breakfast, Lunch, Tea, Dinner).
    - **Water**, **Workout**, **Medication**, **Supplement** each count as **1 unit**.
    - **Expenses** are **excluded** from the score.
2.  Each unit is scored from 0.0 to 1.0:
    - **Water**: `min(consumed / dailyGoal, 1.0)` (0 if the goal is not positive).
    - **Each regular meal**: correct = **1.0**, warning = **0.5**, wrong = **0.0**, not logged = 0.0 (irregular `OTHER_MEAL` records are ignored).
    - **Workout/Medication/Supplement**: completed = **1.0**, otherwise **0.0**.
3.  The day color comes from the resulting percentage:
    - **GREEN**: $\ge 85\%$
    - **YELLOW**: $\ge 55\%$ (and below 85%)
    - **RED**: below 55%
    - **NONE**: no enabled trackers, or no data at all that day (the day is then only marked "has data" if any records exist).

The same inputs also produce a human-readable breakdown via `DayScore.getBreakdown()` shown on the history day view.

### Expense Statement Cycles
- Each configured **card** (`CARDS` table) has its own closing day (1–31); spending is summed from the most recent occurrence of that day (`Common.getStartOfExpenseCycleInMillis()`).
- With **no cards configured**, expenses are summed inside the single global cycle defined by `USER_CONFIG.CARD_STATEMENT_CLOSING`.
- The **monthly limit** is used to compute per-card progress on the home card.

### Meal Logging Rules
- Default slot is pre-selected by the current hour: Breakfast 5h–10h, Lunch 11h–15h, Tea 16h–19h, Dinner otherwise.
- Regular meals are **unique per day**: saving over an existing slot prompts the user to update it instead of inserting a duplicate.
- **"Different meal"** (irregular): stores the typed name as `MEAL` and the clicked button as `MEAL_STATUS`; the observation field becomes required. Irregular meals do not count toward the 4 regular slots anywhere (home progress, scoring, day details).

---

## 📝 Guidelines for AI Agents

### When adding new features:
1.  **Data**: Check if a new table is needed in `DatabaseHelper` (bump `DATABASE_VERSION` and add a guarded migration in `onUpgrade`) or if a generic `TRACKER_RECORDS` entry suffices.
2.  **Repository**: Create or update a `Repository` class in `com.android.nls.routine.repository` to handle the SQL. Remember the `acquire()`/`release()` contract with `DatabaseHelper`.
3.  **Service**: Implement the business logic in a `Service` class (split per home card when needed). Avoid putting business logic directly in `Activity`.
4.  **UI**: Use Material Components and ensure compatibility with the current theme. Keep screen-building logic in renderer-style classes when it grows (see `service.calendar`).

### When fixing bugs:
- **Bank Parsing**: If a bank changes its notification format, update the corresponding `Parser` implementation (e.g., `NubankParser`).
- **Database**: Since the app uses manual SQL queries, always verify the column names against `Constants` and `DatabaseHelper`.
- **Dates**: Use the helpers in `Common` (`getStartOfDayInMillis`, `getStartOfWeekInMillis`, `getStartOfExpenseCycleInMillis`, ...). Week calculations explicitly compute Monday and do **not** use `Calendar.set(DAY_OF_WEEK, MONDAY)` because it is locale-dependent.

### Constraints:
- Maintain the "Activity → Service → Repository" pattern; keep UI construction in Activities/renderers.
- Use `Constants` for table and column names to avoid typos.
- Ensure `NotificationListener` permissions are handled gracefully (the Config screen offers the system settings shortcut).
- Balance every `DatabaseHelper.acquire()` with a `release()` (usually via the repository's `closeDb()`), otherwise the database never closes.
