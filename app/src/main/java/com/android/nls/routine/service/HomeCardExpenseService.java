package com.android.nls.routine.service;

import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import com.android.nls.routine.model.Expense;
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

    public String getTotalSpent() {
        long startOfMonth = Common.getStartOfMonthInMillis();
        long endOfMonth = Common.getEndOfMonthInMillis();
        double sum = mExpenseRepository.getTotalSpent(startOfMonth, endOfMonth);
        return String.valueOf((int) sum);
    }

    public String getMonthlyLimitValue() {
        return mConfigRepository.getMonthlyLimitValue();
    }

    public void closeDb() {
        mExpenseRepository.closeDb();
        mConfigRepository.closeDb();
    }
}