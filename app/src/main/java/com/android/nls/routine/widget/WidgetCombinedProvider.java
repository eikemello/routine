package com.android.nls.routine.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;
import com.android.nls.routine.R;
import com.android.nls.routine.model.ExpenseWidgetData;
import com.android.nls.routine.model.MealWidgetData;
import com.android.nls.routine.model.WaterWidgetData;
import com.android.nls.routine.service.HomeCardExpenseService;
import com.android.nls.routine.service.HomeCardMealService;
import com.android.nls.routine.service.HomeCardWaterService;
import com.android.nls.routine.utils.Common;
import com.android.nls.routine.utils.Constants;

/**
 * Combined home screen widget: the water card (three quick-add buttons with the
 * configured values, today's total against the daily goal, progress bar) stacked
 * on top of the meal card (the meal icon and one button per regular slot -
 * gray while the slot is empty, tinted with the status color of the meal
 * already logged today) and the expenses card on top (a
 * read-only summary: icon, the last expense description and the total spent so
 * far this cycle), all in a single widget so they can be seen without opening
 * the app.
 * <p>
 * A completed section collapses: once the daily water goal is reached or the
 * four regular meals are logged, its progress component and buttons are hidden
 * and a compact line with a check takes their place ("3000 ml completed",
 * "4 meals added"), so the widget keeps only what still needs attention.
 * <p>
 * The three sections keep the same pattern: the icon alone on the left, with the
 * progress component, the numbers and the button row grouped in the same box on
 * its right, the water and meal sections separated from each other and the meal
 * and expenses sections separated from each other by a view line. The expenses
 * card has no buttons at all - it does not log, edit or remove expenses (there is
 * no PendingIntent for it), it just reads the current expense totals each time and
 * repaints so the widget stays in sync with the home card behind it.
 * <p>
 * A widget is rendered through RemoteViews, so a button cannot have a click
 * listener: each one carries a PendingIntent back to this provider instead,
 * with its index or status as an extra. The water amounts are read from the
 * config when the tap arrives, so a button never adds a stale value. Tapping a
 * meal button repaints the widget with the Correct / Warning / Wrong buttons of
 * that slot, and tapping a status logs it and brings the four meal buttons
 * back - a two-tap flow that always logs the tapped slot, not the slot of the
 * current time of day. Water goes through {@link HomeCardWaterService} and
 * meals through {@link HomeCardMealService}, the same services behind the home
 * cards. Adding water or logging a meal repaints every placed widget:
 * {@link #refresh(Context)} is called by the provider itself, by HomeActivity
 * (resume, water added, meal saved, history panel closed) and by the config
 * dialogs; updatePeriodMillis (30 min, the smallest interval allowed) also
 * keeps both sections fresh, which is what rolls them over to the new day.
 */
public class WidgetCombinedProvider extends AppWidgetProvider {
    private static final String TAG = Common.generateTag(WidgetCombinedProvider.class);

    // ===== WATER =====
    public static final String ACTION_ADD_WATER = "com.android.nls.routine.action.widget.ADD_WATER";
    private static final String EXTRA_BUTTON_INDEX = "extra_button_index";
    private static final int BUTTON_COUNT = 3;
    private static final int PROGRESS_MAX = 100;
    private static final int[] WATER_BUTTON_IDS = {
            R.id.btnWidgetAddWater1,
            R.id.btnWidgetAddWater2,
            R.id.btnWidgetAddWater3
    };

