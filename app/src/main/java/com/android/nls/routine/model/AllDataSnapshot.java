package com.android.nls.routine.model;

import java.util.List;

/**
 * Everything the All data screen shows: the whole record tables (already
 * ordered newest first), the tracker definitions, the configured cards and
 * the stored settings. Built by {@link com.android.nls.routine.service.AllDataService}.
 */
public record AllDataSnapshot(List<WaterRecord> waterRecords,
                              List<MealRecord> mealRecords,
                              List<ExpenseRecord> expenseRecords,
                              List<TrackerRecord> routineRecords,
                              List<Tracker> trackers,
                              List<CreditCard> cards,
                              double dailyWaterGoal,
                              double btn1Value,
                              double btn2Value,
                              double btn3Value,
                              double monthlyLimit,
                              double statementClosingDay,
                              double totalWater,
                              int totalMeals,
                              double totalExpenses,
                              int totalRoutines) {
}