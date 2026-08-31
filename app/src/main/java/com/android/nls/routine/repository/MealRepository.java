package com.android.nls.routine.repository;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import com.android.nls.routine.model.MealRecord;
import com.android.nls.routine.service.database.DatabaseHelper;
import com.android.nls.routine.utils.Common;
import com.android.nls.routine.utils.Constants;
import java.util.ArrayList;
import java.util.List;

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
     * Returns true if any meal record exists within the given time range.
     */
    public boolean hasMealData(long start, long end) {
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

    public void closeDb() {
        mDatabaseHelper.release();
    }
}