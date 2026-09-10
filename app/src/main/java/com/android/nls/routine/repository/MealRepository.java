package com.android.nls.routine.repository;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import com.android.nls.routine.model.MealRecord;
import com.android.nls.routine.database.DatabaseHelper;
import com.android.nls.routine.utils.Common;
import com.android.nls.routine.utils.Constants;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MealRepository {
    private static final String TAG = Common.generateTag(MealRepository.class);
    private final DatabaseHelper mDatabaseHelper;
    private final SQLiteDatabase mSqliteDatabase;

    public MealRepository(Context context) {
        mDatabaseHelper = DatabaseHelper.getInstance(context);
        mDatabaseHelper.acquire();
        mSqliteDatabase = mDatabaseHelper.getWritableDatabase();
    }

    public long insertMeal(String meal, String status, String observation, long timestamp) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(Constants.COLUMN_NAME_MEAL, meal);
        contentValues.put(Constants.COLUMN_NAME_MEAL_STATUS, status);
        contentValues.put(Constants.COLUMN_NAME_MEAL_OBS, observation);
        contentValues.put(Constants.COLUMN_NAME_TIMESTAMP, timestamp);

        long newRowId = mSqliteDatabase.insert(Constants.TABLE_NAME_MEAL, null, contentValues);
        Log.d(TAG, "Inserted row ID: " + newRowId);

        if (newRowId == -1) {
            Log.e(TAG, "Failed to save meal " + status);
        }
        return newRowId;
    }

    public int[] getMealCounts(long start, long end) {
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

    /**
     * Returns all meal records within the given time range, ordered by timestamp ascending.
     */
    public List<MealRecord> getMealRecords(long start, long end) {
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

    /**
     * Returns the meal status per meal type per day within the given time range.
     * The map keys are the start-of-day timestamps (local timezone).
     * The inner map keys are meal types (Breakfast, Lunch, Tea, Dinner).
     */
    public Map<Long, Map<String, String>> getDailyMealStatusesByType(long start, long end) {
        Map<Long, Map<String, String>> dailyStatuses = new HashMap<>();

        String query = "SELECT " + Constants.COLUMN_NAME_TIMESTAMP + ", " +
                Constants.COLUMN_NAME_MEAL + ", " +
                Constants.COLUMN_NAME_MEAL_STATUS +
                " FROM " + Constants.TABLE_NAME_MEAL +
                " WHERE " + Constants.COLUMN_NAME_TIMESTAMP + " >= ? AND " +
                Constants.COLUMN_NAME_TIMESTAMP + " <= ?";

        try (Cursor cursor = mSqliteDatabase.rawQuery(query, new String[]{String.valueOf(start), String.valueOf(end)})) {
            while (cursor.moveToNext()) {
                long timestamp = cursor.getLong(0);
                String meal = cursor.getString(1);
                String status = cursor.getString(2);
                long dayStart = Common.getStartOfDayInMillis(timestamp);

                Map<String, String> byType = dailyStatuses.computeIfAbsent(dayStart, k -> new HashMap<>());
                byType.put(meal, status);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting daily meal statuses by type: " + e.getMessage());
        }

        return dailyStatuses;
    }

    /**
     * Returns the set of day-start timestamps that have any meal records
     * within the given time range.
     */
    public Set<Long> getDaysWithMealData(long start, long end) {
        Set<Long> daysWithData = new HashSet<>();

        String query = "SELECT " + Constants.COLUMN_NAME_TIMESTAMP +
                " FROM " + Constants.TABLE_NAME_MEAL +
                " WHERE " + Constants.COLUMN_NAME_TIMESTAMP + " >= ? AND " +
                Constants.COLUMN_NAME_TIMESTAMP + " <= ?";

        try (Cursor cursor = mSqliteDatabase.rawQuery(query, new String[]{String.valueOf(start), String.valueOf(end)})) {
            while (cursor.moveToNext()) {
                long timestamp = cursor.getLong(0);
                long dayStart = Common.getStartOfDayInMillis(timestamp);
                daysWithData.add(dayStart);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting days with meal data: " + e.getMessage());
        }

        return daysWithData;
    }

    public void closeDb() {
        mDatabaseHelper.release();
    }

    /**
     * Checks if a meal of the given type already exists for today (excluding OTHER_MEAL).
     * Used for regular meals (Breakfast, Lunch, Tea, Dinner).
     * @param mealType The meal type to check (e.g., Constants.BREAKFAST, Constants.LUNCH, etc.)
     * @return true if a meal of this type exists for today (excluding OTHER_MEAL), false otherwise
     */
    public boolean hasMealForToday(String mealType) {
        return getMealIdForTodayExcludingOtherMeal(mealType) != -1;
    }

    public long getMealIdForToday(String mealType) {
        return getMealIdForTodayExcludingOtherMeal(mealType);
    }

    /**
     * Gets the ID of the existing meal for a given meal type today (excluding OTHER_MEAL).
     * Used for regular meals (Breakfast, Lunch, Tea, Dinner).
     * @param mealType The meal type to look for (e.g., Constants.BREAKFAST, Constants.LUNCH, etc.)
     * @return The row ID of the existing meal, or -1 if no meal exists
     */
    public long getMealIdForTodayExcludingOtherMeal(String mealType) {
        long startOfDay = Common.getStartOfDayInMillis();
        long endOfDay = Common.getEndOfDayInMillis();
        
        String query = "SELECT " + Constants._ID + " FROM " + Constants.TABLE_NAME_MEAL +
                " WHERE " + Constants.COLUMN_NAME_MEAL + " = ? AND " +
                Constants.COLUMN_NAME_MEAL_STATUS + " != ? AND " +
                Constants.COLUMN_NAME_TIMESTAMP + " >= ? AND " +
                Constants.COLUMN_NAME_TIMESTAMP + " <= ?";
        
        try (Cursor cursor = mSqliteDatabase.rawQuery(query, 
                new String[]{mealType, Constants.OTHER_MEAL, String.valueOf(startOfDay), String.valueOf(endOfDay)})) {
            if (cursor.moveToFirst()) {
                return cursor.getLong(0);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting meal ID: " + e.getMessage());
        }
        return -1;
    }

    /**
     * Gets the ID of the existing OTHER_MEAL for a given meal type today.
     * Used when saving irregular meals (when no radio button is selected).
     * @param mealType The meal type to look for (e.g., Constants.BREAKFAST, Constants.LUNCH, etc.)
     * @return The row ID of the existing OTHER_MEAL, or -1 if no meal exists
     */
    public long getOtherMealIdForToday(String mealType) {
        long startOfDay = Common.getStartOfDayInMillis();
        long endOfDay = Common.getEndOfDayInMillis();
        
        String query = "SELECT " + Constants._ID + " FROM " + Constants.TABLE_NAME_MEAL +
                " WHERE " + Constants.COLUMN_NAME_MEAL + " = ? AND " +
                Constants.COLUMN_NAME_MEAL_STATUS + " = ? AND " +
                Constants.COLUMN_NAME_TIMESTAMP + " >= ? AND " +
                Constants.COLUMN_NAME_TIMESTAMP + " <= ?";
        
        try (Cursor cursor = mSqliteDatabase.rawQuery(query, 
                new String[]{mealType, Constants.OTHER_MEAL, String.valueOf(startOfDay), String.valueOf(endOfDay)})) {
            if (cursor.moveToFirst()) {
                return cursor.getLong(0);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting OTHER_MEAL ID: " + e.getMessage());
        }
        return -1;
    }

    /**
     * Updates an existing meal record.
     * @param rowId The ID of the meal record to update
     * @param status The new meal status
     * @param observation The new observation
     * @param timestamp The new timestamp
     * @return The number of rows updated (should be 1 on success), or -1 on failure
     */
    public int updateMeal(long rowId, String status, String observation, long timestamp) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(Constants.COLUMN_NAME_MEAL_STATUS, status);
        contentValues.put(Constants.COLUMN_NAME_MEAL_OBS, observation);
        contentValues.put(Constants.COLUMN_NAME_TIMESTAMP, timestamp);

        String whereClause = Constants._ID + " = ?";
        String[] whereArgs = new String[]{String.valueOf(rowId)};

        int rowsUpdated = mSqliteDatabase.update(Constants.TABLE_NAME_MEAL, contentValues, whereClause, whereArgs);
        Log.d(TAG, "Updated meal row ID: " + rowId + ", rows updated: " + rowsUpdated);

        if (rowsUpdated == 0) {
            Log.e(TAG, "Failed to update meal with ID: " + rowId);
        }
        return rowsUpdated;
    }
}