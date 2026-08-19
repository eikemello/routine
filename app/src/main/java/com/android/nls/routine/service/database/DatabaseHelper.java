package com.android.nls.routine.service.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;
import android.util.Log;
import com.android.nls.routine.utils.Common;
import com.android.nls.routine.utils.Constants;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String TAG = Common.generateTag(DatabaseHelper.class);
    private static final int DATABASE_VERSION = 2;
    private static final String DATABASE_NAME = "Routine";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public void onCreate(SQLiteDatabase db) {
        db.execSQL(WaterFeedEntry.SQL_CREATE_ENTRIES_WATER);
        db.execSQL(WaterFeedEntry.SQL_CREATE_ENTRIES_USER_CONFIG);
        db.execSQL(WaterFeedEntry.SQL_CREATE_ENTRIES_EXPENSE_TEST);
        db.execSQL(WaterFeedEntry.SQL_CREATE_ENTRIES_MEAL);
        db.execSQL(WaterFeedEntry.SQL_CREATE_ENTRIES_TRACKERS);
        db.execSQL(WaterFeedEntry.SQL_CREATE_ENTRIES_TRACKER_RECORDS);
        insertDefaultTrackers(db);
    }

    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            db.execSQL(WaterFeedEntry.SQL_CREATE_ENTRIES_TRACKERS);
            db.execSQL(WaterFeedEntry.SQL_CREATE_ENTRIES_TRACKER_RECORDS);
            insertDefaultTrackers(db);
        }
    }

    private void insertDefaultTrackers(SQLiteDatabase db) {
        // Default enabled trackers: Water, Meals, Expenses
        // Default disabled trackers: Workout, Medication, Supplement
        insertTracker(db, Constants.TRACKER_NAME_WATER, Constants.TRACKER_ICON_WATER, 1, null);
        insertTracker(db, Constants.TRACKER_NAME_EXPENSES, Constants.TRACKER_ICON_EXPENSES, 1, null);
        insertTracker(db, Constants.TRACKER_NAME_MEALS, Constants.TRACKER_ICON_MEALS, 1, null);
        insertTracker(db, Constants.TRACKER_NAME_WORKOUT, Constants.TRACKER_ICON_WORKOUT, 0, null);
        insertTracker(db, Constants.TRACKER_NAME_MEDICATION, Constants.TRACKER_ICON_MEDICATION, 0, null);
        insertTracker(db, Constants.TRACKER_NAME_SUPPLEMENT, Constants.TRACKER_ICON_SUPPLEMENT, 0, null);
    }

    private void insertTracker(SQLiteDatabase db, String name, String icon, int enabled, String description) {
        db.execSQL("INSERT INTO " + Constants.TABLE_NAME_TRACKERS + " (" +
                Constants.COLUMN_NAME_TRACKER_TYPE + ", " +
                Constants.COLUMN_NAME_TRACKER_NAME + ", " +
                Constants.COLUMN_NAME_TRACKER_ICON + ", " +
                Constants.COLUMN_NAME_TRACKER_ENABLED + ", " +
                Constants.COLUMN_NAME_TRACKER_DESCRIPTION + ") VALUES ('" +
                name.toUpperCase() + "', '" + name + "', '" + icon + "', " + enabled + ", " +
                (description == null ? "NULL" : "'" + description + "'") + ")");
        Log.d(TAG, "Inserted default tracker: " + name + " enabled=" + enabled);
    }

    public static class WaterFeedEntry implements BaseColumns {
        private static final String SQL_CREATE_ENTRIES_WATER =
                "CREATE TABLE " + Constants.TABLE_NAME_WATER + " (" +
                        _ID + " INTEGER PRIMARY KEY," +
                        Constants.COLUMN_NAME_WATER_DRANK + " TEXT," +
                        Constants.COLUMN_NAME_TIMESTAMP + " TEXT)";

        private static final String SQL_CREATE_ENTRIES_USER_CONFIG =
                "CREATE TABLE " + Constants.TABLE_NAME_USER_CONFIG + " (" +
                        _ID + " INTEGER PRIMARY KEY," +
                        Constants.COLUMN_NAME_DAILY_WATER + " TEXT," +
                        Constants.COLUMN_NAME_BTN_1_ADD_WATER + " TEXT," +
                        Constants.COLUMN_NAME_BTN_2_ADD_WATER + " TEXT," +
                        Constants.COLUMN_NAME_BTN_3_ADD_WATER + " TEXT," +
                        Constants.COLUMN_NAME_MONTHLY_LIMIT + " TEXT)";

        private static final String SQL_CREATE_ENTRIES_EXPENSE_TEST =
                "CREATE TABLE " + Constants.TABLE_NAME_EXPENSE_TEST + " (" +
                        _ID + " INTEGER PRIMARY KEY," +
                        Constants.COLUMN_NAME_EXPENSE_VALUE + " TEXT," +
                        Constants.COLUMN_NAME_EXPENSE_TEXT + " TEXT," +
                        Constants.COLUMN_NAME_BANK_NAME + " TEXT," +
                        Constants.COLUMN_NAME_TIMESTAMP + " TEXT)";

        private static final String SQL_CREATE_ENTRIES_MEAL =
                "CREATE TABLE " + Constants.TABLE_NAME_MEAL + " (" +
                        _ID + " INTEGER PRIMARY KEY," +
                        Constants.COLUMN_NAME_MEAL + " TEXT," +
                        Constants.COLUMN_NAME_MEAL_STATUS + " TEXT," +
                        Constants.COLUMN_NAME_MEAL_OBS + " TEXT," +
                        Constants.COLUMN_NAME_TIMESTAMP + " TEXT)";

        private static final String SQL_CREATE_ENTRIES_TRACKERS =
                "CREATE TABLE " + Constants.TABLE_NAME_TRACKERS + " (" +
                        _ID + " INTEGER PRIMARY KEY," +
                        Constants.COLUMN_NAME_TRACKER_TYPE + " TEXT," +
                        Constants.COLUMN_NAME_TRACKER_NAME + " TEXT," +
                        Constants.COLUMN_NAME_TRACKER_ICON + " TEXT," +
                        Constants.COLUMN_NAME_TRACKER_ENABLED + " INTEGER," +
                        Constants.COLUMN_NAME_TRACKER_DESCRIPTION + " TEXT)";

        private static final String SQL_CREATE_ENTRIES_TRACKER_RECORDS =
                "CREATE TABLE " + Constants.TABLE_NAME_TRACKER_RECORDS + " (" +
                        _ID + " INTEGER PRIMARY KEY," +
                        Constants.COLUMN_NAME_TRACKER_RECORD_TYPE + " TEXT," +
                        Constants.COLUMN_NAME_TRACKER_RECORD_COMPLETED + " INTEGER," +
                        Constants.COLUMN_NAME_TRACKER_RECORD_NOTE + " TEXT," +
                        Constants.COLUMN_NAME_TRACKER_RECORD_TIMESTAMP + " TEXT)";
    }
}