    // ===== MEAL =====
    public static final String ACTION_SELECT_MEAL = "com.android.nls.routine.action.widget.SELECT_MEAL";
    public static final String ACTION_SAVE_MEAL = "com.android.nls.routine.action.widget.SAVE_MEAL";
    private static final String EXTRA_APP_WIDGET_ID = "extra_app_widget_id";
    private static final String EXTRA_MEAL_INDEX = "extra_meal_index";
    private static final String EXTRA_MEAL_STATUS = "extra_meal_status";
    private static final String[] REGULAR_MEALS = {
            Constants.BREAKFAST,
            Constants.LUNCH,
            Constants.TEA,
            Constants.DINNER
    };
    private static final String[] MEAL_STATUSES = {
            Constants.CORRECT_MEAL,
            Constants.WARNING_MEAL,
            Constants.WRONG_MEAL
    };
    private static final int[] MEAL_BUTTON_IDS = {
            R.id.btnWidgetMealBreakfast,
            R.id.btnWidgetMealLunch,
            R.id.btnWidgetMealTea,
            R.id.btnWidgetMealDinner
    };
    private static final int[] MEAL_STATUS_BUTTON_IDS = {
            R.id.btnWidgetCorrectMeal,
            R.id.btnWidgetWarningMeal,
            R.id.btnWidgetWrongMeal
    };
    /** No meal slot chosen: the row shows the four meal buttons. */
    private static final int NO_MEAL_SELECTED = -1;

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            appWidgetManager.updateAppWidget(appWidgetId,
                    buildRemoteViews(context, appWidgetId, NO_MEAL_SELECTED));
        }
    }

    @Override
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager appWidgetManager,
                                          int appWidgetId, Bundle newOptions) {
        appWidgetManager.updateAppWidget(appWidgetId,
                buildRemoteViews(context, appWidgetId, NO_MEAL_SELECTED));
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        if (ACTION_ADD_WATER.equals(action)) {
            addWater(context, intent.getIntExtra(EXTRA_BUTTON_INDEX, -1));
            return;
        }

        if (ACTION_SELECT_MEAL.equals(action)) {
            selectMeal(context, intent.getIntExtra(EXTRA_APP_WIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID),
                    intent.getIntExtra(EXTRA_MEAL_INDEX, NO_MEAL_SELECTED));
            return;
        }

        if (ACTION_SAVE_MEAL.equals(action)) {
            String mealStatus = intent.getStringExtra(EXTRA_MEAL_STATUS);
            if (mealStatus != null) {
                saveMeal(context, intent.getIntExtra(EXTRA_MEAL_INDEX, NO_MEAL_SELECTED), mealStatus);
            }
            return;
        }

        super.onReceive(context, intent);
    }

    /**
     * Repaints every placed widget in its default state - always the four meal
     * buttons, never a half-finished chooser - so any refresh also gives up a
     * meal selection left behind.
     */
    public static void refresh(Context context) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        ComponentName provider = new ComponentName(context, WidgetCombinedProvider.class);

        for (int appWidgetId : appWidgetManager.getAppWidgetIds(provider)) {
            appWidgetManager.updateAppWidget(appWidgetId,
                    buildRemoteViews(context, appWidgetId, NO_MEAL_SELECTED));
        }
    }

    /**
     * Builds the combined widget RemoteViews: binds the pending intents (the
     * three water quick-add buttons plus the meal buttons, which are built per
     * widget id) and reads from the database everything that can change:
     * today's water total, goal and configured button values, the status of
     * each meal slot already logged, and the current expense summary (last
     * expense and total spent). The water and
     * meal sections are also switched here between their normal and completed
     * states, and the meal row between the four meal buttons and the status
     * chooser of selectedMealIndex (-1 when none).
     */
    private static RemoteViews buildRemoteViews(Context context, int appWidgetId, int selectedMealIndex) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_combined);

        // Water buttons
        for (int i = 1; i <= BUTTON_COUNT; i++) {
            views.setOnClickPendingIntent(WATER_BUTTON_IDS[i - 1], buildAddWaterPendingIntent(context, i));
        }

        HomeCardWaterService homeCardWaterService = new HomeCardWaterService(context);
        try {
            WaterWidgetData waterData = homeCardWaterService.getWidgetData(context);
            boolean waterCompleted = waterData.goalReached();

            // The section collapses to the completed line once the goal is reached
            views.setViewVisibility(R.id.layoutWidgetWaterProgress, waterCompleted ? View.GONE : View.VISIBLE);
            views.setViewVisibility(R.id.layoutWidgetWaterButtons, waterCompleted ? View.GONE : View.VISIBLE);
            views.setViewVisibility(R.id.layoutWidgetWaterCompleted, waterCompleted ? View.VISIBLE : View.GONE);

            if (waterCompleted) {
                views.setTextViewText(R.id.txtWidgetWaterCompleted, waterData.completedText());
            } else {
                views.setTextViewText(R.id.txtWidgetWaterDrank, waterData.totalText());
                views.setTextColor(R.id.txtWidgetWaterDrank, waterData.totalColor());
                views.setTextViewText(R.id.txtWidgetWaterGoal, waterData.goalText());
                views.setProgressBar(R.id.progressWidgetWater, PROGRESS_MAX,
                        waterData.progressPercentage(), false);

                for (int i = 0; i < BUTTON_COUNT; i++) {
                    views.setTextViewText(WATER_BUTTON_IDS[i], waterData.buttonLabels()[i]);
                }
            }
        } finally {
            homeCardWaterService.closeDb();
        }

        // Meal row: the four buttons, or the status chooser of the tapped slot
        HomeCardMealService homeCardMealService = new HomeCardMealService(context);
        try {
            MealWidgetData mealData = homeCardMealService.getWidgetData(context);
            boolean mealsCompleted = mealData.allMealsLogged();
            boolean choosingMeal = !mealsCompleted && selectedMealIndex != NO_MEAL_SELECTED;

            // The section collapses to the completed line once the four slots are logged
            views.setViewVisibility(R.id.layoutWidgetMealMeals, mealsCompleted || choosingMeal ? View.GONE : View.VISIBLE);
            views.setViewVisibility(R.id.layoutWidgetMealStatuses, choosingMeal ? View.VISIBLE : View.GONE);
            views.setViewVisibility(R.id.layoutWidgetMealCompleted, mealsCompleted ? View.VISIBLE : View.GONE);

            if (mealsCompleted) {
                views.setTextViewText(R.id.txtWidgetMealCompleted, mealData.completedText());
            } else if (choosingMeal) {
                paintMealStatusButtons(context, views, appWidgetId, selectedMealIndex);
            } else {
                paintMealButtons(context, views, appWidgetId, mealData.mealStatuses());
            }
        } finally {
            homeCardMealService.closeDb();
        }

        // Expense summary
        HomeCardExpenseService homeCardExpenseService = new HomeCardExpenseService(context);
        try {
            ExpenseWidgetData expenseData = homeCardExpenseService.getWidgetData(context);
            views.setTextViewText(R.id.txtWidgetExpenseLast, expenseData.lastExpenseText());
            views.setTextViewText(R.id.txtWidgetExpenseTotal, expenseData.totalSpentText());
        } finally {
            homeCardExpenseService.closeDb();
        }

        return views;
    }

    private static PendingIntent buildAddWaterPendingIntent(Context context, int buttonIndex) {
        Intent intent = new Intent(context, WidgetCombinedProvider.class)
                .setAction(ACTION_ADD_WATER)
                .putExtra(EXTRA_BUTTON_INDEX, buttonIndex);

        return PendingIntent.getBroadcast(context, buttonIndex, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    //Adds the amount configured for the tapped water button and repaints the widgets
    private static void addWater(Context context, int buttonIndex) {
        if (buttonIndex < 1 || buttonIndex > BUTTON_COUNT) {
            Log.e(TAG, "Unknown water widget button: " + buttonIndex);
            return;
        }

        HomeCardWaterService homeCardWaterService = new HomeCardWaterService(context);

        try {
            double amount = switch (buttonIndex) {
                case 1 -> homeCardWaterService.getDefaultValueBtn1();
                case 2 -> homeCardWaterService.getDefaultValueBtn2();
                case 3  -> homeCardWaterService.getDefaultValueBtn3();
                default -> throw new IllegalStateException("Unexpected value: " + buttonIndex);
            };
            homeCardWaterService.addWater(String.valueOf(amount));
            Log.d(TAG, "Added " + amount + "ml from widget button " + buttonIndex);
        } finally {
            homeCardWaterService.closeDb();
        }

        refresh(context);
    }

    /**
     * One pending intent per (widget, slot): tapping a meal button repaints the
     * widget with the status chooser of that slot. The data URI carries the
     * widget id and the slot, so the intents of different widgets and slots
     * never clash even though the request code repeats.
     */
    private static PendingIntent buildSelectMealPendingIntent(Context context, int appWidgetId, int mealIndex) {
        Intent intent = new Intent(context, WidgetCombinedProvider.class)
                .setAction(ACTION_SELECT_MEAL)
                .setData(Uri.parse("routine://widget/" + appWidgetId + "/meal/" + mealIndex))
                .putExtra(EXTRA_APP_WIDGET_ID, appWidgetId)
                .putExtra(EXTRA_MEAL_INDEX, mealIndex);

        return PendingIntent.getBroadcast(context, mealIndex, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    /**
     * One pending intent per (widget, slot, status): tapping a status logs the
     * chosen slot with it and brings the four meal buttons back, the slot now
     * tinted with its color.
     */
    private static PendingIntent buildSaveMealPendingIntent(Context context, int appWidgetId, int mealIndex,
                                                            int statusIndex) {
        String mealStatus = MEAL_STATUSES[statusIndex];
        Intent intent = new Intent(context, WidgetCombinedProvider.class)
                .setAction(ACTION_SAVE_MEAL)
                .setData(Uri.parse("routine://widget/" + appWidgetId + "/meal/" + mealIndex + "/" + mealStatus))
                .putExtra(EXTRA_MEAL_INDEX, mealIndex)
                .putExtra(EXTRA_MEAL_STATUS, mealStatus);

        return PendingIntent.getBroadcast(context, statusIndex, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    /**
     * Paints the four meal buttons: gray while the slot is empty, and tinted
     * with the status color of the meal already logged today - text and
     * background alike (green for correct, yellow for warning, red for wrong).
     */
    private static void paintMealButtons(Context context, RemoteViews views, int appWidgetId,
                                         String[] mealStatuses) {
        for (int i = 0; i < MEAL_BUTTON_IDS.length; i++) {
            String status = mealStatuses[i] == null ? "" : mealStatuses[i];
            int textColorRes;
            int backgroundRes;

            switch (status) {
                case Constants.CORRECT_MEAL -> {
                    textColorRes = R.color.green_dark;
                    backgroundRes = R.drawable.widget_button_correct;
                }
                case Constants.WARNING_MEAL -> {
                    textColorRes = R.color.yellow_dark;
                    backgroundRes = R.drawable.widget_button_warning;
                }
                case Constants.WRONG_MEAL -> {
                    textColorRes = R.color.red_dark;
                    backgroundRes = R.drawable.widget_button_wrong;
                }
                default -> {
                    textColorRes = R.color.text_secondary;
                    backgroundRes = R.drawable.widget_button_background;
                }
            }

            views.setTextColor(MEAL_BUTTON_IDS[i], context.getColor(textColorRes));
            views.setInt(MEAL_BUTTON_IDS[i], "setBackgroundResource", backgroundRes);
            views.setOnClickPendingIntent(MEAL_BUTTON_IDS[i],
                    buildSelectMealPendingIntent(context, appWidgetId, i));
        }
    }

    /** Binds the Correct / Warning / Wrong buttons of the slot being logged. */
    private static void paintMealStatusButtons(Context context, RemoteViews views, int appWidgetId, int mealIndex) {
        for (int i = 0; i < MEAL_STATUS_BUTTON_IDS.length; i++) {
            views.setOnClickPendingIntent(MEAL_STATUS_BUTTON_IDS[i],
                    buildSaveMealPendingIntent(context, appWidgetId, mealIndex, i));
        }
    }

    /**
     * Switches the tapped widget's row to the Correct / Warning / Wrong buttons
     * for the tapped slot; the other widgets keep (or return to) the four meal
     * buttons. The selection lives only in this repaint: the next refresh of
     * any kind brings every widget back to the four buttons, so a chooser left
     * behind never survives a repaint.
     */
    private static void selectMeal(Context context, int appWidgetId, int mealIndex) {
        if (isUnknownMealIndex(mealIndex)) {
            Log.e(TAG, "Unknown meal slot: " + mealIndex);
            return;
        }

        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        ComponentName provider = new ComponentName(context, WidgetCombinedProvider.class);

        for (int id : appWidgetManager.getAppWidgetIds(provider)) {
            appWidgetManager.updateAppWidget(id,
                    buildRemoteViews(context, id, id == appWidgetId ? mealIndex : NO_MEAL_SELECTED));
        }
    }

    /**
     * Logs the tapped status for the chosen slot and repaints the widgets, so
     * the four meal buttons come back with the slot tinted with its color.
     */
    private static void saveMeal(Context context, int mealIndex, String mealStatus) {
        if (isUnknownMealIndex(mealIndex) || isUnknownStatus(mealStatus)) {
            Log.e(TAG, "Unknown meal widget tap: slot " + mealIndex + ", status " + mealStatus);
            return;
        }

        HomeCardMealService homeCardMealService = new HomeCardMealService(context);

        try {
            homeCardMealService.saveQuickMeal(mealStatus, REGULAR_MEALS[mealIndex]);
        } finally {
            homeCardMealService.closeDb();
        }

        refresh(context);
    }

    /** True when the index does not match one of the four regular meal slots. */
    private static boolean isUnknownMealIndex(int mealIndex) {
        return mealIndex < 0 || mealIndex >= REGULAR_MEALS.length;
    }

    /** True when the status is not one of Correct / Warning / Wrong. */
    private static boolean isUnknownStatus(String mealStatus) {
        for (String status : MEAL_STATUSES) {
            if (status.equals(mealStatus)) {
                return false;
            }
        }
        return true;
    }
}