package com.android.nls.routine.parser;

import android.content.Context;
import android.service.notification.StatusBarNotification;
import android.util.Log;
import com.android.nls.routine.model.Expense;
import com.android.nls.routine.utils.Common;
import com.android.nls.routine.utils.Constants;

public class BankDetector {
    private static final String TAG = Common.generateTag(BankDetector.class);

    public Expense detect(Context context, StatusBarNotification sbn) {
        String pkg = sbn.getOpPkg();

        // Some notifications are posted with a null package (system sources,
        // group summaries). getOpPkg() must be null-checked, otherwise the
        // NullPointerException kills the whole app process in the background.
        if (pkg == null) {
            Log.w(TAG, "Notification with null package ignored");
            return null;
        }

        if (pkg.contains(Constants.BANK_NUBANK)) {
            Log.d(TAG, "nubank pkg detect for > " + pkg);
            return new NubankParser().parse(context, sbn);
        }
        if (pkg.contains(Constants.BANK_ITAU)) {
            Log.d(TAG, "itau pkg detect for > " + pkg);
            return new ItauParser().parse(context, sbn);
        }
        if (pkg.contains(Constants.BANK_BRADESCO)) {
            Log.d(TAG, "bradesco pkg detect for > " + pkg);
            return new BradescoParser().parse(context, sbn);
        }
        if (pkg.contains(Constants.BANK_XP)) {
            Log.d(TAG, "xp pkg detect for > " + pkg);
            return new XpParser().parse(context, sbn);
        }
        if (pkg.contains(Constants.BANK_INTER)) {
            Log.d(TAG, "inter pkg detect for > " + pkg);
            return new InterParser().parse(context, sbn);
        }

        Log.d(TAG, "Before BTG check : " + pkg);

        if (pkg.contains(Constants.BANK_BTG) || pkg.contains("pactual")) {
            Log.d(TAG, "btg pkg detect for > " + pkg);
            return new BtgParser().parse(context, sbn);
        }

        return null;
    }
}