package com.android.nls.routine.repository;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import com.android.nls.routine.database.DatabaseHelper;
import com.android.nls.routine.model.CreditCard;
import com.android.nls.routine.utils.Common;
import com.android.nls.routine.utils.Constants;
import java.util.ArrayList;
import java.util.List;

public class CardRepository {
    private static final String TAG = Common.generateTag(CardRepository.class);
    private final DatabaseHelper mDatabaseHelper;
    private final SQLiteDatabase mSqliteDatabase;

    public CardRepository(Context context) {
        mDatabaseHelper = DatabaseHelper.getInstance(context);
        mDatabaseHelper.acquire();
        mSqliteDatabase = mDatabaseHelper.getWritableDatabase();
    }

    /**
     * Returns the configured credit cards, ordered by insertion.
     */
    public List<CreditCard> getCards() {
        List<CreditCard> cards = new ArrayList<>();

        String query = "SELECT " + Constants.COLUMN_NAME_CARD_BANK + ", " +
                Constants.COLUMN_NAME_CARD_LAST_FOUR + ", " +
                Constants.COLUMN_NAME_CARD_CLOSING_DAY +
                " FROM " + Constants.TABLE_NAME_CARDS +
                " ORDER BY " + Constants._ID + " ASC";

        try (Cursor cursor = mSqliteDatabase.rawQuery(query, null)) {
            while (cursor.moveToNext()) {
                cards.add(new CreditCard(cursor.getString(0), cursor.getString(1), cursor.getInt(2)));
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting cards: " + e.getMessage());
        }

        return cards;
    }

    /**
     * Replaces every stored card with the given list, inside a single
     * transaction. The cards dialog always saves the whole list, so there is
     * no need to diff each card individually.
     */
    public void saveCards(List<CreditCard> cards) {
        mSqliteDatabase.beginTransaction();
        try {
            mSqliteDatabase.delete(Constants.TABLE_NAME_CARDS, null, null);

            for (CreditCard card : cards) {
                ContentValues contentValues = new ContentValues();
                contentValues.put(Constants.COLUMN_NAME_CARD_BANK, card.bankName());
                contentValues.put(Constants.COLUMN_NAME_CARD_LAST_FOUR, card.lastFour());
                contentValues.put(Constants.COLUMN_NAME_CARD_CLOSING_DAY, card.closingDay());
                mSqliteDatabase.insert(Constants.TABLE_NAME_CARDS, null, contentValues);
            }

            mSqliteDatabase.setTransactionSuccessful();
            Log.d(TAG, "Saved cards: " + cards.size());
        } catch (Exception e) {
            Log.e(TAG, "Error saving cards: " + e.getMessage());
        } finally {
            mSqliteDatabase.endTransaction();
        }
    }

    public void closeDb() {
        mDatabaseHelper.release();
    }
}