package com.android.nls.routine.repository;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.provider.BaseColumns;
import android.util.Log;
import com.android.nls.routine.model.Expense;
import com.android.nls.routine.model.ExpenseRecord;
import com.android.nls.routine.service.database.DatabaseHelper;
import com.android.nls.routine.utils.Common;
import com.android.nls.routine.utils.Constants;
import java.util.ArrayList;
import java.util.List;

public class ExpenseRepository {
    private static final String TAG = Common.generateTag(ExpenseRepository.class);
    private final DatabaseHelper mDatabaseHelper;
    private final SQLiteDatabase mSqliteDatabase;

    public ExpenseRepository(Context context) {
        mDatabaseHelper = new DatabaseHelper(context);
        mSqliteDatabase = mDatabaseHelper.getWritableDatabase();
    }

    public void insertExpense(Expense expense) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(Constants.COLUMN_NAME_EXPENSE_TEXT, expense.toString());
        contentValues.put(Constants.COLUMN_NAME_EXPENSE_VALUE, expense.amount());
        contentValues.put(Constants.COLUMN_NAME_BANK_NAME, expense.bank());
        contentValues.put(Constants.COLUMN_NAME_TIMESTAMP, expense.timestamp());

        long newRowId = mSqliteDatabase.insert(Constants.TABLE_NAME_EXPENSE_TEST, null, contentValues);
        Log.d(TAG, "Inserted row ID: " + newRowId);

        if (newRowId == -1) {
            Log.e(TAG, "Failed to insert expense record");
        }
    }

    /**
     * Returns the sum of expense values within the given time range.
     */
    public double getTotalSpent(long start, long end) {
        double sum = 0.0;
        String query = "SELECT SUM(" + Constants.COLUMN_NAME_EXPENSE_VALUE + ") FROM " + Constants.TABLE_NAME_EXPENSE_TEST +
                " WHERE " + Constants.COLUMN_NAME_TIMESTAMP + " >= ? AND " +
                Constants.COLUMN_NAME_TIMESTAMP + " <= ?";

        try (Cursor cursor = mSqliteDatabase.rawQuery(query, new String[]{String.valueOf(start), String.valueOf(end)})) {
            if (cursor.moveToFirst() && cursor.getString(0) != null) {
                sum = cursor.getDouble(0);
                Log.d(TAG, "Expense sum loaded: " + sum);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error calculating expense sum: " + e.getMessage());
        }

        return sum;
    }

    /**
     * Returns all expense records within the given time range, ordered by timestamp ascending.
     */
    public List<ExpenseRecord> getExpenseRecords(long start, long end) {
        List<ExpenseRecord> records = new ArrayList<>();

        String query = "SELECT " + Constants.COLUMN_NAME_EXPENSE_VALUE + ", " +
                Constants.COLUMN_NAME_EXPENSE_TEXT + ", " +
                Constants.COLUMN_NAME_BANK_NAME + ", " +
                Constants.COLUMN_NAME_TIMESTAMP +
                " FROM " + Constants.TABLE_NAME_EXPENSE_TEST +
                " WHERE " + Constants.COLUMN_NAME_TIMESTAMP + " >= ? AND " +
                Constants.COLUMN_NAME_TIMESTAMP + " <= ?" +
                " ORDER BY " + Constants.COLUMN_NAME_TIMESTAMP + " ASC";

        try (Cursor cursor = mSqliteDatabase.rawQuery(query, new String[]{String.valueOf(start), String.valueOf(end)})) {
            while (cursor.moveToNext()) {
                double amount = cursor.getDouble(0);
                String description = cursor.getString(1);
                String bank = cursor.getString(2);
                long timestamp = cursor.getLong(3);
                records.add(new ExpenseRecord(amount, description, bank, timestamp));
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting expense records: " + e.getMessage());
        }

        return records;
    }

    public boolean hasExpenseData(long start, long end) {
        String query = "SELECT 1 FROM " + Constants.TABLE_NAME_EXPENSE_TEST +
                " WHERE " + Constants.COLUMN_NAME_TIMESTAMP + " >= ? AND " +
                Constants.COLUMN_NAME_TIMESTAMP + " <= ? LIMIT 1";

        try (Cursor cursor = mSqliteDatabase.rawQuery(query, new String[]{String.valueOf(start), String.valueOf(end)})) {
            return cursor.moveToFirst();
        } catch (Exception e) {
            Log.e(TAG, "Error checking expense data: " + e.getMessage());
        }

        return false;
    }

    /**
     * Returns the last expense record (value and bank), or null if none exists.
     */
    public ExpenseRecord getLastExpenseRecord() {
        String query = "SELECT " + Constants.COLUMN_NAME_EXPENSE_VALUE + ", " + Constants.COLUMN_NAME_BANK_NAME +
                " FROM " + Constants.TABLE_NAME_EXPENSE_TEST +
                " ORDER BY " + BaseColumns._ID + " DESC LIMIT 1";

        try (Cursor cursor = mSqliteDatabase.rawQuery(query, null)) {
            if (cursor.moveToFirst() && !cursor.isNull(0)) {
                double amount = cursor.getDouble(0);
                String bank = cursor.getString(1);
                return new ExpenseRecord(amount, null, bank, 0);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting last drank record: " + e.getMessage());
        }

        return null;
    }

    public void closeDb() {
        mDatabaseHelper.close();
    }
}