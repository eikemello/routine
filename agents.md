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
- `com.android.nls.routine.service`: Business logic — `HomeService`, the home card services (`HomeCardWaterService`, `HomeCardMealService`, `HomeCardExpenseService`), `HistoryService` and `ConfigService`; the calendar pieces live in `service.calendar` and the card history panel in `cardhistory`.
- `com.android.nls.routine.cardhistory`: Everything a home card needs to open its history panel — `CardHistory` (the contract each card implements), `CardHistoryDialog` (the shared panel renderer), `CardRecordDialog` (the per-record edit/remove dialogs), `DynamicCardHistory` (the single `CardHistory` implementation that answers for the simple cards) and `DynamicCardHistoryService` (the service that backs them).
- `com.android.nls.routine.service.calendar`: History/calendar logic — `DayScore` (weighted scoring), `DayScoreData`, `HistoryContext`, `HistoryCalendarRenderer`, `DayDetailsRenderer`.
- `com.android.nls.routine.widget`: Home screen widgets — `WidgetCombinedProvider` (the combined water + meal card on the launcher: the water part adds water and shows today's total against the goal, and the meal part shows one segment per daily slot with the meals already logged, the day's `x/4` count and the Correct/Warning/Wrong status buttons, logging the current time-of-day slot without opening the app).
- `com.android.nls.routine.utils`: `Constants` (all table/column names, message strings, tracker/bank identifiers), `Common` (date/time helpers), `BottomNavHelper`.

