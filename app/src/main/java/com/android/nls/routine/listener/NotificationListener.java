package com.android.nls.routine.listener;

import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;
import com.android.nls.routine.model.Expense;
import com.android.nls.routine.parser.BankDetector;
import com.android.nls.routine.service.HomeCardExpenseService;
import com.android.nls.routine.utils.Common;
import com.android.nls.routine.utils.Constants;
import com.android.nls.routine.widget.WidgetCombinedProvider;

public class NotificationListener extends NotificationListenerService {
    private static final String TAG = Common.generateTag(NotificationListener.class);
    private final BankDetector mBankDetector = new BankDetector();
    private HomeCardExpenseService mHomeCardExpenseService;

    @Override
    public void onCreate() {
        super.onCreate();
        mHomeCardExpenseService = new HomeCardExpenseService(this);
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        if (sbn == null) {
            return;
        }

        try {
            Expense expense = mBankDetector.detect(sbn);
            if (expense != null) {
                Log.d(Constants.TAG, "Detected expense: " + expense);
                mHomeCardExpenseService.saveExpenseTest(expense);
                WidgetCombinedProvider.refresh(this);
            }
        } catch (Exception e) {
            // A single malformed notification must never kill the process:
            // this callback runs in the app process, so an unhandled exception
            // here closes the app even when it is in the background.
            Log.e(TAG, "Failed to process notification", e);
        }
    }

    @Override
    public void onDestroy() {
        mHomeCardExpenseService.closeDb();
        mHomeCardExpenseService = null;
        super.onDestroy();
    }
}