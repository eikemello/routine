package com.android.nls.routine.services;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;
import com.android.nls.routine.services.database.DatabaseHelper;
import com.android.nls.routine.utils.Common;
import com.android.nls.routine.utils.Constants;

public class HomeService {
    private static final String TAG = Common.generateTag(HomeService.class);

    private final DatabaseHelper dbHelper;
    private final SQLiteDatabase db;
    private final Context mContext;
    private int currentWaterIntake = 0;

    public HomeService(Context context) {
        mContext = context;
        dbHelper = new DatabaseHelper(mContext);
        db = dbHelper.getWritableDatabase();
    }

    public void addWater(TextView textView, int amount) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(Constants.COLUMN_NAME_WATER_SUM, String.valueOf(amount));
        contentValues.put(Constants.COLUMN_NAME_WATER_TIMESTAMP, String.valueOf(System.currentTimeMillis()));
        
        long newRowId = db.insert(Constants.TABLE_NAME_WATER, null, contentValues);
        Log.d(TAG, "Inserted row ID: " + newRowId);
        
        if (newRowId == -1) {
            Log.e(TAG, "Failed to insert water record");
            return;
        }
        
        currentWaterIntake += amount;
        textView.setText(currentWaterIntake + "ml");
        Log.d(TAG, "Added " + amount + "ml water. Total: " + currentWaterIntake + "ml");
    }

    public void saveCustomWaterValue(TextView textView, int amount) {
        try {
            if (amount > 0 && amount < 5000) {
                addWater(textView, amount);
            } else {
                Toast.makeText(mContext, "Please enter a positive value", Toast.LENGTH_SHORT).show();
            }
        } catch (NumberFormatException e) {
            Toast.makeText(mContext, "Please enter a valid number", Toast.LENGTH_SHORT).show();
        }
    }

    public void closeDb() {
        dbHelper.close();
    }
}