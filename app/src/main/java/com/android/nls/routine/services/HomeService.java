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
import com.google.android.material.progressindicator.CircularProgressIndicator;

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

    public void addWater(TextView txtDailyWaterDrank, TextView txtLastWaterAdded, CircularProgressIndicator progressWater, TextView txtWaterPercentage, String amount) {
        int parsedAmount = Integer.parseInt(amount.replace("+", "").trim());
        long currentTimeMillis = System.currentTimeMillis();
        String currentTime = Common.getHourFromTimestamp(String.valueOf(currentTimeMillis));

        ContentValues contentValues = new ContentValues();
        contentValues.put(Constants.COLUMN_NAME_WATER_DRANK, String.valueOf(parsedAmount));
        contentValues.put(Constants.COLUMN_NAME_WATER_TIMESTAMP, currentTimeMillis);

        long newRowId = mSqliteDatabase.insert(Constants.TABLE_NAME_WATER, null, contentValues);
        Log.d(TAG, "Inserted row ID: " + newRowId);

        if (newRowId == -1) {
            Log.e(TAG, "Failed to insert water record");
            return;
        }

        int dailyWaterSum = getDailyWaterSum();
        String dailyWaterGoal = getDailyWaterGoal();

        setDailyWaterDrank(txtDailyWaterDrank, dailyWaterSum, dailyWaterGoal);
        updateWaterProgress(progressWater, txtWaterPercentage, dailyWaterSum, dailyWaterGoal);

        txtLastWaterAdded.setText(mContext.getString(R.string.last_added_at, currentTime));
        Log.d(TAG, "Added " + parsedAmount + "ml water. Total: " + dailyWaterSum + "ml");
    }

    public void setDailyWaterDrank(TextView txtDailyWaterDrank, int dailyWaterSum, String dailyWaterGoal) {
        if (Integer.parseInt(dailyWaterGoal) < dailyWaterSum) {
            txtDailyWaterDrank.setText(mContext.getString(R.string.water_default_value_init, String.valueOf(dailyWaterSum)));
            txtDailyWaterDrank.setTextColor(mContext.getColor(R.color.green));
        } else {
            txtDailyWaterDrank.setText(mContext.getString(R.string.water_default_value_init, String.valueOf(dailyWaterSum)));
        }
    }

    public void updateWaterProgress(CircularProgressIndicator progressWater, TextView txtWaterPercentage, int totalSum, String dailyWaterGoal) {
        long percentage = Math.min(100, Math.round((totalSum * 100.0) / Integer.parseInt(dailyWaterGoal)));
        progressWater.setProgress((int) percentage);
        txtWaterPercentage.setText(mContext.getString(R.string.circular_progress_init, percentage));
    }

    public int getDailyWaterSum() {
        int sum = 0;
        long startOfDay = Common.getStartOfDayInMillis();
        long endOfDay = Common.getEndOfDayInMillis();

        String query = "SELECT SUM(" + Constants.COLUMN_NAME_WATER_DRANK + ") FROM " + Constants.TABLE_NAME_WATER +
                " WHERE " + Constants.COLUMN_NAME_WATER_TIMESTAMP + " >= ? AND " +
                Constants.COLUMN_NAME_WATER_TIMESTAMP + " <= ?";

        try (Cursor cursor = mSqliteDatabase.rawQuery(query, new String[]{String.valueOf(startOfDay), String.valueOf(endOfDay)})) {
            if (cursor.moveToFirst() && cursor.getString(0) != null) {
                sum = cursor.getInt(0);
                Log.d(TAG, "Daily water sum loaded: " + sum + "ml");
                return sum;
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
            if (cursor.moveToFirst() && cursor.getString(0) != null) {
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

    public String getDefaultValueBtn1() {
        String query = "SELECT " + Constants.COLUMN_NAME_BTN_1_ADD_WATER +
                " FROM " + Constants.TABLE_NAME_USER_CONFIG +
                " ORDER BY " + DatabaseHelper.WaterFeedEntry._ID;

        try (Cursor cursor = mSqliteDatabase.rawQuery(query, null)) {
            if (cursor.moveToFirst() && cursor.getString(0) != null) {
                String btn1Water = cursor.getString(0);
                Log.d(TAG, "Getting default value for button 1: " + btn1Water);
                return btn1Water;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting default value for button 1: " + e.getMessage());
        }

        return Constants.DEFAULT_ADD_WATER_BUTTON_VALUES.get(0);
    }

    public String getDefaultValueBtn2() {
        String query = "SELECT " + Constants.COLUMN_NAME_BTN_2_ADD_WATER +
                " FROM " + Constants.TABLE_NAME_USER_CONFIG +
                " ORDER BY " + DatabaseHelper.WaterFeedEntry._ID;

        try (Cursor cursor = mSqliteDatabase.rawQuery(query, null)) {
            if (cursor.moveToFirst() && cursor.getString(0) != null) {
                String btn2Water = cursor.getString(0);
                Log.d(TAG, "Getting default value for button 2: " + btn2Water);
                return btn2Water;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting default value for button 2: " + e.getMessage());
        }

        return Constants.DEFAULT_ADD_WATER_BUTTON_VALUES.get(1);
    }

    public String getDefaultValueBtn3() {
        String query = "SELECT " + Constants.COLUMN_NAME_BTN_3_ADD_WATER +
                " FROM " + Constants.TABLE_NAME_USER_CONFIG +
                " ORDER BY " + DatabaseHelper.WaterFeedEntry._ID;

        try (Cursor cursor = mSqliteDatabase.rawQuery(query, null)) {
            if (cursor.moveToFirst() && cursor.getString(0) != null) {
                String btn3Water = cursor.getString(0);
                Log.d(TAG, "Getting default value for button 3: " + btn3Water);
                return btn3Water;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting default value for button 3: " + e.getMessage());
        }

        return Constants.DEFAULT_ADD_WATER_BUTTON_VALUES.get(2);
    }

    public String getDailyWaterGoal() {
        String query = "SELECT " + Constants.COLUMN_NAME_DAILY_WATER +
                " FROM " + Constants.TABLE_NAME_USER_CONFIG +
                " ORDER BY " + DatabaseHelper.WaterFeedEntry._ID;

        try (Cursor cursor = mSqliteDatabase.rawQuery(query, null)) {
            if (cursor.moveToFirst() && cursor.getString(0) != null) {
                String dailyWater = cursor.getString(0);
                Log.d(TAG, "Getting daily water: " + dailyWater);
                return dailyWater;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting daily water: " + e.getMessage());
        }

        return Constants.DEFAULT_DAILY_WATER;
    }
}