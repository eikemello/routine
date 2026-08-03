package com.android.nls.routine.services;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import android.widget.TextView;
import com.android.nls.routine.R;
import com.android.nls.routine.services.database.DatabaseHelper;
import com.android.nls.routine.utils.Common;
import com.android.nls.routine.utils.Constants;

public class HomeService {
    private static final String TAG = Common.generateTag(HomeService.class);
    private final DatabaseHelper mDatabaseHelper;
    private final SQLiteDatabase mSqliteDatabase;
    private final Context mContext;

    public HomeService(Context context) {
        mContext = context;
        mDatabaseHelper = new DatabaseHelper(mContext);
        mSqliteDatabase = mDatabaseHelper.getWritableDatabase();
    }

    public void addWater(TextView txtDailyWaterDrank, TextView txtLastWaterAdded, int amount) {
        long currentTimeMillis = System.currentTimeMillis();
        String currentTime = Common.getHourFromTimestamp(String.valueOf(currentTimeMillis));

        ContentValues contentValues = new ContentValues();
        contentValues.put(Constants.COLUMN_NAME_WATER_DRANK, String.valueOf(amount));
        contentValues.put(Constants.COLUMN_NAME_WATER_TIMESTAMP, currentTimeMillis);

        long newRowId = mSqliteDatabase.insert(Constants.TABLE_NAME_WATER, null, contentValues);
        Log.d(TAG, "Inserted row ID: " + newRowId);

        if (newRowId == -1) {
            Log.e(TAG, "Failed to insert water record");
            return;
        }

        int totalSum = getDailyWaterSum();
        txtDailyWaterDrank.setText(mContext.getString(R.string.water_default_value_init, String.valueOf(3000 - totalSum)));
        txtLastWaterAdded.setText(mContext.getString(R.string.last_added_at, currentTime));
        Log.d(TAG, "Added " + amount + "ml water. Total: " + totalSum + "ml");
    }

    public void saveCustomWaterValue(TextView txtDailyWaterDrank, TextView txtLastWaterAdded, int amount) {
        try {
            if (amount > 0 && amount < 5000) {
                addWater(txtDailyWaterDrank, txtLastWaterAdded, amount);
            } else {
                Common.generateToastMessageShortInvalidNumber(mContext, Constants.WATER_INVALID_NUMBER);
            }
        } catch (NumberFormatException e) {
            Common.generateToastMessageShortInvalidNumber(mContext, Constants.WATER_INVALID_NUMBER);
        }
    }

    public int getDailyWaterSum() {
        int sum = 0;
        long startOfDay = Common.getStartOfDayInMillis();
        long endOfDay = Common.getEndOfDayInMillis();

        String query = "SELECT SUM(" + Constants.COLUMN_NAME_WATER_DRANK + ") FROM " + Constants.TABLE_NAME_WATER +
                " WHERE " + Constants.COLUMN_NAME_WATER_TIMESTAMP + " >= ? AND " +
                Constants.COLUMN_NAME_WATER_TIMESTAMP + " <= ?";

        try (Cursor cursor = mSqliteDatabase.rawQuery(query, new String[]{String.valueOf(startOfDay), String.valueOf(endOfDay)})) {
            if (cursor.moveToFirst()) {
                sum = cursor.getInt(0);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error calculating water sum: " + e.getMessage());
        }

        return sum;
    }

    public String getLastWaterAddedTime() {
        String query = "SELECT " + Constants.COLUMN_NAME_WATER_TIMESTAMP +
                " FROM " + Constants.TABLE_NAME_WATER +
                " ORDER BY " + DatabaseHelper.WaterFeedEntry._ID + " DESC LIMIT 1";

        try (Cursor cursor = mSqliteDatabase.rawQuery(query, null)) {
            if (cursor.moveToFirst()) {
                String lastTime = Common.getHourFromTimestamp(cursor.getString(0));
                Log.d(TAG, "Last water drank time: " + lastTime);
                return lastTime;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting last drank time: " + e.getMessage());
        }

        return "";
    }

    public void closeDb() {
        mDatabaseHelper.close();
    }
}