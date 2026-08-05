package com.android.nls.routine.service.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;
import com.android.nls.routine.utils.Constants;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "Routine";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public void onCreate(SQLiteDatabase db) {
        db.execSQL(WaterFeedEntry.SQL_CREATE_ENTRIES_WATER);
        db.execSQL(WaterFeedEntry.SQL_CREATE_ENTRIES_USER_CONFIG);
    }

    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(WaterFeedEntry.SQL_DELETE_ENTRIES);
        onCreate(db);
    }

    public static class WaterFeedEntry implements BaseColumns {
        private static final String SQL_CREATE_ENTRIES_WATER =
                "CREATE TABLE " + Constants.TABLE_NAME_WATER + " (" +
                        _ID + " INTEGER PRIMARY KEY," +
                        Constants.COLUMN_NAME_WATER_DRANK + " TEXT," +
                        Constants.COLUMN_NAME_WATER_TIMESTAMP + " TEXT)";

        private static final String SQL_CREATE_ENTRIES_USER_CONFIG =
                "CREATE TABLE " + Constants.TABLE_NAME_USER_CONFIG + " (" +
                        _ID + " INTEGER PRIMARY KEY," +
                        Constants.COLUMN_NAME_DAILY_WATER + " TEXT," +
                        Constants.COLUMN_NAME_BTN_1_ADD_WATER + " TEXT," +
                        Constants.COLUMN_NAME_BTN_2_ADD_WATER + " TEXT,"+
                        Constants.COLUMN_NAME_BTN_3_ADD_WATER + " TEXT,"+
                        Constants.COLUMN_NAME_MONTHLY_LIMIT + " TEXT)";

        private static final String SQL_DELETE_ENTRIES =
                "DROP TABLE IF EXISTS " + Constants.TABLE_NAME_WATER;
    }
}
