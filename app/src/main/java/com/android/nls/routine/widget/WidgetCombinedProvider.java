package com.android.nls.routine.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
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
 * on top of the meal card (the meal icon, one segment per daily slot with the
 * meals already logged, the day's "x/4" count and the Correct / Warning / Wrong
 * status buttons) and the expenses card on top (a read-only summary: icon, the
 * last expense description and the total spent so far this cycle), all in a
 * single widget so they can be seen without opening the app.
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
 * config when the tap arrives, so a button never adds a stale value. Water goes
 * through {@link HomeCardWaterService} and meals through
 * {@link HomeCardMealService}, the same services behind the home cards. Adding
 * water or logging a meal repaints every placed widget:
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
    public static final String ACTION_SAVE_MEAL = "com.android.nls.routine.action.widget.SAVE_MEAL";
    private static final String EXTRA_MEAL_STATUS = "extra_meal_status";
    private static final String[] MEAL_STATUSES = {
            Constants.CORRECT_MEAL,
            Constants.WARNING_MEAL,
            Constants.WRONG_MEAL
    };
    private static final int[] MEAL_BUTTON_IDS = {
            R.id.btnWidgetCorrectMeal,
            R.id.btnWidgetWarningMeal,
            R.id.btnWidgetWrongMeal
    };
    private static final int MEAL_REQUEST_CODE_OFFSET = 100;

    //  One segment of the meal header per daily slot, left to right
    private static final int[] MEAL_SEGMENT_IDS = {
            R.id.imgWidgetMealSegment1,
            R.id.imgWidgetMealSegment2,
            R.id.imgWidgetMealSegment3,
            R.id.imgWidgetMealSegment4
    };

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            appWidgetManager.updateAppWidget(appWidgetId, buildRemoteViews(context));
        }
    }

    @Override
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager appWidgetManager,
                                          int appWidgetId, Bundle newOptions) {
        appWidgetManager.updateAppWidget(appWidgetId, buildRemoteViews(context));
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        if (ACTION_ADD_WATER.equals(action)) {
            addWater(context, intent.getIntExtra(EXTRA_BUTTON_INDEX, -1));
            return;
        }

        if (ACTION_SAVE_MEAL.equals(action)) {
            String mealStatus = intent.getStringExtra(EXTRA_MEAL_STATUS);
            if (mealStatus != null) {
                saveMeal(context, mealStatus);
            }
            return;
        }

        super.onReceive(context, intent);
    }

    public static void refresh(Context context) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        ComponentName provider = new ComponentName(context, WidgetCombinedProvider.class);

        if (appWidgetManager.getAppWidgetIds(provider).length == 0) {
            return;
        }

        appWidgetManager.updateAppWidget(provider, buildRemoteViews(context));
    }

    /**
     * Builds the combined widget RemoteViews: binds the six pending intents
     * (three water quick-add buttons and three meal status buttons) and reads
     * from the database everything that can change: today's water total, goal
     * and configured button values, plus the meal slots already logged today
     * (the four segments of the meal header and its "x/4" count).
     */
    private static RemoteViews buildRemoteViews(Context context) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_water_meal);

        // Water buttons
        for (int i = 1; i <= BUTTON_COUNT; i++) {
            views.setOnClickPendingIntent(WATER_BUTTON_IDS[i - 1], buildAddWaterPendingIntent(context, i));
        }

        HomeCardWaterService homeCardWaterService = new HomeCardWaterService(context);
        try {
            WaterWidgetData waterData = homeCardWaterService.getWidgetData(context);

            views.setTextViewText(R.id.txtWidgetWaterDrank, waterData.totalText());
            views.setTextColor(R.id.txtWidgetWaterDrank, waterData.totalColor());
            views.setTextViewText(R.id.txtWidgetWaterGoal, waterData.goalText());
            views.setProgressBar(R.id.progressWidgetWater, PROGRESS_MAX,
                    waterData.progressPercentage(), false);

            for (int i = 0; i < BUTTON_COUNT; i++) {
                views.setTextViewText(WATER_BUTTON_IDS[i], waterData.buttonLabels()[i]);
            }
        } finally {
            homeCardWaterService.closeDb();
        }

        // Meal buttons
        for (int i = 1; i <= MEAL_STATUSES.length; i++) {
            views.setOnClickPendingIntent(MEAL_BUTTON_IDS[i - 1], buildSaveMealPendingIntent(context, i));
        }

        // Meal header: one segment per daily slot and the day's "x/4" count
        HomeCardMealService homeCardMealService = new HomeCardMealService(context);
        try {
            MealWidgetData mealData = homeCardMealService.getWidgetData(context);

            for (int i = 0; i < MEAL_SEGMENT_IDS.length; i++) {
                views.setImageViewResource(MEAL_SEGMENT_IDS[i], mealData.segmentDrawables()[i]);
            }

            views.setTextViewText(R.id.txtWidgetMealCount, mealData.countText());
            views.setTextColor(R.id.txtWidgetMealCount, mealData.countColor());
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
     * One pending intent per meal button (the request code is offset by 100 so it
     * never clashes with water intents), so each button keeps its own status.
     * Any widget instance can share it, since logging a meal only depends on the
     * tapped button, not on the widget id.
     */
    private static PendingIntent buildSaveMealPendingIntent(Context context, int buttonIndex) {
        Intent intent = new Intent(context, WidgetCombinedProvider.class)
                .setAction(ACTION_SAVE_MEAL)
                .putExtra(EXTRA_MEAL_STATUS, MEAL_STATUSES[buttonIndex - 1]);

        return PendingIntent.getBroadcast(context, MEAL_REQUEST_CODE_OFFSET + buttonIndex, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    /**
     * Logs the tapped status for the meal slot of the current time of day and
     * repaints the widgets, so the segments and the "x/4" count update at once.
     */
    private static void saveMeal(Context context, String mealStatus) {
        if (!isKnownStatus(mealStatus)) {
            Log.e(TAG, "Unknown meal widget status: " + mealStatus);
            return;
        }

        HomeCardMealService homeCardMealService = new HomeCardMealService(context);

        try {
            homeCardMealService.saveQuickMeal(mealStatus);
        } finally {
            homeCardMealService.closeDb();
        }

        refresh(context);
    }

    private static boolean isKnownStatus(String mealStatus) {
        for (String status : MEAL_STATUSES) {
            if (status.equals(mealStatus)) {
                return true;
            }
        }
        return false;
    }
}