### Data Flow
1.  **Expenses**: `NotificationListener` $\rightarrow$ `BankDetector` $\rightarrow$ `Parser` (per bank) $\rightarrow$ `HomeCardExpenseService.saveExpenseTest()` $\rightarrow$ `ExpenseRepository` $\rightarrow$ SQLite `EXPENSE_TEST`.
2.  **Home cards**: `HomeActivity` $\rightarrow$ `HomeCardWaterService` / `HomeCardMealService` / `HomeCardExpenseService` / `HomeService` $\rightarrow$ `Repository` $\rightarrow$ SQLite. The card list is built dynamically from `TrackerRepository.getEnabledTrackers()`. Tapping the header of the water, meal, expense, workout, medication or supplement card opens the shared history panel (`CardHistoryDialog`, in the `cardhistory` package).
3.  **History**: `HistoryActivity` $\rightarrow$ `HistoryService` (+ `HistoryContext`) $\rightarrow$ Repositories $\rightarrow$ SQLite $\rightarrow$ `DayScore.compute()` per day $\rightarrow$ `HistoryCalendarRenderer` (grid cells) and `DayDetailsRenderer` (detail grid).
4.  **Widget**: tapping a water button of the widget $\rightarrow$ `WidgetCombinedProvider` (the tap arrives as a `PendingIntent` broadcast carrying the button index) $\rightarrow$ `HomeCardWaterService.addWater()` $\rightarrow$ `WaterRepository` $\rightarrow$ SQLite, then the provider repaints every placed widget (water total, goal and the three configured values).
5.  **Meal widget**: tapping a status button of the widget $\rightarrow$ `WidgetCombinedProvider` (the tap arrives as a `PendingIntent` broadcast carrying the status) $\rightarrow$ `HomeCardMealService.saveQuickMeal()` $\rightarrow$ `MealRepository` $\rightarrow$ SQLite (the slot is the one matching the current time of day, and a slot already logged today is updated in place), then the provider repaints the four header segments and the day's `x/4` count.

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
- **`HomeCardWaterService` / `HomeCardMealService` / `HomeCardExpenseService`**: Per-card business logic for the home dashboard; each one implements the `CardHistory` contract (`cardhistory` package) and opens its own panel. `HomeService` computes which trackers are completed today (water goal, all 4 meals logged, routines done).
- **`ConfigService`**: Backs `ConfigActivity` (goal/button/limit dialogs, cards dialog, notification access shortcut).
- **`WidgetCombinedProvider`**: The combined water + meal + expense card on the home screen (`layout/widget_combined.xml`, registered by `xml/widget_combined_info.xml`). Both sections keep the same pattern: the icon alone on the left, with the progress component, the numbers and the button row grouped in the same box on its right, separated by a view line. The water header shows today's total against the daily goal (green once the goal is reached) and a progress bar, above the three quick-add buttons with the values configured in Config. The meal row shows one button per regular slot, gray while the slot is empty and tinted with the status color of the meal already logged today (green/yellow/red). Tapping a meal button swaps that row to the Correct / Warning / Wrong chooser of the tapped slot, and choosing a status logs it and brings the four meal buttons back with the slot now colored; the chooser is not persisted, so any refresh returns the row to the four buttons. A tap always logs the tapped slot, never the slot of the current time of day (the hour only pre-selects the slot in the in-app dialog). A completed section collapses: once the daily water goal is reached or the four regular meals are logged, the progress component/texts and the buttons are hidden and a compact line with a check takes their place (`3000 ml completed` / `4 meals added`). A widget runs in the launcher, so there is no click listener: each button is a `TextView` with a `PendingIntent` broadcast to the provider carrying its index or status, and the water amount is read from the config when the tap arrives (never a stale value). Adding water goes through `HomeCardWaterService` and logging a meal through `HomeCardMealService.saveQuickMeal()`, the same services behind the home cards (the meal slot is the tapped one, and a slot already logged today is updated in place, keeping an observation already written - the "meal already exists" prompt of the dialog has no widget equivalent). Since the widget has nowhere to ask for an observation, a warning/wrong meal saved there is marked with `Constants.WIDGET_MEAL_OBSERVATION` (`WIDGET`) as its observation when it has none; the marker is never shown to the user (it reads as an empty observation in the history panel note and in its editor) and `HomeCardMealService.showMissingObservationPrompt()` - called by `HomeActivity` on resume - offers, one at a time and once per visit to the screen, to describe those meals through `CardRecordDialog.showEditTextDialog()`, the status and the time of the meal being kept. A correct meal drops the marker, and a meal left for later ("not now") stops the offer and can still be described in its history panel. The static `refresh(context)` repaints every placed widget and is called by the provider itself (after water is added or a meal is saved), by `HomeActivity` (resume, water added, meal saved, history panel closed) and by the config dialogs; `updatePeriodMillis` (30 min, the smallest interval allowed) also keeps both sections fresh, which is what rolls them over to the new day.
- **`CardHistoryDialog` + `CardHistory` + `CardRecordDialog`**: The history panel opened from a home card. `CardHistoryDialog` renders the whole panel (`dialog_card_history.xml` chrome + one `item_history_record.xml` per record: empty state, height-capped scrolling list, total line, the optional add button under the title and optional edit/remove actions) and is shared by every card. Each card service implements `CardHistory` to answer with its title, empty text, row icon/tints and its rows through `getHistoryPanel(refresh)`, calling the `refresh` handle it receives after adding, editing or removing a record so the panel and the card behind it stay in sync. The add button is the optional `getHistoryAddAction(refresh)`, answered only by the expense card so far: it opens the same dialog as the edit action of a row (`showEditOptionAmountDialog`, bank + value), which stores a new expense with the current time or updates the record being edited, bank included; the banks offered are the configured ones, or the known banks (`Constants.KNOWN_BANK_LABELS`) when there is no card configured. `CardRecordDialog` is the sibling renderer that owns the per-record dialogs (`showEditAmountDialog`, `showEditOptionTextDialog`, `showEditOptionAmountDialog` and `confirmRemove`): a card passes its texts and returns the error to show, or null when it accepted and stored the amount, so validation and persistence stay in the card service. The simple cards (workout, medication, supplement) have a **single value per day**, so their panel is the one dynamic implementation `DynamicCardHistory`, built per open with the `Tracker` of the card: at most one row with the hour and `Completed` / `Not completed`, the configured name as detail and the description as note, no total line and no editor - only removing the mark, which returns the card to "not completed" (`TrackerRepository.deleteTrackerRecord()`). The whole panel lives in `com.android.nls.routine.cardhistory`, together with `DynamicCardHistoryService`: the single service behind the simple cards (workout, medication, supplement), which owns the `TrackerRepository` and receives the card `Tracker` - type, name and description - as the parameter that makes the difference between them.

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
- Meals logged **from the widget** have no dialog to ask for an observation: a warning/wrong meal saved without one is stored with the `WIDGET` marker (`Constants.WIDGET_MEAL_OBSERVATION`), which is never displayed as a note, and `HomeActivity` offers to describe those meals when it resumes (one at a time, once per visit, through `CardRecordDialog.showEditTextDialog()`; the status and the time of the meal are kept).

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
- A card history panel is added by implementing `CardHistory` in the card service - or, when several cards list the same shape of data, in one implementation they share, as `DynamicCardHistory` does for the simple cards - never by building another dialog: `CardHistoryDialog` already owns the shared panel and `CardRecordDialog` owns the per-record edit/remove dialogs. All of them live in `com.android.nls.routine.cardhistory`, so a new card only imports that package.
- Use `Constants` for table and column names to avoid typos.
- Ensure `NotificationListener` permissions are handled gracefully (the Config screen offers the system settings shortcut).
- Balance every `DatabaseHelper.acquire()` with a `release()` (usually via the repository's `closeDb()`), otherwise the database never closes.
- A home screen widget is an `AppWidgetProvider` in `com.android.nls.routine.widget`, rendered through `RemoteViews`: only framework views and concrete resources (a widget is inflated by the launcher, so no Material components and no `?attr/...` theme attributes in its layout), and every tap is a `PendingIntent` broadcast back to the provider instead of a click listener. The widget reads and writes through the same `Service`/`Repository` layers as the screen it mirrors, opening and closing them around each use, and its "size" lives in the `xml/widget_*_info.xml` file (`targetCellWidth`/`targetCellHeight` plus the matching `minWidth`/`minHeight` in dp).
- The expense card layout (`layout/card_expense.xml`) uses a `MaterialCardView` with a header that triggers the history panel, a `RelativeLayout` for total spent and trace values, and a `cardProgressContainer` where one progress bar per configured card is dynamically inflated.
