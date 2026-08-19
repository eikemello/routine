package com.android.nls.routine.service;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import com.android.nls.routine.model.DayDetails;
import com.android.nls.routine.model.DayStatus;
import com.android.nls.routine.model.ExpenseRecord;
import com.android.nls.routine.model.MealRecord;
import com.android.nls.routine.model.TrackerRecord;
import com.android.nls.routine.model.TrackerType;
import com.android.nls.routine.model.WaterRecord;
import com.android.nls.routine.model.WeeklySummary;
import com.android.nls.routine.service.database.DatabaseHelper;
import com.android.nls.routine.utils.Common;
import com.android.nls.routine.utils.Constants;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

public class HistoryService {
    private static final String TAG = Common.generateTag(HistoryService.class);
    private final DatabaseHelper mDatabaseHelper;
    private final SQLiteDatabase mSqliteDatabase;
    private final ConfigService mConfigService;

    public HistoryService(Context context) {
        mDatabaseHelper = new DatabaseHelper(context);
        mSqliteDatabase = mDatabaseHelper.getWritableDatabase();
        mConfigService = new ConfigService(context);
    }

    public WeeklySummary getWeeklySummary() {
        long startOfWeek = Common.getStartOfWeekInMillis();
        long endOfWeek = Common.getEndOfWeekInMillis();

        int waterDaysAchieved = countWaterDaysAchieved(startOfWeek, endOfWeek);
        int totalDays = getDaysElapsedInWeek();
        double totalSpent = getTotalSpent(startOfWeek, endOfWeek);
        int[] mealCounts = getMealCounts(startOfWeek, endOfWeek);

        return new WeeklySummary(
                waterDaysAchieved,
                totalDays,
                totalSpent,
                mealCounts[0],
                mealCounts[1],
                mealCounts[2]
        );
    }

    public DayDetails getDayDetails(long timestamp) {
        long startOfDay = Common.getStartOfDayInMillis(timestamp);
        long endOfDay = Common.getEndOfDayInMillis(timestamp);

        List<WaterRecord> waterRecords = getWaterRecords(startOfDay, endOfDay);
        List<MealRecord> mealRecords = getMealRecords(startOfDay, endOfDay);
        List<ExpenseRecord> expenseRecords = getExpenseRecords(startOfDay, endOfDay);
        List<TrackerRecord> workoutRecords = getTrackerRecords(TrackerType.WORKOUT, startOfDay, endOfDay);
        List<TrackerRecord> medicationRecords = getTrackerRecords(TrackerType.MEDICATION, startOfDay, endOfDay);
        List<TrackerRecord> supplementRecords = getTrackerRecords(TrackerType.SUPPLEMENT, startOfDay, endOfDay);

        return new DayDetails(waterRecords, mealRecords, expenseRecords, workoutRecords, medicationRecords, supplementRecords);
    }

    public boolean hasDataOnDay(long timestamp) {
        long startOfDay = Common.getStartOfDayInMillis(timestamp);
        long endOfDay = Common.getEndOfDayInMillis(timestamp);

        return hasWaterData(startOfDay, endOfDay)
                || hasMealData(startOfDay, endOfDay)
                || hasExpenseData(startOfDay, endOfDay)
                || hasTrackerData(TrackerType.WORKOUT, startOfDay, endOfDay)
                || hasTrackerData(TrackerType.MEDICATION, startOfDay, endOfDay)
                || hasTrackerData(TrackerType.SUPPLEMENT, startOfDay, endOfDay);
    }

