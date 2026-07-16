package com.android.nls.routine.services;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import android.widget.TextView;
import com.android.nls.routine.services.database.DatabaseHelper;
import com.android.nls.routine.utils.Common;
import com.android.nls.routine.utils.Constants;

public class HomeService {
    private static final String TAG = Common.generateTag(HomeService.class);
    private final DatabaseHelper dbHelper;
    private final SQLiteDatabase db;
    private final Context mContext;

    public HomeService(Context context) {
        mContext = context;
        dbHelper = new DatabaseHelper(mContext);
        db = dbHelper.getWritableDatabase();
    }

    public void addWater(TextView textView, int amount) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(Constants.COLUMN_NAME_WATER_DRANK, String.valueOf(amount));
        contentValues.put(Constants.COLUMN_NAME_WATER_TIMESTAMP, String.valueOf(System.currentTimeMillis()));
        
        long newRowId = db.insert(Constants.TABLE_NAME_WATER, null, contentValues);
        Log.d(TAG, "Inserted row ID: " + newRowId);
        
        if (newRowId == -1) {
            Log.e(TAG, "Failed to insert water record");
            return;
        }
        
        int totalSum = getWaterSum();
        textView.setText(totalSum + "ml");
        Common.generateToastMessageShortWaterDrank(mContext, amount, Constants.WATER_ADDED);
        Log.d(TAG, "Added " + amount + "ml water. Total: " + totalSum + "ml");
    }

    public void saveCustomWaterValue(TextView textView, int amount) {
        try {
            if (amount > 0 && amount < 5000) {
                addWater(textView, amount);
                Common.generateToastMessageShortWaterDrank(mContext, amount, Constants.WATER_ADDED);
            } else {
                Common.generateToastMessageShortInvalidNumber(mContext, Constants.WATER_INVALID_NUMBER);
            }
        } catch (NumberFormatException e) {
            Common.generateToastMessageShortInvalidNumber(mContext, Constants.WATER_INVALID_NUMBER);
        }
    }

    public int getWaterSum() {
        int sum = 0;
        long startOfDay = Common.getStartOfDayInMillis();
        long endOfDay = Common.getEndOfDayInMillis();
        
        String query = "SELECT SUM(" + Constants.COLUMN_NAME_WATER_DRANK + ") FROM " + Constants.TABLE_NAME_WATER + 
                       " WHERE " + Constants.COLUMN_NAME_WATER_TIMESTAMP + " >= ? AND " + 
                       Constants.COLUMN_NAME_WATER_TIMESTAMP + " <= ?";
        
        try (Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(startOfDay), String.valueOf(endOfDay)})) {
            if (cursor.moveToFirst()) {
                sum = cursor.getInt(0);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error calculating water sum: " + e.getMessage());
        }
        
        Log.d(TAG, "Water sum for today: " + sum + "ml (from " + startOfDay + " to " + endOfDay + ")");
        return sum;
    }

    public void loadWaterSum(TextView textView) {
        int totalSum = getWaterSum();
        textView.setText(totalSum + "ml");
        Log.d(TAG, "Loaded water sum from database: " + totalSum + "ml");
    }

    public void closeDb() {
        dbHelper.close();
    }
}