package com.android.nls.routine.repository;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.provider.BaseColumns;
import android.util.Log;
import com.android.nls.routine.service.database.DatabaseHelper;
import com.android.nls.routine.utils.Common;
import com.android.nls.routine.utils.Constants;

public class ConfigRepository {
    private static final String TAG = Common.generateTag(ConfigRepository.class);
    private final DatabaseHelper mDatabaseHelper;
    private final SQLiteDatabase mSqliteDatabase;

    public ConfigRepository(Context context) {
        mDatabaseHelper = new DatabaseHelper(context);
        mSqliteDatabase = mDatabaseHelper.getWritableDatabase();
    }

    public void saveConfigValue(String columnName, String value) {
        long result;
        ContentValues contentValues = new ContentValues();
        contentValues.put(columnName, value);

        // Check if a row already exists in the table
        Cursor cursor = mSqliteDatabase.rawQuery(
                "SELECT " + BaseColumns._ID + " FROM " + Constants.TABLE_NAME_USER_CONFIG + " LIMIT 1", null);

        if (cursor.moveToFirst() && cursor.getString(0) != null) {
            // Row exists: UPDATE it
            long id = cursor.getLong(0);
            result = mSqliteDatabase.update(Constants.TABLE_NAME_USER_CONFIG,
                    contentValues, BaseColumns._ID + " = ?", new String[]{String.valueOf(id)});
            Log.d(TAG, "Updated row ID: " + id);
        } else {
            // No row exists: INSERT a new one
            result = mSqliteDatabase.insert(Constants.TABLE_NAME_USER_CONFIG, null, contentValues);
            Log.d(TAG, "Inserted row ID: " + result);
        }
        cursor.close();

        if (result == -1) {
            Log.e(TAG, "Failed to configure " + columnName);
        }
    }

    public double getDailyWaterGoal() {
        return getConfigValue(Constants.COLUMN_NAME_DAILY_WATER, Constants.DEFAULT_DAILY_WATER);
    }

    public double getDefaultBtn1Value() {
        return getConfigValue(Constants.COLUMN_NAME_BTN_1_ADD_WATER, Constants.DEFAULT_BTN_1_VALUE);
    }

    public double getDefaultBtn2Value() {
        return getConfigValue(Constants.COLUMN_NAME_BTN_2_ADD_WATER, Constants.DEFAULT_BTN_2_VALUE);
    }

    public double getDefaultBtn3Value() {
        return getConfigValue(Constants.COLUMN_NAME_BTN_3_ADD_WATER, Constants.DEFAULT_BTN_3_VALUE);
    }

    public double getMonthlyLimitValue() {
        return getConfigValue(Constants.COLUMN_NAME_MONTHLY_LIMIT, Constants.DEFAULT_MONTHLY_LIMIT_VALUE);
    }

    public double getCardStatementClosingDate() {
        return getConfigValue(Constants.COLUMN_NAME_CARD_STATEMENT_CLOSING, Constants.DEFAULT_CARD_STATEMENT_CLOSING);
    }

    public double getConfigValue(String columnName, double defaultValue) {
        String query = "SELECT " + columnName +
                " FROM " + Constants.TABLE_NAME_USER_CONFIG;

        try (Cursor cursor = mSqliteDatabase.rawQuery(query, null)) {
            if (cursor.moveToFirst() && cursor.getString(0) != null) {
                double value = cursor.getDouble(0);
                Log.d(TAG, "Config " + columnName + " loaded: " + value);
                return value;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting config " + columnName + ": " + e.getMessage());
        }

        return defaultValue;
    }

    public void closeDb() {
        mDatabaseHelper.close();
    }
}