package com.android.nls.routine.service;

import android.content.Context;
import com.android.nls.routine.model.AllDataSnapshot;
import com.android.nls.routine.model.CreditCard;
import com.android.nls.routine.model.ExpenseRecord;
import com.android.nls.routine.model.MealRecord;
import com.android.nls.routine.model.Tracker;
import com.android.nls.routine.model.TrackerRecord;
import com.android.nls.routine.model.WaterRecord;
import com.android.nls.routine.repository.CardRepository;
import com.android.nls.routine.repository.ConfigRepository;
import com.android.nls.routine.repository.ExpenseRepository;
import com.android.nls.routine.repository.MealRepository;
import com.android.nls.routine.repository.TrackerRepository;
import com.android.nls.routine.repository.WaterRepository;
import java.util.Collections;
import java.util.List;

/**
 * Loads every piece of data stored by the user for the All data screen: the
 * whole water, meal, expense and routine record tables (no date filter, the
 * screen is not tied to a calendar), plus the tracker definitions, the
 * configured cards and the settings. The record lists come back newest first,
 * ready to render.
 */
public class AllDataService {

    /** Epoch millis covering every record the database can hold. */
    private static final long FIRST_MILLIS = 0L;
    private static final long LAST_MILLIS = Long.MAX_VALUE;

    private final WaterRepository mWaterRepository;
    private final MealRepository mMealRepository;
    private final ExpenseRepository mExpenseRepository;
    private final TrackerRepository mTrackerRepository;
    private final ConfigRepository mConfigRepository;
    private final CardRepository mCardRepository;

    public AllDataService(Context context) {
        mWaterRepository = new WaterRepository(context);
        mMealRepository = new MealRepository(context);
        mExpenseRepository = new ExpenseRepository(context);
        mTrackerRepository = new TrackerRepository(context);
        mConfigRepository = new ConfigRepository(context);
        mCardRepository = new CardRepository(context);
    }

    public AllDataSnapshot load(long startMillis, long endMillis) {
        List<WaterRecord> waterRecords = mWaterRepository.getWaterRecords(startMillis, endMillis);
        List<MealRecord> mealRecords = mMealRepository.getMealRecords(startMillis, endMillis);
        List<ExpenseRecord> expenseRecords = mExpenseRepository.getExpenseRecords(startMillis, endMillis);
        List<TrackerRecord> routineRecords = mTrackerRepository.getAllTrackerRecords(startMillis, endMillis);

        // The repositories answer in ascending order; the screen lists the
        // newest record first.
        Collections.reverse(waterRecords);
        Collections.reverse(mealRecords);
        Collections.reverse(expenseRecords);
        Collections.reverse(routineRecords);

        // Compute summaries for the filtered period
        double totalWater = 0;
        for (WaterRecord wr : waterRecords) {
            totalWater += wr.amount();
        }
        int totalMeals = mealRecords.size();
        double totalExpenses = 0;
        for (ExpenseRecord er : expenseRecords) {
            totalExpenses += er.amount();
        }
        int totalRoutines = routineRecords.size();

        // Trackers, cards and settings keep their natural (config) order.
        List<Tracker> trackers = mTrackerRepository.getAllTrackers();
        List<CreditCard> cards = mCardRepository.getCards();

        return new AllDataSnapshot(waterRecords, mealRecords, expenseRecords, routineRecords,
                trackers, cards,
                mConfigRepository.getDailyWaterGoal(),
                mConfigRepository.getDefaultBtn1Value(),
                mConfigRepository.getDefaultBtn2Value(),
                mConfigRepository.getDefaultBtn3Value(),
                mConfigRepository.getMonthlyLimitValue(),
                mConfigRepository.getCardStatementClosingDate(),
                totalWater, totalMeals, totalExpenses, totalRoutines);
    }

    /** Removes one meal record by its database id. */
    public void deleteMeal(long rowId) {
        mMealRepository.deleteMeal(rowId);
    }

    /** Updates one meal record in place, keeping its id and timestamp. */
    public void updateMeal(long rowId, String status, String observation, long timestamp) {
        mMealRepository.updateMeal(rowId, status, observation, timestamp);
    }

    /** Removes one expense record by its database id. */
    public void deleteExpense(int id) {
        mExpenseRepository.deleteExpense(id);
    }

    /** Updates one expense record in place, keeping its id and timestamp. */
    public void updateExpense(int id, double amount, String bank) {
        mExpenseRepository.updateExpense(id, amount, bank);
    }

    /** Removes one tracker record by its database id. */
    public void deleteTrackerRecord(long rowId) {
        mTrackerRepository.deleteTrackerRecord(rowId);
    }

    /** Updates the completed state and note of one tracker record by its database id. */
    public void updateTrackerRecord(long rowId, boolean completed, String note) {
        mTrackerRepository.updateTrackerRecord(rowId, completed, note);
    }

    public void closeDb() {
        mWaterRepository.closeDb();
        mMealRepository.closeDb();
        mExpenseRepository.closeDb();
        mTrackerRepository.closeDb();
        mConfigRepository.closeDb();
        mCardRepository.closeDb();
    }
}