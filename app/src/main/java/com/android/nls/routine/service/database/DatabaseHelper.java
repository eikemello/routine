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
    private static final int DATABASE_VERSION = 5;
    private static final String DATABASE_NAME = "Routine";
    private static DatabaseHelper sInstance;
    private static int sReferenceCount = 0;

    /**
     * Returns the shared DatabaseHelper instance, creating it on first use.
     * Callers must call {@link #acquire()} when they start using the instance
     * and {@link #release()} when they are done, so the underlying database
     * is only closed when the last holder releases it.
     */
    public static synchronized DatabaseHelper getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new DatabaseHelper(context.getApplicationContext());
        }
        return sInstance;
    }

    private DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public synchronized void acquire() {
        sReferenceCount++;
        Log.d(TAG, "DatabaseHelper acquired. References: " + sReferenceCount);
    }

    public synchronized void release() {
        sReferenceCount--;
        if (sReferenceCount <= 0) {
            sReferenceCount = 0;
            Log.d(TAG, "DatabaseHelper released. Closing database.");
            close();
            sInstance = null;
        } else {
            Log.d(TAG, "DatabaseHelper released. References: " + sReferenceCount);
        }
    }

    public void onCreate(SQLiteDatabase db) {
        db.execSQL(WaterFeedEntry.SQL_CREATE_ENTRIES_WATER);
        db.execSQL(WaterFeedEntry.SQL_CREATE_ENTRIES_USER_CONFIG);
        db.execSQL(WaterFeedEntry.SQL_CREATE_ENTRIES_EXPENSE_TEST);
        db.execSQL(WaterFeedEntry.SQL_CREATE_ENTRIES_MEAL);
        db.execSQL(WaterFeedEntry.SQL_CREATE_ENTRIES_TRACKERS);
        db.execSQL(WaterFeedEntry.SQL_CREATE_ENTRIES_TRACKER_RECORDS);
        createIndexes(db);
        insertDefaultTrackers(db);
    }

    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // No migrations needed yet — the app has not been released.
        // Future schema changes should be added here, guarded by version checks.
    }

    private void createIndexes(SQLiteDatabase db) {
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_water_timestamp ON " +
                Constants.TABLE_NAME_WATER + "(" + Constants.COLUMN_NAME_TIMESTAMP + ")");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_expense_timestamp ON " +
                Constants.TABLE_NAME_EXPENSE_TEST + "(" + Constants.COLUMN_NAME_TIMESTAMP + ")");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_meal_timestamp ON " +
                Constants.TABLE_NAME_MEAL + "(" + Constants.COLUMN_NAME_TIMESTAMP + ")");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_tracker_records_type_timestamp ON " +
                Constants.TABLE_NAME_TRACKER_RECORDS + "(" +
                Constants.COLUMN_NAME_TRACKER_RECORD_TYPE + ", " +
                Constants.COLUMN_NAME_TRACKER_RECORD_TIMESTAMP + ")");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_tracker_records_timestamp ON " +
                Constants.TABLE_NAME_TRACKER_RECORDS + "(" + Constants.COLUMN_NAME_TRACKER_RECORD_TIMESTAMP + ")");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_trackers_type ON " +
                Constants.TABLE_NAME_TRACKERS + "(" + Constants.COLUMN_NAME_TRACKER_TYPE + ")");
        Log.d(TAG, "Created database indexes");
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
                        Constants.COLUMN_NAME_WATER_DRANK + " INTEGER," +
                        Constants.COLUMN_NAME_TIMESTAMP + " INTEGER)";

        private static final String SQL_CREATE_ENTRIES_USER_CONFIG =
                "CREATE TABLE " + Constants.TABLE_NAME_USER_CONFIG + " (" +
                        _ID + " INTEGER PRIMARY KEY," +
                        Constants.COLUMN_NAME_DAILY_WATER + " TEXT," +
                        Constants.COLUMN_NAME_BTN_1_ADD_WATER + " TEXT," +
                        Constants.COLUMN_NAME_BTN_2_ADD_WATER + " TEXT," +
                        Constants.COLUMN_NAME_BTN_3_ADD_WATER + " TEXT," +
                        Constants.COLUMN_NAME_MONTHLY_LIMIT + " TEXT," +
                        Constants.COLUMN_NAME_CARD_STATEMENT_CLOSING + " TEXT)";

        private static final String SQL_CREATE_ENTRIES_EXPENSE_TEST =
                "CREATE TABLE " + Constants.TABLE_NAME_EXPENSE_TEST + " (" +
                        _ID + " INTEGER PRIMARY KEY," +
                        Constants.COLUMN_NAME_EXPENSE_VALUE + " REAL," +
                        Constants.COLUMN_NAME_EXPENSE_TEXT + " TEXT," +
                        Constants.COLUMN_NAME_BANK_NAME + " TEXT," +
                        Constants.COLUMN_NAME_TIMESTAMP + " INTEGER)";

        private static final String SQL_CREATE_ENTRIES_MEAL =
                "CREATE TABLE " + Constants.TABLE_NAME_MEAL + " (" +
                        _ID + " INTEGER PRIMARY KEY," +
                        Constants.COLUMN_NAME_MEAL + " TEXT," +
                        Constants.COLUMN_NAME_MEAL_STATUS + " TEXT," +
                        Constants.COLUMN_NAME_MEAL_OBS + " TEXT," +
                        Constants.COLUMN_NAME_TIMESTAMP + " INTEGER)";

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
                        Constants.COLUMN_NAME_TRACKER_RECORD_TIMESTAMP + " INTEGER)";
    }
}