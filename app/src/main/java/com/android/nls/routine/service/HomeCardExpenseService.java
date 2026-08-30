package com.android.nls.routine.service;

import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import com.android.nls.routine.model.Expense;
import com.android.nls.routine.model.ExpenseRecord;
import com.android.nls.routine.repository.ConfigRepository;
import com.android.nls.routine.repository.ExpenseRepository;
import com.android.nls.routine.utils.Common;

public class HomeCardExpenseService {
    private static final String TAG = Common.generateTag(HomeCardExpenseService.class);
    private final ExpenseRepository mExpenseRepository;
    private final ConfigRepository mConfigRepository;
    private final Context mContext;

    public HomeCardExpenseService(Context context) {
        mContext = context;
        mExpenseRepository = new ExpenseRepository(mContext);
        mConfigRepository = new ConfigRepository(mContext);
    }

    public void saveExpenseTest(Expense expense) {
        mExpenseRepository.insertExpense(expense);
    }

    public void setNotifyAccess() {
        Intent intent = new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS");
        mContext.startActivity(intent);
    }

    public boolean isNotifyAccessEnabled() {
        String enabledListeners = Settings.Secure.getString(
                mContext.getContentResolver(),
                "enabled_notification_listeners"
        );
        return enabledListeners != null && enabledListeners.contains(mContext.getPackageName());
    }

    public double getTotalSpent() {
        double closingDay = parseClosingDay(mConfigRepository.getCardStatementClosingDate());
        long startOfCycle = Common.getStartOfExpenseCycleInMillis(closingDay);
        long now = System.currentTimeMillis();
        return mExpenseRepository.getTotalSpent(startOfCycle, now);
    }

    public ExpenseRecord getLastExpenseRecord(){
        return mExpenseRepository.getLastExpenseRecord();
    }

    /**
     * Parses the saved closing day (e.g. "05" or "01/xx") into an int.
     * Falls back to 1 if the value is missing or invalid.
     */
    private int parseClosingDay(double value) {
        String valueToString = String.valueOf(value);
        try {
            int day = Integer.parseInt(valueToString.split("/")[0].trim());
            if (day >= 1 && day <= 31) {
                return day;
            }
        } catch (NumberFormatException ignored) {
        }
        return 1;
    }

    public double getMonthlyLimitValue() {
        return mConfigRepository.getMonthlyLimitValue();
    }

    public void closeDb() {
        mExpenseRepository.closeDb();
        mConfigRepository.closeDb();
    }
}