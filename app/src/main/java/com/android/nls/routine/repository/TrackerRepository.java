package com.android.nls.routine.repository;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.provider.BaseColumns;
import android.util.Log;
import com.android.nls.routine.model.Tracker;
import com.android.nls.routine.model.TrackerRecord;
import com.android.nls.routine.model.TrackerType;
import com.android.nls.routine.service.database.DatabaseHelper;
import com.android.nls.routine.utils.Common;
import com.android.nls.routine.utils.Constants;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TrackerRepository {
    private static final String TAG = Common.generateTag(TrackerRepository.class);
    private final DatabaseHelper mDatabaseHelper;
    private final SQLiteDatabase mSqliteDatabase;

    public TrackerRepository(Context context) {
        mDatabaseHelper = DatabaseHelper.getInstance(context);
        mDatabaseHelper.acquire();
        mSqliteDatabase = mDatabaseHelper.getWritableDatabase();
    }

    /**
     * Returns all trackers in the standard order:
     * Water, Meals, Expenses, Workout, Medication, Supplement
     */
    public List<Tracker> getAllTrackers() {
        List<Tracker> trackers = new ArrayList<>();
        String query = "SELECT * FROM " + Constants.TABLE_NAME_TRACKERS +
                " ORDER BY " + BaseColumns._ID + " ASC";

        try (Cursor cursor = mSqliteDatabase.rawQuery(query, null)) {
            while (cursor.moveToNext()) {
                trackers.add(cursorToTracker(cursor));
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting all trackers: " + e.getMessage());
        }

        return trackers;
    }

    /**
     * Returns only the enabled trackers, in the standard order.
     */
    public List<Tracker> getEnabledTrackers() {
        List<Tracker> trackers = new ArrayList<>();
        String query = "SELECT * FROM " + Constants.TABLE_NAME_TRACKERS +
                " WHERE " + Constants.COLUMN_NAME_TRACKER_ENABLED + " = 1" +
                " ORDER BY " + BaseColumns._ID + " ASC";

        try (Cursor cursor = mSqliteDatabase.rawQuery(query, null)) {
            while (cursor.moveToNext()) {
                trackers.add(cursorToTracker(cursor));
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting enabled trackers: " + e.getMessage());
        }

        return trackers;
    }

    public Tracker getTracker(TrackerType type) {
        String query = "SELECT * FROM " + Constants.TABLE_NAME_TRACKERS +
                " WHERE " + Constants.COLUMN_NAME_TRACKER_TYPE + " = ? LIMIT 1";

        try (Cursor cursor = mSqliteDatabase.rawQuery(query, new String[]{type.name()})) {
            if (cursor.moveToFirst()) {
                return cursorToTracker(cursor);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting tracker " + type + ": " + e.getMessage());
        }

        return null;
    }

    public void setTrackerEnabled(TrackerType type, boolean enabled) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(Constants.COLUMN_NAME_TRACKER_ENABLED, enabled ? 1 : 0);

        int rows = mSqliteDatabase.update(Constants.TABLE_NAME_TRACKERS,
                contentValues,
                Constants.COLUMN_NAME_TRACKER_TYPE + " = ?",
                new String[]{type.name()});
        Log.d(TAG, "Set tracker " + type + " enabled=" + enabled + " rows=" + rows);
    }

    public void updateTrackerConfig(TrackerType type, String name, String description) {
        ContentValues contentValues = new ContentValues();
        if (name != null) {
            contentValues.put(Constants.COLUMN_NAME_TRACKER_NAME, name);
        }
        if (description != null) {
            contentValues.put(Constants.COLUMN_NAME_TRACKER_DESCRIPTION, description);
        }

        int rows = mSqliteDatabase.update(Constants.TABLE_NAME_TRACKERS,
                contentValues,
                Constants.COLUMN_NAME_TRACKER_TYPE + " = ?",
                new String[]{type.name()});
        Log.d(TAG, "Updated tracker config " + type + " name=" + name + " description=" + description + " rows=" + rows);
    }

    /**
     * Saves a completion record for a given tracker. If a record already exists for
     * that tracker on the same day, it is updated; otherwise a new record is inserted.
     */
    public void saveTrackerRecord(TrackerType type, boolean completed, String note) {
        long startOfDay = Common.getStartOfDayInMillis();
        long endOfDay = Common.getEndOfDayInMillis();

        // Check if a record already exists for this tracker today
        String query = "SELECT " + BaseColumns._ID + " FROM " + Constants.TABLE_NAME_TRACKER_RECORDS +
                " WHERE " + Constants.COLUMN_NAME_TRACKER_RECORD_TYPE + " = ? AND " +
                Constants.COLUMN_NAME_TRACKER_RECORD_TIMESTAMP + " >= ? AND " +
                Constants.COLUMN_NAME_TRACKER_RECORD_TIMESTAMP + " <= ? LIMIT 1";

        try (Cursor cursor = mSqliteDatabase.rawQuery(query,
                new String[]{type.name(), String.valueOf(startOfDay), String.valueOf(endOfDay)})) {

            ContentValues contentValues = new ContentValues();
            contentValues.put(Constants.COLUMN_NAME_TRACKER_RECORD_TYPE, type.name());
            contentValues.put(Constants.COLUMN_NAME_TRACKER_RECORD_COMPLETED, completed ? 1 : 0);
            contentValues.put(Constants.COLUMN_NAME_TRACKER_RECORD_NOTE, note);
            contentValues.put(Constants.COLUMN_NAME_TRACKER_RECORD_TIMESTAMP, System.currentTimeMillis());

            if (cursor.moveToFirst()) {
                long id = cursor.getLong(0);
                int rows = mSqliteDatabase.update(Constants.TABLE_NAME_TRACKER_RECORDS,
                        contentValues, BaseColumns._ID + " = ?", new String[]{String.valueOf(id)});
                Log.d(TAG, "Updated tracker record " + type + " completed=" + completed + " rows=" + rows);
            } else {
                long newRowId = mSqliteDatabase.insert(Constants.TABLE_NAME_TRACKER_RECORDS, null, contentValues);
                Log.d(TAG, "Inserted tracker record " + type + " completed=" + completed + " id=" + newRowId);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error saving tracker record " + type + ": " + e.getMessage());
        }
    }

    /**
     * Returns all tracker records for a given type within the given time range,
     * ordered by timestamp ascending.
     */
    public List<TrackerRecord> getTrackerRecords(TrackerType type, long start, long end) {
        List<TrackerRecord> records = new ArrayList<>();

        String query = "SELECT " + Constants.COLUMN_NAME_TRACKER_RECORD_TYPE + ", " +
                Constants.COLUMN_NAME_TRACKER_RECORD_COMPLETED + ", " +
                Constants.COLUMN_NAME_TRACKER_RECORD_NOTE + ", " +
                Constants.COLUMN_NAME_TRACKER_RECORD_TIMESTAMP +
                " FROM " + Constants.TABLE_NAME_TRACKER_RECORDS +
                " WHERE " + Constants.COLUMN_NAME_TRACKER_RECORD_TYPE + " = ? AND " +
                Constants.COLUMN_NAME_TRACKER_RECORD_TIMESTAMP + " >= ? AND " +
                Constants.COLUMN_NAME_TRACKER_RECORD_TIMESTAMP + " <= ?" +
                " ORDER BY " + Constants.COLUMN_NAME_TRACKER_RECORD_TIMESTAMP + " ASC";

        try (Cursor cursor = mSqliteDatabase.rawQuery(query,
                new String[]{type.name(), String.valueOf(start), String.valueOf(end)})) {
            while (cursor.moveToNext()) {
                String typeStr = cursor.getString(0);
                boolean completed = cursor.getInt(1) == 1;
                String note = cursor.getString(2);
                long timestamp = cursor.getLong(3);
                records.add(new TrackerRecord(0, TrackerType.valueOf(typeStr), completed, note, timestamp));
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting tracker records " + type + ": " + e.getMessage());
        }

        return records;
    }

    /**
     * Returns the tracker completion status per day within the given time range.
     * The map keys are the start-of-day timestamps (local timezone).
     * The inner map keys are tracker types, values are whether they were completed.
     */
    public Map<Long, Map<TrackerType, Boolean>> getDailyTrackerCompletions(long start, long end) {
        Map<Long, Map<TrackerType, Boolean>> dailyCompletions = new HashMap<>();

        String query = "SELECT " + Constants.COLUMN_NAME_TRACKER_RECORD_TYPE + ", " +
                Constants.COLUMN_NAME_TRACKER_RECORD_COMPLETED + ", " +
                Constants.COLUMN_NAME_TRACKER_RECORD_TIMESTAMP +
                " FROM " + Constants.TABLE_NAME_TRACKER_RECORDS +
                " WHERE " + Constants.COLUMN_NAME_TRACKER_RECORD_TIMESTAMP + " >= ? AND " +
                Constants.COLUMN_NAME_TRACKER_RECORD_TIMESTAMP + " <= ?";

        try (Cursor cursor = mSqliteDatabase.rawQuery(query,
                new String[]{String.valueOf(start), String.valueOf(end)})) {
            while (cursor.moveToNext()) {
                String typeStr = cursor.getString(0);
                boolean completed = cursor.getInt(1) == 1;
                long timestamp = cursor.getLong(2);
                long dayStart = Common.getStartOfDayInMillis(timestamp);

                TrackerType trackerType;
                try {
                    trackerType = TrackerType.valueOf(typeStr);
                } catch (IllegalArgumentException e) {
                    Log.e(TAG, "Unknown tracker type: " + typeStr);
                    continue;
                }

                dailyCompletions.computeIfAbsent(dayStart, k -> new HashMap<>())
                        .put(trackerType, completed);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting daily tracker completions: " + e.getMessage());
        }

        return dailyCompletions;
    }

    /**
     * Returns the set of day-start timestamps that have any tracker records
     * within the given time range.
     */
    public Set<Long> getDaysWithTrackerData(long start, long end) {
        Set<Long> daysWithData = new HashSet<>();

        String query = "SELECT " + Constants.COLUMN_NAME_TRACKER_RECORD_TIMESTAMP +
                " FROM " + Constants.TABLE_NAME_TRACKER_RECORDS +
                " WHERE " + Constants.COLUMN_NAME_TRACKER_RECORD_TIMESTAMP + " >= ? AND " +
                Constants.COLUMN_NAME_TRACKER_RECORD_TIMESTAMP + " <= ?";

        try (Cursor cursor = mSqliteDatabase.rawQuery(query, new String[]{String.valueOf(start), String.valueOf(end)})) {
            while (cursor.moveToNext()) {
                long timestamp = cursor.getLong(0);
                long dayStart = Common.getStartOfDayInMillis(timestamp);
                daysWithData.add(dayStart);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting days with tracker data: " + e.getMessage());
        }

        return daysWithData;
    }

    /**
     * Returns the record for a tracker on a given day, or null if none exists.
     */
    public TrackerRecord getTrackerRecordForDay(TrackerType type, long dayTimestamp) {
        long startOfDay = Common.getStartOfDayInMillis(dayTimestamp);
        long endOfDay = Common.getEndOfDayInMillis(dayTimestamp);

        String query = "SELECT * FROM " + Constants.TABLE_NAME_TRACKER_RECORDS +
                " WHERE " + Constants.COLUMN_NAME_TRACKER_RECORD_TYPE + " = ? AND " +
                Constants.COLUMN_NAME_TRACKER_RECORD_TIMESTAMP + " >= ? AND " +
                Constants.COLUMN_NAME_TRACKER_RECORD_TIMESTAMP + " <= ? LIMIT 1";

        try (Cursor cursor = mSqliteDatabase.rawQuery(query,
                new String[]{type.name(), String.valueOf(startOfDay), String.valueOf(endOfDay)})) {
            if (cursor.moveToFirst()) {
                return cursorToTrackerRecord(cursor);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting tracker record " + type + ": " + e.getMessage());
        }

        return null;
    }

    private Tracker cursorToTracker(Cursor cursor) {
        long id = cursor.getLong(cursor.getColumnIndexOrThrow(BaseColumns._ID));
        String typeStr = cursor.getString(cursor.getColumnIndexOrThrow(Constants.COLUMN_NAME_TRACKER_TYPE));
        String name = cursor.getString(cursor.getColumnIndexOrThrow(Constants.COLUMN_NAME_TRACKER_NAME));
        String icon = cursor.getString(cursor.getColumnIndexOrThrow(Constants.COLUMN_NAME_TRACKER_ICON));
        boolean enabled = cursor.getInt(cursor.getColumnIndexOrThrow(Constants.COLUMN_NAME_TRACKER_ENABLED)) == 1;
        String description = cursor.getString(cursor.getColumnIndexOrThrow(Constants.COLUMN_NAME_TRACKER_DESCRIPTION));

        TrackerType trackerType;
        try {
            trackerType = TrackerType.valueOf(typeStr);
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Unknown tracker type: " + typeStr);
            trackerType = null;
        }

        return new Tracker(id, trackerType, name, icon, enabled, description);
    }

    private TrackerRecord cursorToTrackerRecord(Cursor cursor) {
        long id = cursor.getLong(cursor.getColumnIndexOrThrow(BaseColumns._ID));
        String typeStr = cursor.getString(cursor.getColumnIndexOrThrow(Constants.COLUMN_NAME_TRACKER_RECORD_TYPE));
        boolean completed = cursor.getInt(cursor.getColumnIndexOrThrow(Constants.COLUMN_NAME_TRACKER_RECORD_COMPLETED)) == 1;
        String note = cursor.getString(cursor.getColumnIndexOrThrow(Constants.COLUMN_NAME_TRACKER_RECORD_NOTE));
        long timestamp = cursor.getLong(cursor.getColumnIndexOrThrow(Constants.COLUMN_NAME_TRACKER_RECORD_TIMESTAMP));

        TrackerType trackerType;
        try {
            trackerType = TrackerType.valueOf(typeStr);
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Unknown tracker type: " + typeStr);
            trackerType = null;
        }

        return new TrackerRecord(id, trackerType, completed, note, timestamp);
    }

    public void closeDb() {
        mDatabaseHelper.release();
    }
}