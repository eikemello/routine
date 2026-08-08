package com.android.nls.routine.service;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.provider.Settings;
import android.util.Log;
import com.android.nls.routine.model.Expense;
import com.android.nls.routine.service.database.DatabaseHelper;
import com.android.nls.routine.utils.Common;
import com.android.nls.routine.utils.Constants;

public class HomeCardExpenseService {
    private static final String TAG = Common.generateTag(HomeCardExpenseService.class);
    private final DatabaseHelper mDatabaseHelper;
    private final SQLiteDatabase mSqliteDatabase;
    private final ConfigService mConfigService;
    private final Context mContext;

    public HomeCardExpenseService(Context context) {
        mContext = context;
        mDatabaseHelper = new DatabaseHelper(mContext);
        mSqliteDatabase = mDatabaseHelper.getWritableDatabase();
        mConfigService = new ConfigService(mContext);
    }

    public void saveExpenseTest(Expense expense) {
        long currentTimeMillis = System.currentTimeMillis();

        ContentValues contentValues = new ContentValues();
        contentValues.put(Constants.COLUMN_NAME_EXPENSE_TEXT, expense.toString());
        contentValues.put(Constants.COLUMN_NAME_EXPENSE_VALUE, expense.amount());
        contentValues.put(Constants.COLUMN_NAME_BANK_NAME, expense.bank());
        contentValues.put(Constants.COLUMN_NAME_TIMESTAMP, currentTimeMillis);

        long newRowId = mSqliteDatabase.insert(Constants.TABLE_NAME_EXPENSE_TEST, null, contentValues);
        Log.d(TAG, "Inserted row ID: " + newRowId);

        if (newRowId == -1) {
            Log.e(TAG, "Failed to insert expense record");
        }
    }

    public void setNotifyAccess() {
        Intent intent = new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS");
        mContext.startActivity(intent);
    }

    public boolean isNotifyAccessEnabled() {
        String enabledListeners = Settings.Secure.getString(
                mContext.getContentResolver(),
                "enabled_notification_listeners"
        );
        return enabledListeners != null && enabledListeners.contains(mContext.getPackageName());
    }

    public String getTotalSpent() {
        int sum = 0;
        long startOfMonth = Common.getStartOfMonthInMillis();
        long endOfMonth = Common.getEndOfMonthInMillis();

        String query = "SELECT SUM(" + Constants.COLUMN_NAME_EXPENSE_VALUE + ") FROM " + Constants.TABLE_NAME_EXPENSE_TEST +
                " WHERE " + Constants.COLUMN_NAME_TIMESTAMP + " >= ? AND " +
                Constants.COLUMN_NAME_TIMESTAMP + " <= ?";

        try (Cursor cursor = mSqliteDatabase.rawQuery(query, new String[]{String.valueOf(startOfMonth), String.valueOf(endOfMonth)})) {
            if (cursor.moveToFirst() && cursor.getString(0) != null) {
                sum = cursor.getInt(0);
                Log.d(TAG, "Monthly expense sum loaded: " + sum);
                return String.valueOf(sum);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error calculating expense sum: " + e.getMessage());
        }

        return String.valueOf(sum);
    }

    public String getMonthlyLimitValue() {
        return mConfigService.getMonthlyLimitValue();
    }

    public void closeDb() {
        mDatabaseHelper.close();
    }
}