    public DayStatus getDayStatus(long timestamp) {
        long startOfDay = Common.getStartOfDayInMillis(timestamp);
        long endOfDay = Common.getEndOfDayInMillis(timestamp);

        int waterSum = getWaterSum(startOfDay, endOfDay);
        int dailyGoal = Integer.parseInt(mConfigService.getDailyWaterGoal());
        boolean waterAchieved = waterSum >= dailyGoal;

        int[] mealCounts = getMealCounts(startOfDay, endOfDay);
        int correctMeals = mealCounts[0];
        int warningMeals = mealCounts[1];
        int wrongMeals = mealCounts[2];

        // If there's no data at all (no water, no meals), don't color the day
        if (waterSum == 0 && correctMeals == 0 && warningMeals == 0 && wrongMeals == 0) {
            return DayStatus.NONE;
        }

        // User's priority rules (preserved):
        //   RED: 2+ wrong meals
        //   YELLOW: 2+ warning meals OR 1 wrong meal
        if (wrongMeals >= 2) {
            return DayStatus.RED;
        }
        if (warningMeals >= 2 || wrongMeals >= 1) {
            return DayStatus.YELLOW;
        }

        // Scoring model for remaining cases:
        //   Achieved water goal  -> +2 points
        //   Each correct meal    -> +1 point
        //   Each warning meal    -> -0.5 points
        //   Each wrong meal      -> -1 point
        double score = 0;
        if (waterAchieved) {
            score += 2;
        }
        score += correctMeals;
        score -= warningMeals * 0.5;
        score -= wrongMeals;

        // GREEN: score >= 3
        if (score >= 3) {
            return DayStatus.GREEN;
        }

        // YELLOW: score between 0 and 2 (inclusive)
        if (score >= 0) {
            return DayStatus.YELLOW;
        }

        // Has data but doesn't meet green/yellow/red criteria
        if (waterSum > 0 || correctMeals > 0 || warningMeals > 0) {
            return DayStatus.NONE;
        }

        return DayStatus.NONE;
    }

    private int countWaterDaysAchieved(long startOfWeek, long endOfWeek) {
        int dailyGoal = Integer.parseInt(mConfigService.getDailyWaterGoal());
        int daysAchieved = 0;
        Calendar calendar = new GregorianCalendar();
        calendar.setTimeInMillis(startOfWeek);

        while (calendar.getTimeInMillis() <= endOfWeek) {
            long dayStart = calendar.getTimeInMillis();
            long dayEnd = Common.getEndOfDayInMillis(dayStart);
            int daySum = getWaterSum(dayStart, dayEnd);

            if (daySum >= dailyGoal) {
                daysAchieved++;
            }
            calendar.add(Calendar.DAY_OF_MONTH, 1);
        }

        return daysAchieved;
    }

    private int getDaysElapsedInWeek() {
        long startOfWeek = Common.getStartOfWeekInMillis();
        Calendar startCal = new GregorianCalendar();
        startCal.setTimeInMillis(startOfWeek);

        long now = System.currentTimeMillis();
        Calendar nowCal = new GregorianCalendar();
        nowCal.setTimeInMillis(now);

        int days = 0;

        while (startCal.getTimeInMillis() <= nowCal.getTimeInMillis()) {
            days++;
            startCal.add(Calendar.DAY_OF_MONTH, 1);
        }

        return days;
    }

