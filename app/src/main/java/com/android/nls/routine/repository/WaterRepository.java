package com.android.nls.routine.repository;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.provider.BaseColumns;
import android.util.Log;
import com.android.nls.routine.model.WaterRecord;
import com.android.nls.routine.service.database.DatabaseHelper;
import com.android.nls.routine.utils.Common;
import com.android.nls.routine.utils.Constants;
import java.util.ArrayList;
import java.util.List;

public class WaterRepository {
    private static final String TAG = Common.generateTag(WaterRepository.class);
    private final DatabaseHelper mDatabaseHelper;
    private final SQLiteDatabase mSqliteDatabase;

    public WaterRepository(Context context) {
        mDatabaseHelper = DatabaseHelper.getInstance(context);
        mDatabaseHelper.acquire();
        mSqliteDatabase = mDatabaseHelper.getWritableDatabase();
    }

    public long insertWater(int amount, long timestamp) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(Constants.COLUMN_NAME_WATER_DRANK, String.valueOf(amount));
        contentValues.put(Constants.COLUMN_NAME_TIMESTAMP, timestamp);

        long newRowId = mSqliteDatabase.insert(Constants.TABLE_NAME_WATER, null, contentValues);
        Log.d(TAG, "Inserted row ID: " + newRowId);

        if (newRowId == -1) {
            Log.e(TAG, "Failed to insert water record");
        }
        return newRowId;
    }

    public int getWaterSum(long start, long end) {
        int sum = 0;
        String query = "SELECT SUM(" + Constants.COLUMN_NAME_WATER_DRANK + ") FROM " + Constants.TABLE_NAME_WATER +
                " WHERE " + Constants.COLUMN_NAME_TIMESTAMP + " >= ? AND " +
                Constants.COLUMN_NAME_TIMESTAMP + " <= ?";

        try (Cursor cursor = mSqliteDatabase.rawQuery(query, new String[]{String.valueOf(start), String.valueOf(end)})) {
            if (cursor.moveToFirst() && cursor.getString(0) != null) {
                sum = cursor.getInt(0);
                Log.d(TAG, "Water sum loaded: " + sum + "ml");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error calculating water sum: " + e.getMessage());
        }

        return sum;
    }

    /**
     * Returns the last water record (amount and timestamp), or null if none exists.
     */
    public WaterRecord getLastWaterAddedRecord() {
        String query = "SELECT " + Constants.COLUMN_NAME_TIMESTAMP + ", " + Constants.COLUMN_NAME_WATER_DRANK +
                " FROM " + Constants.TABLE_NAME_WATER +
                " ORDER BY " + BaseColumns._ID + " DESC LIMIT 1";

        try (Cursor cursor = mSqliteDatabase.rawQuery(query, null)) {
            if (cursor.moveToFirst() && !cursor.isNull(0)) {
                long timestamp = cursor.getLong(0);
                int amount = cursor.getInt(1);
                return new WaterRecord(amount, timestamp);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting last drank record: " + e.getMessage());
        }

        return null;
    }

    /**
     * Returns all water records within the given time range, ordered by timestamp ascending.
     */
    public List<WaterRecord> getWaterRecords(long start, long end) {
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

    /**
     * Returns true if any water record exists within the given time range.
     */
    public boolean hasWaterData(long start, long end) {
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

    public void closeDb() {
        mDatabaseHelper.release();
    }
}