    private int getWaterSum(long start, long end) {
        String query = "SELECT SUM(" + Constants.COLUMN_NAME_WATER_DRANK + ") FROM " + Constants.TABLE_NAME_WATER +
                " WHERE " + Constants.COLUMN_NAME_TIMESTAMP + " >= ? AND " +
                Constants.COLUMN_NAME_TIMESTAMP + " <= ?";

        try (Cursor cursor = mSqliteDatabase.rawQuery(query, new String[]{String.valueOf(start), String.valueOf(end)})) {
            if (cursor.moveToFirst() && cursor.getString(0) != null) {
                return cursor.getInt(0);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error calculating water sum: " + e.getMessage());
        }

        return 0;
    }

    private double getTotalSpent(long start, long end) {
        String query = "SELECT SUM(" + Constants.COLUMN_NAME_EXPENSE_VALUE + ") FROM " + Constants.TABLE_NAME_EXPENSE_TEST +
                " WHERE " + Constants.COLUMN_NAME_TIMESTAMP + " >= ? AND " +
                Constants.COLUMN_NAME_TIMESTAMP + " <= ?";

        try (Cursor cursor = mSqliteDatabase.rawQuery(query, new String[]{String.valueOf(start), String.valueOf(end)})) {
            if (cursor.moveToFirst() && cursor.getString(0) != null) {
                return cursor.getDouble(0);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error calculating expense sum: " + e.getMessage());
        }

        return 0.0;
    }

    private int[] getMealCounts(long start, long end) {
        int correct = 0;
        int warning = 0;
        int wrong = 0;

        String query = "SELECT " + Constants.COLUMN_NAME_MEAL_STATUS + " FROM " + Constants.TABLE_NAME_MEAL +
                " WHERE " + Constants.COLUMN_NAME_TIMESTAMP + " >= ? AND " +
                Constants.COLUMN_NAME_TIMESTAMP + " <= ?";

        try (Cursor cursor = mSqliteDatabase.rawQuery(query, new String[]{String.valueOf(start), String.valueOf(end)})) {
            while (cursor.moveToNext()) {
                String status = cursor.getString(0);
                if (Constants.CORRECT_MEAL.equals(status)) {
                    correct++;
                } else if (Constants.WARNING_MEAL.equals(status)) {
                    warning++;
                } else if (Constants.WRONG_MEAL.equals(status)) {
                    wrong++;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error counting meals: " + e.getMessage());
        }

        return new int[]{correct, warning, wrong};
    }

    private List<WaterRecord> getWaterRecords(long start, long end) {
        List<WaterRecord> records = new ArrayList<>();

        String query = "SELECT " + Constants.COLUMN_NAME_WATER_DRANK + ", " + Constants.COLUMN_NAME_TIMESTAMP +
                " FROM " + Constants.TABLE_NAME_WATER +
                " WHERE " + Constants.COLUMN_NAME_TIMESTAMP + " >= ? AND " +
                Constants.COLUMN_NAME_TIMESTAMP + " <= ?" +
                " ORDER BY " + Constants.COLUMN_NAME_TIMESTAMP + " ASC";

        try (Cursor cursor = mSqliteDatabase.rawQuery(query, new String[]{String.valueOf(start), String.valueOf(end)})) {
            while (cursor.moveToNext()) {
                int amount = cursor.getInt(0);
                long timestamp = cursor.getLong(1);
                records.add(new WaterRecord(amount, timestamp));
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting water records: " + e.getMessage());
        }

        return records;
    }

    private List<MealRecord> getMealRecords(long start, long end) {
        List<MealRecord> records = new ArrayList<>();

        String query = "SELECT " + Constants.COLUMN_NAME_MEAL_STATUS + ", " +
                Constants.COLUMN_NAME_MEAL + ", " +
                Constants.COLUMN_NAME_MEAL_OBS + ", " +
                Constants.COLUMN_NAME_TIMESTAMP +
                " FROM " + Constants.TABLE_NAME_MEAL +
                " WHERE " + Constants.COLUMN_NAME_TIMESTAMP + " >= ? AND " +
                Constants.COLUMN_NAME_TIMESTAMP + " <= ?" +
                " ORDER BY " + Constants.COLUMN_NAME_TIMESTAMP + " ASC";

        try (Cursor cursor = mSqliteDatabase.rawQuery(query, new String[]{String.valueOf(start), String.valueOf(end)})) {
            while (cursor.moveToNext()) {
                String status = cursor.getString(0);
                String meal = cursor.getString(1);
                String observation = cursor.getString(2);
                long timestamp = cursor.getLong(3);
                records.add(new MealRecord(status, meal, observation, timestamp));
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting meal records: " + e.getMessage());
        }

        return records;
    }

    private List<ExpenseRecord> getExpenseRecords(long start, long end) {
        List<ExpenseRecord> records = new ArrayList<>();

        String query = "SELECT " + Constants.COLUMN_NAME_EXPENSE_VALUE + ", " +
                Constants.COLUMN_NAME_EXPENSE_TEXT + ", " +
                Constants.COLUMN_NAME_BANK_NAME + ", " +
                Constants.COLUMN_NAME_TIMESTAMP +
                " FROM " + Constants.TABLE_NAME_EXPENSE_TEST +
                " WHERE " + Constants.COLUMN_NAME_TIMESTAMP + " >= ? AND " +
                Constants.COLUMN_NAME_TIMESTAMP + " <= ?" +
                " ORDER BY " + Constants.COLUMN_NAME_TIMESTAMP + " ASC";

        try (Cursor cursor = mSqliteDatabase.rawQuery(query, new String[]{String.valueOf(start), String.valueOf(end)})) {
            while (cursor.moveToNext()) {
                double amount = cursor.getDouble(0);
                String description = cursor.getString(1);
                String bank = cursor.getString(2);
                long timestamp = cursor.getLong(3);
                records.add(new ExpenseRecord(amount, description, bank, timestamp));
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting expense records: " + e.getMessage());
        }

        return records;
    }

    private boolean hasWaterData(long start, long end) {
        String query = "SELECT 1 FROM " + Constants.TABLE_NAME_WATER +
                " WHERE " + Constants.COLUMN_NAME_TIMESTAMP + " >= ? AND " +
                Constants.COLUMN_NAME_TIMESTAMP + " <= ? LIMIT 1";

        try (Cursor cursor = mSqliteDatabase.rawQuery(query, new String[]{String.valueOf(start), String.valueOf(end)})) {
            return cursor.moveToFirst();
        } catch (Exception e) {
            Log.e(TAG, "Error checking water data: " + e.getMessage());
        }

        return false;
    }

    private boolean hasMealData(long start, long end) {
        String query = "SELECT 1 FROM " + Constants.TABLE_NAME_MEAL +
                " WHERE " + Constants.COLUMN_NAME_TIMESTAMP + " >= ? AND " +
                Constants.COLUMN_NAME_TIMESTAMP + " <= ? LIMIT 1";

        try (Cursor cursor = mSqliteDatabase.rawQuery(query, new String[]{String.valueOf(start), String.valueOf(end)})) {
            return cursor.moveToFirst();
        } catch (Exception e) {
            Log.e(TAG, "Error checking meal data: " + e.getMessage());
        }

        return false;
    }

    private boolean hasExpenseData(long start, long end) {
        String query = "SELECT 1 FROM " + Constants.TABLE_NAME_EXPENSE_TEST +
                " WHERE " + Constants.COLUMN_NAME_TIMESTAMP + " >= ? AND " +
                Constants.COLUMN_NAME_TIMESTAMP + " <= ? LIMIT 1";

        try (Cursor cursor = mSqliteDatabase.rawQuery(query, new String[]{String.valueOf(start), String.valueOf(end)})) {
            return cursor.moveToFirst();
        } catch (Exception e) {
            Log.e(TAG, "Error checking expense data: " + e.getMessage());
        }

        return false;
    }

    private List<TrackerRecord> getTrackerRecords(TrackerType type, long start, long end) {
        List<TrackerRecord> records = new ArrayList<>();

        String query = "SELECT " + Constants.COLUMN_NAME_TRACKER_RECORD_TYPE + ", " +
                Constants.COLUMN_NAME_TRACKER_RECORD_COMPLETED + ", " +
                Constants.COLUMN_NAME_TRACKER_RECORD_NOTE + ", " +
                Constants.COLUMN_NAME_TRACKER_RECORD_TIMESTAMP +
                " FROM " + Constants.TABLE_NAME_TRACKER_RECORDS +
                " WHERE " + Constants.COLUMN_NAME_TRACKER_RECORD_TYPE + " = ? AND " +
                Constants.COLUMN_NAME_TRACKER_RECORD_TIMESTAMP + " >= ? AND " +
                Constants.COLUMN_NAME_TRACKER_RECORD_TIMESTAMP + " <= ?" +
                " ORDER BY " + Constants.COLUMN_NAME_TRACKER_RECORD_TIMESTAMP + " ASC";

        try (Cursor cursor = mSqliteDatabase.rawQuery(query,
                new String[]{type.name(), String.valueOf(start), String.valueOf(end)})) {
            while (cursor.moveToNext()) {
                String typeStr = cursor.getString(0);
                boolean completed = cursor.getInt(1) == 1;
                String note = cursor.getString(2);
                long timestamp = cursor.getLong(3);
                records.add(new TrackerRecord(0, TrackerType.valueOf(typeStr), completed, note, timestamp));
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting tracker records " + type + ": " + e.getMessage());
        }

        return records;
    }

    private boolean hasTrackerData(TrackerType type, long start, long end) {
        String query = "SELECT 1 FROM " + Constants.TABLE_NAME_TRACKER_RECORDS +
                " WHERE " + Constants.COLUMN_NAME_TRACKER_RECORD_TYPE + " = ? AND " +
                Constants.COLUMN_NAME_TRACKER_RECORD_TIMESTAMP + " >= ? AND " +
                Constants.COLUMN_NAME_TRACKER_RECORD_TIMESTAMP + " <= ? LIMIT 1";

        try (Cursor cursor = mSqliteDatabase.rawQuery(query,
                new String[]{type.name(), String.valueOf(start), String.valueOf(end)})) {
            return cursor.moveToFirst();
        } catch (Exception e) {
            Log.e(TAG, "Error checking tracker data " + type + ": " + e.getMessage());
        }

        return false;
    }

    public void closeDb() {
        mDatabaseHelper.close();
    }
}